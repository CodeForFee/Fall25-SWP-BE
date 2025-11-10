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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Payment API", description = "APIs for payment processing with VNPay")
public class PaymentController {

    private final VNPayService vnPayService;
    private final PaymentProcessingService paymentProcessingService;
    private final OrderRepository orderRepository;

    @Value("${vnpay.return-url.success}")
    private String vnpayReturnUrlSuccess;

    @Value("${vnpay.return-url.fail}")
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

    /**
     * VNPay callback endpoint - MUST RETURN HTML for redirect
     * VNPay sẽ gọi endpoint này sau khi user thanh toán
     */
    @SecurityRequirement(name = "")
    @GetMapping("/vnpay/return")
    public void handleVNPayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            log.info("=== VNPay CALLBACK RECEIVED ===");

            // Thu thập tất cả parameters từ VNPay
            Map<String, String> fields = new java.util.HashMap<>();
            request.getParameterNames().asIterator()
                    .forEachRemaining(param -> {
                        String value = request.getParameter(param);
                        fields.put(param, value);
                        log.debug("VNPay Param - {}: {}", param, value);
                    });

            // Validate chữ ký từ VNPay
            boolean isValid = vnPayService.validateResponse(fields);
            
            String txnRef = fields.get("vnp_TxnRef");
            String responseCode = fields.get("vnp_ResponseCode");

            log.info("Transaction Info - TxnRef: {}, ResponseCode: {}, Valid: {}", 
                    txnRef, responseCode, isValid);

            String redirectUrl;

            // Nếu chữ ký không hợp lệ
            if (!isValid) {
                log.error("INVALID SIGNATURE from VNPay");
                redirectUrl = buildRedirectUrl(
                    vnpayReturnUrlFail,
                    txnRef,
                    null,
                    "INVALID_SIGNATURE",
                    "Chữ ký không hợp lệ"
                );
                sendHtmlRedirect(response, redirectUrl);
                return;
            }

            // Xử lý payment trong database
            log.info("Processing payment in database...");
            Payment payment = paymentProcessingService.processVNPayReturn(fields);

            // Kiểm tra kết quả thanh toán
            if ("00".equals(responseCode)) {
                // THANH TOÁN THÀNH CÔNG
                log.info("✅ PAYMENT SUCCESS - Order: {}, Amount: {}", 
                        payment.getOrderId(), payment.getAmount());

                redirectUrl = buildRedirectUrl(
                    vnpayReturnUrlSuccess,
                    txnRef,
                    payment.getOrderId(),
                    responseCode,
                    "Thanh toán thành công"
                );
                
                log.info("✅ Redirecting to SUCCESS: {}", redirectUrl);
                sendHtmlRedirect(response, redirectUrl);
                
            } else {
                // THANH TOÁN THẤT BẠI
                log.warn("❌ PAYMENT FAILED - Code: {}, Order: {}", 
                        responseCode, payment.getOrderId());

                redirectUrl = buildRedirectUrl(
                    vnpayReturnUrlFail,
                    txnRef,
                    payment.getOrderId(),
                    responseCode,
                    getErrorMessage(responseCode)
                );
                
                log.info("❌ Redirecting to FAIL: {}", redirectUrl);
                sendHtmlRedirect(response, redirectUrl);
            }

        } catch (Exception e) {
            log.error("❌ ERROR processing VNPay callback: {}", e.getMessage(), e);
            
            String redirectUrl = buildRedirectUrl(
                vnpayReturnUrlFail,
                null,
                null,
                "PROCESSING_ERROR",
                "Lỗi xử lý thanh toán: " + e.getMessage()
            );
            
            log.info("❌ Redirecting to FAIL (exception): {}", redirectUrl);
            sendHtmlRedirect(response, redirectUrl);
        }
    }

    /**
     * Send HTML with JavaScript redirect
     * Đây là cách chắc chắn nhất để redirect từ backend sang frontend
     */
    private void sendHtmlRedirect(HttpServletResponse response, String redirectUrl) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Đang chuyển hướng...</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                    }
                    .container {
                        text-align: center;
                        padding: 40px;
                        background: rgba(255, 255, 255, 0.1);
                        border-radius: 20px;
                        backdrop-filter: blur(10px);
                    }
                    .spinner {
                        border: 4px solid rgba(255, 255, 255, 0.3);
                        border-radius: 50%;
                        border-top: 4px solid white;
                        width: 50px;
                        height: 50px;
                        animation: spin 1s linear infinite;
                        margin: 20px auto;
                    }
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                    h2 { margin: 20px 0 10px 0; }
                    p { opacity: 0.9; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="spinner"></div>
                    <h2>Đang xử lý kết quả thanh toán...</h2>
                    <p>Vui lòng chờ trong giây lát</p>
                    <p style="font-size: 12px; margin-top: 20px;">
                        Nếu không tự động chuyển hướng, 
                        <a href="%s" style="color: white; text-decoration: underline;">nhấn vào đây</a>
                    </p>
                </div>
                <script>
                    // Redirect ngay lập tức
                    window.location.href = '%s';
                </script>
            </body>
            </html>
            """.formatted(redirectUrl, redirectUrl);
        
        response.getWriter().write(html);
        response.getWriter().flush();
    }

    /**
     * Build redirect URL với các parameters
     */
    private String buildRedirectUrl(String baseUrl, String txnRef, 
                                   Integer orderId, String errorCode, String message) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        if (txnRef != null) {
            url.append("&transactionId=").append(txnRef);
        }
        
        if (orderId != null) {
            url.append("&orderId=").append(orderId);
        }
        
        if (errorCode != null) {
            url.append("&code=").append(errorCode);
        }
        
        if (message != null) {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            url.append("&message=").append(encodedMessage);
        }
        
        return url.toString();
    }

    /**
     * Lấy thông báo lỗi dựa trên response code của VNPay
     */
    private String getErrorMessage(String responseCode) {
        return switch (responseCode) {
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ";
            case "09" -> "Giao dịch không thành công do: Thẻ chưa đăng ký dịch vụ";
            case "10" -> "Giao dịch không thành công do: Xác thực không chính xác";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán";
            case "12" -> "Giao dịch không thành công do: Thẻ bị khóa";
            case "13" -> "Giao dịch không thành công do: Sai mật khẩu";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản không đủ số dư";
            case "65" -> "Giao dịch không thành công do: Vượt quá số lần nhập sai";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: Nhập sai mật khẩu quá số lần quy định";
            default -> "Giao dịch thất bại - Mã lỗi: " + responseCode;
        };
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
                    "message", "Lấy trạng thái thanh toán thành công"
            ));

        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy trạng thái: " + e.getMessage()
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
}
