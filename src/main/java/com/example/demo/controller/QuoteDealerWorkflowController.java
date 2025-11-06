package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.entity.*;
import com.example.demo.service.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dealer-workflow")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class QuoteDealerWorkflowController {

    private final QuoteDealerStaffService quoteDealerStaffService;
    private final QuoteDealerManagerService quoteDealerManagerService;
    private final OrderWorkflowService orderWorkflowService;
    private final PaymentProcessingService paymentProcessingService;
    private final DealerOrderWorkflowService dealerOrderWorkflowService;


    @PostMapping("/quotes/{quoteId}/submit-for-approval")
    public ResponseEntity<String> submitForManagerApproval(
            @PathVariable Integer quoteId,
            @RequestParam Integer staffId) {  // 🔥 THÊM staffId parameter
        quoteDealerStaffService.submitToDealerManager(quoteId, staffId);
        return ResponseEntity.ok("Quote submitted for dealer manager approval successfully");
    }


    @PostMapping("/quotes/{quoteId}/approve")
    public ResponseEntity<String> approveQuote(
            @PathVariable Integer quoteId,
            @RequestParam Integer managerId,
            @RequestParam(required = false) String notes) {
        quoteDealerManagerService.approveQuoteByManager(quoteId, managerId, notes);
        return ResponseEntity.ok("Quote approved by dealer manager successfully");
    }

    @PostMapping("/quotes/{quoteId}/reject")
    public ResponseEntity<String> rejectQuote(
            @PathVariable Integer quoteId,
            @RequestParam Integer managerId,
            @RequestParam String reason) {
        quoteDealerManagerService.rejectQuoteByManager(quoteId, managerId, reason);
        return ResponseEntity.ok("Quote rejected by dealer manager");
    }

    @GetMapping("/quotes/{quoteId}/check-inventory")
    public ResponseEntity<Map<String, Object>> checkInventoryForQuote(@PathVariable Integer quoteId) {
        Quote quote = quoteDealerManagerService.getQuoteById(quoteId);
        boolean hasSufficientInventory = quoteDealerManagerService.checkDealerInventoryForQuote(
                quoteId, quote.getCustomer().getDealerId());

        Map<String, Object> response = new HashMap<>();
        response.put("quoteId", quoteId);
        response.put("hasSufficientInventory", hasSufficientInventory);
        response.put("dealerId", quote.getCustomer().getDealerId());
        response.put("message", hasSufficientInventory ?
                "Kho đại lý đủ hàng" : "Kho đại lý không đủ mẫu xe đang được đặt");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/quotes/pending-approval")
    public ResponseEntity<List<Quote>> getQuotesPendingApproval(@RequestParam Integer managerId) {
        List<Quote> quotes = quoteDealerManagerService.getPendingQuotesForManager(managerId);
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/quotes/approved-ready")
    public ResponseEntity<List<Quote>> getApprovedQuotesReadyForOrder(@RequestParam Integer managerId) {
        List<Quote> quotes = quoteDealerManagerService.getApprovedQuotesReadyForOrder(managerId);
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/quotes/{quoteId}/can-create-order")
    public ResponseEntity<Boolean> canCreateOrderFromQuote(@PathVariable Integer quoteId) {
        boolean canCreate = orderWorkflowService.canCreateOrderFromQuote(quoteId);
        return ResponseEntity.ok(canCreate);
    }

    @PostMapping("/orders/create-from-approved-quote")
    public ResponseEntity<OrderResponseDTO> createOrderFromApprovedQuote(
            @RequestBody OrderDTO orderDTO,
            @RequestParam Integer staffId) {
        OrderResponseDTO order = dealerOrderWorkflowService.createOrderFromApprovedQuote(orderDTO, staffId);
        return ResponseEntity.ok(order);
    }

    /**
     * Duyệt order
     * GIỐNG: POST /api/workflow/orders/{orderId}/approve
     */
    @PostMapping("/orders/{orderId}/approve")
    public ResponseEntity<String> approveOrder(
            @PathVariable Integer orderId,
            @RequestParam Integer approvedBy,
            @RequestParam(required = false) String notes) {
        orderWorkflowService.approveOrder(orderId, approvedBy, notes);
        return ResponseEntity.ok("Order approved successfully with inventory processing");
    }

    /**
     * Từ chối order
     * GIỐNG: POST /api/workflow/orders/{orderId}/reject
     */
    @PostMapping("/orders/{orderId}/reject")
    public ResponseEntity<String> rejectOrder(
            @PathVariable Integer orderId,
            @RequestParam Integer rejectedBy,
            @RequestParam String reason) {
        orderWorkflowService.rejectOrder(orderId, rejectedBy, reason);
        return ResponseEntity.ok("Order rejected");
    }

    /**
     * Lấy orders chờ duyệt
     * GIỐNG: GET /api/workflow/orders/pending-approval
     */
    @GetMapping("/orders/pending-approval")
    public ResponseEntity<List<Order>> getOrdersPendingApproval() {
        List<Order> orders = orderWorkflowService.getOrdersPendingApproval();
        return ResponseEntity.ok(orders);
    }

    /**
     * Kiểm tra order có thể duyệt không
     * GIỐNG: GET /api/workflow/orders/{orderId}/can-approve
     */
    @GetMapping("/orders/{orderId}/can-approve")
    public ResponseEntity<Boolean> canApproveOrder(@PathVariable Integer orderId) {
        boolean canApprove = orderWorkflowService.canApproveOrder(orderId);
        return ResponseEntity.ok(canApprove);
    }

    // ==================== PAYMENT WORKFLOW ====================

    /**
     * Xử lý thanh toán với phần trăm
     */
    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<Payment> processPaymentWithPercentage(
            @PathVariable Integer orderId,
            @RequestBody PaymentRequestDTO paymentRequest) {
        paymentRequest.setOrderId(orderId);
        Payment payment = paymentProcessingService.processPaymentWithPercentage(paymentRequest);
        return ResponseEntity.ok(payment);
    }

    /**
     * Lấy thông tin thanh toán của order
     */
    @GetMapping("/orders/{orderId}/payments")
    public ResponseEntity<List<Payment>> getPaymentsByOrder(@PathVariable Integer orderId) {
        List<Payment> payments = paymentProcessingService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(payments);
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * Kiểm tra trạng thái quote
     */
    @GetMapping("/quotes/{quoteId}/status")
    public ResponseEntity<Map<String, Object>> getQuoteStatus(@PathVariable Integer quoteId) {
        Quote quote = quoteDealerStaffService.getQuoteById(quoteId);

        Map<String, Object> response = new HashMap<>();
        response.put("quoteId", quote.getId());
        response.put("status", quote.getStatus());
        response.put("approvalStatus", quote.getApprovalStatus());
        response.put("customerId", quote.getCustomerId());
        response.put("customerName", quote.getCustomer().getFullName());
        response.put("totalAmount", quote.getTotalAmount());
        response.put("finalTotal", quote.getFinalTotal());
        response.put("isVipCustomer", quote.getCustomer().getIsVip());

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách quotes của staff
     */
    @GetMapping("/staff/{staffId}/quotes")
    public ResponseEntity<List<Quote>> getQuotesByStaff(@PathVariable Integer staffId) {
        List<Quote> quotes = quoteDealerStaffService.getQuotesByStaff(staffId);
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/payment/vnpay-return")
    public ResponseEntity<Map<String, Object>> handleVNPayReturn(
            @RequestParam Map<String, String> params) {
        try {
            log.info("DEALER WORKFLOW - Handling VNPay return callback");

            // Xử lý payment thông qua service chung
            Payment payment = paymentProcessingService.processVNPayReturn(params);

            // 🔥 NẾU THANH TOÁN THÀNH CÔNG, CẬP NHẬT ORDER VÀ INVENTORY
            if ("COMPLETED".equals(payment.getStatus().toString())) {
                dealerOrderWorkflowService.handleVNPayPaymentSuccess(
                        payment.getOrderId(),
                        payment.getVnpayTxnRef()
                );
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", "00");
            response.put("message", "Payment processed successfully");
            response.put("paymentId", payment.getId());
            response.put("orderId", payment.getOrderId());
            response.put("status", payment.getStatus());
            response.put("transactionNo", payment.getVnpayTransactionNo());
            response.put("workflow", "DEALER");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Error processing VNPay return: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("code", "99");
            response.put("message", "Payment processing failed: " + e.getMessage());
            response.put("workflow", "DEALER");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán
     */
    @GetMapping("/orders/{orderId}/payment-status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Integer orderId) {
        BigDecimal totalPaid = paymentProcessingService.getTotalPaidAmountByOrder(orderId);
        List<Payment> payments = paymentProcessingService.getPaymentsByOrder(orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("totalPaid", totalPaid);
        response.put("payments", payments);
        response.put("paymentCount", payments.size());
        response.put("workflow", "DEALER");

        return ResponseEntity.ok(response);
    }
}