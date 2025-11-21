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
import org.springframework.web.servlet.view.RedirectView;

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

    // SET MẶC ĐỊNH TRONG CODE - KHÔNG cần config
    private final String vnpayReturnUrlSuccess = "https://electric-vehicle-dealer-management.vercel.app/payment-result?status=success";
    private final String vnpayReturnUrlFail = "https://electric-vehicle-dealer-management.vercel.app/payment-result?status=fail";

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
    public RedirectView handleVNPayReturn(HttpServletRequest request) {
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

            String txnRef = fields.get("vnp_TxnRef");
            String responseCode = fields.get("vnp_ResponseCode");

            // Log URLs để debug
            log.info("Success URL: {}", vnpayReturnUrlSuccess);
            log.info("Fail URL: {}", vnpayReturnUrlFail);

            if (!isValid) {
                log.error("INVALID SIGNATURE FROM VNPay");
                String redirectUrl = buildFailUrl(txnRef, "INVALID_SIGNATURE", null);
                return new RedirectView(redirectUrl);
            }

            log.info("VALID SIGNATURE - Processing payment...");

            Payment payment = paymentProcessingService.processVNPayReturn(fields);

            if ("00".equals(responseCode)) {
                log.info("PAYMENT SUCCESS - Order: {}, Amount: {}",
                        payment.getOrderId(), payment.getAmount());

                String redirectUrl = buildSuccessUrl(txnRef, payment.getOrderId());
                return new RedirectView(redirectUrl);
            } else {
                log.warn("PAYMENT FAILED - Code: {}, Order: {}",
                        responseCode, payment.getOrderId());

                String redirectUrl = buildFailUrl(txnRef, responseCode, payment.getOrderId());
                return new RedirectView(redirectUrl);
            }

        } catch (Exception e) {
            log.error("ERROR PROCESSING VNPay RETURN: {}", e.getMessage(), e);
            String redirectUrl = buildFailUrl(null, "PROCESSING_ERROR", null);
            return new RedirectView(redirectUrl);
        }
    }

    /**
     * Build success URL với các tham số
     */
    private String buildSuccessUrl(String transactionId, Integer orderId) {
        StringBuilder url = new StringBuilder(vnpayReturnUrlSuccess);

        if (transactionId != null) {
            url.append("&transactionId=").append(transactionId);
        }

        if (orderId != null) {
            url.append("&orderId=").append(orderId);
        }

        return url.toString();
    }

    /**
     * Build fail URL với các tham số
     */
    private String buildFailUrl(String transactionId, String errorCode, Integer orderId) {
        StringBuilder url = new StringBuilder(vnpayReturnUrlFail);

        if (transactionId != null) {
            url.append("&transactionId=").append(transactionId);
        }

        if (errorCode != null) {
            url.append("&errorCode=").append(errorCode);
        }

        if (orderId != null) {
            url.append("&orderId=").append(orderId);
        }

        return url.toString();
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

    /**
     * API để frontend kiểm tra trạng thái payment
     */
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
            testParams.put("vnp_Amount", "1000000");
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

    @GetMapping("/test/timezone")
    public ResponseEntity<?> testTimezone() {
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZoneId utcZone = ZoneId.of("UTC");

        ZonedDateTime nowVietnam = ZonedDateTime.now(vietnamZone);
        ZonedDateTime nowUTC = ZonedDateTime.now(utcZone);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        Map<String, String> timeInfo = new LinkedHashMap<>();
        timeInfo.put("Vietnam Time (UTC+7)", nowVietnam.format(formatter));
        timeInfo.put("UTC Time", nowUTC.format(formatter));
        timeInfo.put("System Time", LocalDateTime.now().format(formatter));
        timeInfo.put("Timezone Difference", "Vietnam is 7 hours ahead of UTC");

        return ResponseEntity.ok(timeInfo);
    }

    /**
     * Test endpoint để kiểm tra redirect URLs
     */
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
