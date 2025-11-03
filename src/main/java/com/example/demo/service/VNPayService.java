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
            log.info("Order ID: {}, Amount: {}", paymentRequest.getOrderId(), paymentRequest.getAmount());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));

            if (!order.canProcessPayment()) {
                throw new RuntimeException("Order cannot process payment");
            }

            Payment payment = Payment.createVNPayPayment(order, paymentRequest.getAmount());
            payment = paymentRepository.save(payment);

            log.info("Payment created: ID={}, TxnRef={}", payment.getId(), payment.getVnpayTxnRef());

            String paymentUrl = createVNPayPaymentUrl(payment, paymentRequest, request);

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

    private String createVNPayPaymentUrl(Payment payment, VNPayPaymentRequestDTO paymentRequest, HttpServletRequest request) {
        try {
            log.info("Creating VNPay URL for TxnRef: {}", payment.getVnpayTxnRef());

            Map<String, String> vnpParams = new TreeMap<>();

            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnpayTmnCode);

            long amount = paymentRequest.getAmount().multiply(new java.math.BigDecimal(100)).longValue();
            vnpParams.put("vnp_Amount", String.valueOf(amount));

            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", payment.getVnpayTxnRef());

            String orderInfo = "Thanh toan don hang " + paymentRequest.getOrderId();
            vnpParams.put("vnp_OrderInfo", orderInfo);

            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", paymentRequest.getLanguage());
            vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);

            String ipAddr = getRealClientIpAddress(request);
            vnpParams.put("vnp_IpAddr", ipAddr);

            String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_CreateDate", createDate);

            String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnpParams.put("vnp_ExpireDate", expireDate);

            if (paymentRequest.getBankCode() != null && !paymentRequest.getBankCode().isEmpty()) {
                vnpParams.put("vnp_BankCode", paymentRequest.getBankCode());
            }

            log.info("VNPay Parameters: {}", vnpParams);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<Map.Entry<String, String>> iterator = vnpParams.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(fieldValue);

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    if (iterator.hasNext()) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }

            String hashDataStr = hashData.toString();
            log.info("Hash Data: {}", hashDataStr);

            String vnpSecureHash = hmacSHA512(vnpaySecretKey, hashDataStr);
            log.info("Secure Hash: {}", vnpSecureHash);

            query.append("&vnp_SecureHash=").append(vnpSecureHash);

            String finalUrl = vnpayUrl + "?" + query.toString();
            log.info("Final VNPay URL: {}", finalUrl);

            return finalUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create VNPay URL: " + e.getMessage(), e);
        }
    }

    private String getRealClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "127.0.0.1".equals(remoteAddr)) {
            return "42.112.78.100";
        }

        return remoteAddr;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating security signature", e);
        }
    }

    public boolean validateResponse(Map<String, String> params) {
        try {
            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                log.error("Missing signature in response");
                return false;
            }

            Map<String, String> hashParams = new TreeMap<>(params);
            hashParams.remove("vnp_SecureHash");

            StringBuilder hashData = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = hashParams.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(fieldValue);
                    if (iterator.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String calculatedHash = hmacSHA512(vnpaySecretKey, hashData.toString());
            boolean isValid = vnpSecureHash.equals(calculatedHash);

            log.info("Signature validation: {}", isValid ? "VALID" : "INVALID");
            return isValid;

        } catch (Exception e) {
            log.error("Error validating VNPay signature: {}", e.getMessage());
            return false;
        }
    }
}