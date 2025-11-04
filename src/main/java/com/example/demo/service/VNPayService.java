package com.example.demo.service;

import com.example.demo.dto.VNPayPaymentRequestDTO;
import com.example.demo.dto.VNPayPaymentResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.tmn-code}")
    private String vnpayTmnCode;

    @Value("${vnpay.secret-key}")
    private String vnpaySecretKey;

    @Value("${vnpay.return-url}")
    private String vnpayReturnUrl;

    @Transactional
    public VNPayPaymentResponseDTO createPayment(VNPayPaymentRequestDTO paymentRequest, HttpServletRequest request) {
        try {
            log.info("START Creating VNPay Payment");
            log.info("Order ID: {}", paymentRequest.getOrderId());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));

            if (!order.canProcessPayment()) {
                throw new RuntimeException("Order cannot process payment");
            }
//            BigDecimal amount = order.getTotalAmount();
            BigDecimal amount = new BigDecimal("100000");

            Payment payment = Payment.createVNPayPayment(order, amount);
            payment = paymentRepository.save(payment);
            log.info("Payment created: ID={}, TxnRef={}, Amount={}", payment.getId(), payment.getVnpayTxnRef(), amount);
            String paymentUrl = createVNPayPaymentUrl(payment, order, request);
            VNPayPaymentResponseDTO response = new VNPayPaymentResponseDTO();
            response.setCode("00");
            response.setMessage("Success");
            response.setPaymentUrl(paymentUrl);
            response.setTransactionId(payment.getVnpayTxnRef());

            log.info("END Creating VNPay Payment - SUCCESS");
            return response;

        } catch (Exception e) {
            log.error("ERROR Creating VNPay Payment: {}", e.getMessage(), e);

            VNPayPaymentResponseDTO response = new VNPayPaymentResponseDTO();
            response.setCode("99");
            response.setMessage("Error: " + e.getMessage());
            return response;
        }
    }

    private String createVNPayPaymentUrl(Payment payment, Order order, HttpServletRequest request) {
        try {
            log.info("Creating VNPay URL for TxnRef: {}", payment.getVnpayTxnRef());
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnpayTmnCode);
            long amount = payment.getAmount().multiply(new java.math.BigDecimal(100)).longValue();
            vnpParams.put("vnp_Amount", String.valueOf(amount));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", payment.getVnpayTxnRef());
            String orderInfo = "Payment for order " + order.getId();
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
            String ipAddr = getRealClientIpAddress(request);
            vnpParams.put("vnp_IpAddr", ipAddr);
            String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_CreateDate", createDate);
            String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_ExpireDate", expireDate);
            vnpParams.put("vnp_BankCode", "VNBANK");
            log.info("VNPay Parameters: {}", vnpParams);
            StringBuilder hashData = new StringBuilder();
            vnpParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(key)
                            .append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            });
            String hashDataStr = hashData.toString();
            log.info("Hash Data: {}", hashDataStr);
            String vnpSecureHash = hmacSHA512(vnpaySecretKey, hashDataStr);
            log.info("Secure Hash: {}", vnpSecureHash);
            String finalUrl = vnpayUrl + "?" + hashDataStr + "&vnp_SecureHash=" + vnpSecureHash;

            log.info("Final VNPay URL: {}", finalUrl);

            return finalUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create VNPay URL: " + e.getMessage(), e);
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKeySpec);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getRealClientIpAddress(HttpServletRequest request) {
        return "127.0.0.1";
    }

    public boolean validateResponse(Map<String, String> params) {
        try {
            log.info("🔐 VALIDATING VNPay RESPONSE");

            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                log.error("Missing vnp_SecureHash in response");
                return false;
            }

            Map<String, String> hashParams = new TreeMap<>(params);
            hashParams.remove("vnp_SecureHash");
            hashParams.remove("vnp_SecureHashType");
            StringBuilder hashData = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = hashParams.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName)
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                }
            }

            String hashDataStr = hashData.toString();
            log.info("Validation Hash Data: {}", hashDataStr);

            String calculatedHash = hmacSHA512(vnpaySecretKey, hashDataStr);
            boolean isValid = vnpSecureHash.equalsIgnoreCase(calculatedHash);

            log.info("Signature validation: {}", isValid ? "✅ VALID" : "❌ INVALID");
            log.info("Received Hash: {}", vnpSecureHash);
            log.info("Calculated Hash: {}", calculatedHash);

            if (!isValid) {
                log.error("❌ HASH MISMATCH - Check encoding and parameter order");
                log.error("Params received: {}", params);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating VNPay signature: {}", e.getMessage(), e);
            return false;
        }
    }

}