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
            log.info("Creating VNPay payment for order: {}", paymentRequest.getOrderId());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));

            if (!order.canProcessPayment()) {
                throw new RuntimeException("Order cannot process payment");
            }

            // Lấy total amount từ order
            BigDecimal amount = order.getTotalAmount();

            Payment payment = Payment.createVNPayPayment(order, amount);
            payment = paymentRepository.save(payment);

            log.info("Payment created - ID: {}, TxnRef: {}, Amount: {}",
                    payment.getId(), payment.getVnpayTxnRef(), amount);

            String paymentUrl = createVNPayPaymentUrl(payment, order, request);

            VNPayPaymentResponseDTO response = new VNPayPaymentResponseDTO();
            response.setCode("00");
            response.setMessage("Success");
            response.setPaymentUrl(paymentUrl);
            response.setTransactionId(payment.getVnpayTxnRef());

            log.info("VNPay payment created successfully for order: {}", order.getId());
            return response;

        } catch (Exception e) {
            log.error("Error creating VNPay payment: {}", e.getMessage(), e);

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
            long amount = payment.getAmount().multiply(new BigDecimal(100)).longValue();
            vnpParams.put("vnp_Amount", String.valueOf(amount));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", payment.getVnpayTxnRef());

            vnpParams.put("vnp_OrderInfo", "Payment for order " + order.getId());
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
            vnpParams.put("vnp_IpAddr", getRealClientIpAddress(request));

            String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_CreateDate", createDate);

            String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_ExpireDate", expireDate);

            log.info("VNPay Parameters: {}", vnpParams);
            StringBuilder hashData = new StringBuilder();
            vnpParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
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
        // Tạm thời trả về 127.0.0.1 để test
        return "127.0.0.1";
    }

    public boolean validateResponse(Map<String, String> params) {
        try {
            log.info("Validating VNPay response");

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

            log.info("Signature validation: {}", isValid ? "VALID" : "INVALID");
            log.info("Received Hash: {}, Calculated Hash: {}", vnpSecureHash, calculatedHash);

            if (!isValid) {
                log.error("Hash mismatch - Check encoding and parameter order");
                log.error("Params received: {}", params);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating VNPay signature: {}", e.getMessage(), e);
            return false;
        }
    }

    public String testCreatePaymentUrl() {
        try {
            Map<String, String> testParams = new TreeMap<>();
            testParams.put("vnp_Version", "2.1.0");
            testParams.put("vnp_Command", "pay");
            testParams.put("vnp_TmnCode", "2E1QAK9N");
            testParams.put("vnp_Amount", "10000000");
            testParams.put("vnp_CurrCode", "VND");
            testParams.put("vnp_TxnRef", "TEST123456");
            testParams.put("vnp_OrderInfo", "Test payment");
            testParams.put("vnp_OrderType", "other");
            testParams.put("vnp_Locale", "vn");
            testParams.put("vnp_ReturnUrl", "http://localhost:8080/api/payments/return");
            testParams.put("vnp_IpAddr", "127.0.0.1");
            testParams.put("vnp_CreateDate", "20251104150000");
            testParams.put("vnp_ExpireDate", "2025110811500");

            StringBuilder hashData = new StringBuilder();
            testParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            });

            String hashDataStr = hashData.toString();
            log.info("Test Hash Data: {}", hashDataStr);

            String testSecretKey = "SSV6W78GLQ48H28KQCB89XZI4XVJS7Y9";
            String signature = hmacSHA512(testSecretKey, hashDataStr);

            log.info("Test Signature: {}", signature);

            String finalUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + hashDataStr + "&vnp_SecureHash=" + signature;

            return "Test URL: " + finalUrl;

        } catch (Exception e) {
            return "Test Error: " + e.getMessage();
        }
    }
}