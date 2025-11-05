package com.example.demo.controller;

import com.example.demo.dto.VNPayPaymentRequestDTO;
import com.example.demo.dto.VNPayPaymentResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.PaymentProcessingService;
import com.example.demo.service.VNPayService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Payment API", description = "APIs for payment processing with VNPay")
public class PaymentController {

    private final VNPayService vnPayService;
    private final PaymentProcessingService paymentProcessingService;
    private final OrderRepository orderRepository;

    @Value("${vnpay.tmn-code}")
    private String vnpayTmnCode;

    @Value("${vnpay.secret-key}")
    private String vnpaySecretKey;

    @Value("${vnpay.return-url}")
    private String vnpayReturnUrl;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createVNPayPayment(
            @RequestBody VNPayPaymentRequestDTO paymentRequest,
            HttpServletRequest request) {

        try {
            log.info("Creating VNPay payment for order: {}", paymentRequest.getOrderId());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy order với ID: " + paymentRequest.getOrderId()));

            if (!order.canProcessPayment()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Order không thể thanh toán",
                        "orderId", order.getId(),
                        "status", order.getStatus()
                ));
            }

            VNPayPaymentResponseDTO response = vnPayService.createPayment(paymentRequest, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating VNPay payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Lỗi khi tạo thanh toán: " + e.getMessage()
            ));
        }
    }

    @SecurityRequirement(name = "")
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVNPayReturn(HttpServletRequest request) {

        try {
            log.info("HANDLING VNPay RETURN CALLBACK");

            Map<String, String> fields = new java.util.HashMap<>();
            request.getParameterNames().asIterator()
                    .forEachRemaining(param -> {
                        String value = request.getParameter(param);
                        fields.put(param, value);
                        log.info("VNPay Param - {}: {}", param, value);
                    });

            log.info("VNPay RETURN PARAMS: {}", fields);

            boolean isValid = vnPayService.validateResponse(fields);

            if (!isValid) {
                log.error("INVALID SIGNATURE FROM VNPay");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid signature - Payment verification failed"
                ));
            }

            log.info("VALID SIGNATURE - Processing payment...");

            String responseCode = fields.get("vnp_ResponseCode");

            Payment payment = paymentProcessingService.processVNPayReturn(fields);

            if ("00".equals(responseCode)) {
                log.info("PAYMENT SUCCESS - Order: {}, Amount: {}",
                        payment.getOrderId(), payment.getAmount());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán thành công",
                        "paymentId", payment.getId(),
                        "orderId", payment.getOrderId(),
                        "amount", payment.getAmount(),
                        "transactionNo", payment.getVnpayTransactionNo()
                ));
            } else {
                log.warn("PAYMENT FAILED - Code: {}, Order: {}",
                        responseCode, payment.getOrderId());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thanh toán thất bại",
                        "errorCode", responseCode,
                        "orderId", payment.getOrderId()
                ));
            }

        } catch (Exception e) {
            log.error("ERROR PROCESSING VNPay RETURN: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi xử lý thanh toán: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentsByOrder(@PathVariable Integer orderId) {
        if (!orderRepository.existsById(orderId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order không tồn tại"));
        }

        return ResponseEntity.ok(paymentProcessingService.getPaymentsByOrder(orderId));
    }

    @GetMapping("/vnpay/txn/{txnRef}")
    public ResponseEntity<?> getPaymentByTxnRef(@PathVariable String txnRef) {
        return ResponseEntity.ok(paymentProcessingService.getPaymentByTxnRef(txnRef));
    }

    // 🔧 TEST ENDPOINTS

    @GetMapping("/test/create-url-localhost")
    public ResponseEntity<?> testCreatePaymentUrlLocalhost() {
        try {
            log.info("Testing VNPay URL creation for LOCALHOST");

            Map<String, String> testParams = new TreeMap<>();
            testParams.put("vnp_Version", "2.1.0");
            testParams.put("vnp_Command", "pay");
            testParams.put("vnp_TmnCode", vnpayTmnCode);
            testParams.put("vnp_Amount", "100000"); // 1,000 VND - số tiền nhỏ để test
            testParams.put("vnp_CurrCode", "VND");
            testParams.put("vnp_TxnRef", "LOCALTEST" + System.currentTimeMillis());
            testParams.put("vnp_OrderInfo", "Test payment on localhost");
            testParams.put("vnp_OrderType", "other");
            testParams.put("vnp_Locale", "vn");
            testParams.put("vnp_ReturnUrl", "http://localhost:8080/api/payments/vnpay/return");
            testParams.put("vnp_IpAddr", "127.0.0.1");

            String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            testParams.put("vnp_CreateDate", createDate);

            String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            testParams.put("vnp_ExpireDate", expireDate);

            // Tạo hash data
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
            log.info("Localhost Test Hash Data: {}", hashDataStr);

            String signature = hmacSHA512(vnpaySecretKey, hashDataStr);
            log.info("Localhost Test Signature: {}", signature);

            String finalUrl = vnpayUrl + "?" + hashDataStr + "&vnp_SecureHash=" + signature;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test URL for localhost created",
                    "testUrl", finalUrl,
                    "notes", "Copy URL này và mở trong browser để test thanh toán"
            ));

        } catch (Exception e) {
            log.error("Error testing VNPay URL for localhost: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/test/debug-detailed")
    public ResponseEntity<?> debugDetailed() {
        try {
            // Test hash generation với dữ liệu mẫu
            Map<String, String> testParams = new TreeMap<>();
            testParams.put("vnp_Version", "2.1.0");
            testParams.put("vnp_Command", "pay");
            testParams.put("vnp_TmnCode", vnpayTmnCode);
            testParams.put("vnp_Amount", "1000000");
            testParams.put("vnp_CurrCode", "VND");
            testParams.put("vnp_TxnRef", "TEST123");
            testParams.put("vnp_OrderInfo", "Test");
            testParams.put("vnp_OrderType", "other");
            testParams.put("vnp_Locale", "vn");
            testParams.put("vnp_ReturnUrl", vnpayReturnUrl);
            testParams.put("vnp_IpAddr", "127.0.0.1");
            testParams.put("vnp_CreateDate", "20251105172400");
            testParams.put("vnp_ExpireDate", "20251105173900");

            StringBuilder hashData = new StringBuilder();
            testParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    if (hashData.length() > 0) hashData.append('&');
                    hashData.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            });

            String hashDataStr = hashData.toString();
            String signature = hmacSHA512(vnpaySecretKey, hashDataStr);

            Map<String, Object> debugInfo = Map.of(
                    "config", Map.of(
                            "vnpayUrl", vnpayUrl,
                            "tmnCode", vnpayTmnCode,
                            "returnUrl", vnpayReturnUrl,
                            "secretKeyLength", vnpaySecretKey != null ? vnpaySecretKey.length() : 0
                    ),
                    "testHashData", hashDataStr,
                    "testSignature", signature,
                    "testParams", testParams
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "debugInfo", debugInfo
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/test/create-url-corrected")
    public ResponseEntity<?> testCreatePaymentUrlCorrected() {
        try {
            log.info("Testing VNPay URL creation with CORRECTED parameters");

            Map<String, String> testParams = new TreeMap<>();
            testParams.put("vnp_Version", "2.1.0");
            testParams.put("vnp_Command", "pay");
            testParams.put("vnp_TmnCode", vnpayTmnCode);
            testParams.put("vnp_Amount", "1000000"); // 10,000 VND
            testParams.put("vnp_CurrCode", "VND");
            testParams.put("vnp_TxnRef", "TEST" + System.currentTimeMillis());
            testParams.put("vnp_OrderInfo", "Test payment corrected");
            testParams.put("vnp_OrderType", "other");
            testParams.put("vnp_Locale", "vn");
            testParams.put("vnp_ReturnUrl", "http://localhost:8080/api/payments/vnpay/return"); // ✅ ĐÚNG URL
            testParams.put("vnp_IpAddr", "127.0.0.1");

            // ✅ ĐỊNH DẠNG THỜI GIAN ĐÚNG (14 số)
            String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            testParams.put("vnp_CreateDate", createDate);

            String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            testParams.put("vnp_ExpireDate", expireDate);

            // Tạo hash data
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
            log.info("CORRECTED Hash Data: {}", hashDataStr);

            String signature = hmacSHA512(vnpaySecretKey, hashDataStr);
            log.info("CORRECTED Signature: {}", signature);

            String finalUrl = vnpayUrl + "?" + hashDataStr + "&vnp_SecureHash=" + signature;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "URL created with CORRECTED parameters",
                    "testUrl", finalUrl,
                    "checks", Map.of(
                            "returnUrl", "Contains /vnpay/ - ✅",
                            "expireDate", "14 digits - ✅",
                            "createDate", "14 digits - ✅",
                            "signature", "Generated - ✅"
                    )
            ));

        } catch (Exception e) {
            log.error("Error creating corrected VNPay URL: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // Helper method for HMAC SHA512
    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKeySpec);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}