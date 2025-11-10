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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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

    @Value("${vnpay.return-url.success:http://localhost:5173/payment-result?status=success}")
    private String vnpayReturnUrlSuccess;

    @Value("${vnpay.return-url.fail:http://localhost:5173/payment-result?status=fail}")
    private String vnpayReturnUrlFail;

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

                String txnRef = fields.get("vnp_TxnRef");
                String redirectUrl = vnpayReturnUrlFail + "&transactionId=" + txnRef + "&errorCode=INVALID_SIGNATURE";

                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "status", "fail",
                        "message", "Invalid signature - Payment verification failed",
                        "redirectUrl", redirectUrl,
                        "transactionId", txnRef
                ));
            }

            log.info("VALID SIGNATURE - Processing payment...");

            String responseCode = fields.get("vnp_ResponseCode");
            String txnRef = fields.get("vnp_TxnRef");

            Payment payment = paymentProcessingService.processVNPayReturn(fields);

            if ("00".equals(responseCode)) {
                log.info("PAYMENT SUCCESS - Order: {}, Amount: {}",
                        payment.getOrderId(), payment.getAmount());

                String redirectUrl = vnpayReturnUrlSuccess + "&transactionId=" + txnRef + "&orderId=" + payment.getOrderId();

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", "success",
                        "message", "Thanh toán thành công",
                        "redirectUrl", redirectUrl,
                        "paymentId", payment.getId(),
                        "orderId", payment.getOrderId(),
                        "amount", payment.getAmount(),
                        "transactionNo", payment.getVnpayTransactionNo(),
                        "transactionId", txnRef
                ));
            } else {
                log.warn("PAYMENT FAILED - Code: {}, Order: {}",
                        responseCode, payment.getOrderId());

                String redirectUrl = vnpayReturnUrlFail + "&transactionId=" + txnRef + "&errorCode=" + responseCode + "&orderId=" + payment.getOrderId();

                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "status", "fail",
                        "message", "Thanh toán thất bại",
                        "redirectUrl", redirectUrl,
                        "errorCode", responseCode,
                        "orderId", payment.getOrderId(),
                        "transactionId", txnRef
                ));
            }

        } catch (Exception e) {
            log.error("ERROR PROCESSING VNPay RETURN: {}", e.getMessage(), e);

            String redirectUrl = vnpayReturnUrlFail + "&error=PROCESSING_ERROR";

            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "status", "error",
                    "message", "Lỗi xử lý thanh toán: " + e.getMessage(),
                    "redirectUrl", redirectUrl
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

    @GetMapping("/status/{txnRef}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String txnRef) {
        try {
            Payment payment = paymentProcessingService.getPaymentByTxnRef(txnRef);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transactionId", txnRef,
                    "status", payment.getStatus().name(),
                    "paymentStatus", payment.getStatus(),
                    "orderId", payment.getOrderId(),
                    "amount", payment.getAmount(),
                    "message", "Payment status retrieved successfully"
            ));

        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error getting payment status: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/test/create-url-corrected")
    public ResponseEntity<?> testCreatePaymentUrlCorrected() {
        try {
            log.info("Testing VNPay URL creation with VIETNAM TIMEZONE");

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
            testParams.put("vnp_ReturnUrl", vnpayReturnUrl);
            testParams.put("vnp_IpAddr", "127.0.0.1");
            ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
            ZonedDateTime nowVietnam = ZonedDateTime.now(vietnamZone);

            String createDate = nowVietnam.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            testParams.put("vnp_CreateDate", createDate);

            String expireDate = nowVietnam.plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            testParams.put("vnp_ExpireDate", expireDate);
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
            String signature = hmacSHA512(vnpaySecretKey, hashDataStr);
            String finalUrl = vnpayUrl + "?" + hashDataStr + "&vnp_SecureHash=" + signature;
            log.info("Vietnam Time - Create: {}, Expire: {}", createDate, expireDate);
            log.info("Current UTC Time: {}", LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "URL created with VIETNAM TIMEZONE",
                    "testUrl", finalUrl,
                    "timeInfo", Map.of(
                            "vietnamCreateTime", createDate,
                            "vietnamExpireTime", expireDate,
                            "currentUTCTime", LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                            "timezone", "Asia/Ho_Chi_Minh (UTC+7)"
                    ),
                    "redirectUrls", Map.of(
                            "successUrl", vnpayReturnUrlSuccess,
                            "failUrl", vnpayReturnUrlFail,
                            "returnUrl", vnpayReturnUrl
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


    @GetMapping("/test/redirect-urls")
    public ResponseEntity<?> testRedirectUrls() {
        return ResponseEntity.ok(Map.of(
                "successUrl", vnpayReturnUrlSuccess,
                "failUrl", vnpayReturnUrlFail,
                "returnUrl", vnpayReturnUrl,
                "description", "Frontend sẽ redirect user đến successUrl hoặc failUrl dựa trên kết quả thanh toán"
        ));
    }
}