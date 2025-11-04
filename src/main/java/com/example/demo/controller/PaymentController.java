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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

}