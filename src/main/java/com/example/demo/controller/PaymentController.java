package com.example.demo.controller;

import com.example.demo.dto.VNPayPaymentRequestDTO;
import com.example.demo.dto.VNPayPaymentResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.PaymentProcessingService;
import com.example.demo.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Payment API", description = "APIs for payment processing with VNPay")
public class PaymentController {

    private final VNPayService vnPayService;
    private final PaymentProcessingService paymentProcessingService;
    private final OrderRepository orderRepository;

    @Operation(
            summary = "Tao thanh toan VNPay",
            description = "Tao URL thanh toan VNPay cho order da duoc duyet"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tao thanh toan thanh cong"),
            @ApiResponse(responseCode = "400", description = "Order khong the thanh toan hoac co loi"),
            @ApiResponse(responseCode = "404", description = "Order khong ton tai")
    })
    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createVNPayPayment(
            @Parameter(description = "Thong tin thanh toan VNPay", required = true)
            @RequestBody VNPayPaymentRequestDTO paymentRequest,
            HttpServletRequest request) {

        try {
            log.info("Creating VNPay payment for order: {}", paymentRequest.getOrderId());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay order voi ID: " + paymentRequest.getOrderId()));

            if (!order.canProcessPayment()) {
                Map<String, Object> errorResponse = Map.of(
                        "error", "Order khong the xu ly thanh toan",
                        "orderId", order.getId(),
                        "status", order.getStatus(),
                        "approvalStatus", order.getApprovalStatus(),
                        "paymentStatus", order.getPaymentStatus(),
                        "remainingAmount", order.getRemainingAmount()
                );
                log.warn("Order cannot process payment: {}", errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            VNPayPaymentResponseDTO response = vnPayService.createPayment(paymentRequest, request);

            if ("00".equals(response.getCode())) {
                log.info("VNPay payment created successfully for order: {}", order.getId());
                return ResponseEntity.ok(response);
            } else {
                log.error("Failed to create VNPay payment: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error creating VNPay payment for order {}: {}", paymentRequest.getOrderId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Loi khi tao thanh toan: " + e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Xu ly ket qua thanh toan tu VNPay",
            description = "API nay duoc VNPay goi lai sau khi nguoi dung hoan tat thanh toan"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xu ly ket qua thanh cong"),
            @ApiResponse(responseCode = "400", description = "Loi xu ly ket qua thanh toan")
    })
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVNPayReturn(
            @Parameter(description = "Cac tham so tu VNPay tra ve", required = true)
            @RequestParam Map<String, String> params) {

        try {
            log.info("Processing VNPay return with params: {}", params);

            Payment payment = paymentProcessingService.processVNPayReturn(params);

            if (payment.isSuccessful()) {
                Map<String, Object> successResponse = Map.of(
                        "success", true,
                        "message", "Thanh toan thanh cong",
                        "paymentId", payment.getId(),
                        "orderId", payment.getOrderId(),
                        "amount", payment.getAmount(),
                        "transactionCode", payment.getTransactionCode(),
                        "vnpayTransactionNo", payment.getVnpayTransactionNo()
                );
                log.info("Payment processed successfully: {}", successResponse);
                return ResponseEntity.ok(successResponse);
            } else {
                Map<String, Object> failedResponse = Map.of(
                        "success", false,
                        "message", "Thanh toan that bai",
                        "paymentId", payment.getId(),
                        "orderId", payment.getOrderId(),
                        "errorCode", payment.getVnpayResponseCode()
                );
                log.warn("Payment failed: {}", failedResponse);
                return ResponseEntity.ok(failedResponse);
            }

        } catch (Exception e) {
            log.error("Error processing VNPay return: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Loi xu ly thanh toan: " + e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Lay danh sach thanh toan theo Order",
            description = "Lay tat ca cac payment cua mot order cu the"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lay danh sach payment thanh cong"),
            @ApiResponse(responseCode = "404", description = "Order khong ton tai")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentsByOrder(
            @Parameter(description = "ID cua order", required = true, example = "1")
            @PathVariable Integer orderId) {

        try {
            log.info("Getting payments for order: {}", orderId);

            if (!orderRepository.existsById(orderId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order khong ton tai: " + orderId));
            }

            var payments = paymentProcessingService.getPaymentsByOrder(orderId);
            return ResponseEntity.ok(payments);

        } catch (Exception e) {
            log.error("Error getting payments for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Lay thong tin payment theo Transaction Reference",
            description = "Lay chi tiet payment dua tren VNPay transaction reference"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lay payment thanh cong"),
            @ApiResponse(responseCode = "404", description = "Payment khong ton tai")
    })
    @GetMapping("/{txnRef}")
    public ResponseEntity<?> getPaymentByTxnRef(
            @Parameter(description = "VNPay Transaction Reference", required = true, example = "VNP1701423400123")
            @PathVariable String txnRef) {

        try {
            log.info("Getting payment by txnRef: {}", txnRef);

            var payment = paymentProcessingService.getPaymentByTxnRef(txnRef);
            return ResponseEntity.ok(payment);

        } catch (Exception e) {
            log.error("Error getting payment by txnRef {}: {}", txnRef, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Kiem tra trang thai thanh toan cua Order",
            description = "Kiem tra order co the thanh toan duoc khong va trang thai hien tai"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kiem tra thanh cong"),
            @ApiResponse(responseCode = "404", description = "Order khong ton tai")
    })
    @GetMapping("/order/{orderId}/status")
    public ResponseEntity<?> checkOrderPaymentStatus(
            @Parameter(description = "ID cua order", required = true, example = "1")
            @PathVariable Integer orderId) {

        try {
            log.info("Checking payment status for order: {}", orderId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay order: " + orderId));

            Map<String, Object> statusResponse = Map.of(
                    "orderId", order.getId(),
                    "totalAmount", order.getTotalAmount(),
                    "paidAmount", order.getPaidAmount(),
                    "remainingAmount", order.getRemainingAmount(),
                    "status", order.getStatus(),
                    "approvalStatus", order.getApprovalStatus(),
                    "paymentStatus", order.getPaymentStatus(),
                    "canProcessPayment", order.canProcessPayment(),
                    "isFullyPaid", order.isFullyPaid()
            );

            return ResponseEntity.ok(statusResponse);

        } catch (Exception e) {
            log.error("Error checking payment status for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}