package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.entity.Quote;
import com.example.demo.service.QuoteApprovalService;
import com.example.demo.service.OrderWorkflowService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class QuoteWorkflowController {

    private final QuoteApprovalService quoteApprovalService;
    private final OrderWorkflowService orderWorkflowService;

    // Dealer Manager gửi quote cho EVM duyệt
    @PostMapping("/quotes/{quoteId}/submit-for-approval")
    public ResponseEntity<String> submitForEVMApproval(@PathVariable Integer quoteId) {
        quoteApprovalService.submitForEVMApproval(quoteId);
        return ResponseEntity.ok("Quote submitted for EVM approval successfully");
    }

    // EVM duyệt quote
    @PostMapping("/quotes/{quoteId}/approve")
    public ResponseEntity<String> approveQuoteByEVM(
            @PathVariable Integer quoteId,
            @RequestParam Integer evmUserId,
            @RequestParam(required = false) String notes) {
        quoteApprovalService.approveQuoteByEVM(quoteId, evmUserId, notes);
        return ResponseEntity.ok("Quote approved by EVM successfully");
    }


    @PostMapping("/quotes/{quoteId}/reject")
    public ResponseEntity<String> rejectQuoteByEVM(
            @PathVariable Integer quoteId,
            @RequestParam Integer evmUserId,
            @RequestParam String reason) {
        quoteApprovalService.rejectQuoteByEVM(quoteId, evmUserId, reason);
        return ResponseEntity.ok("Quote rejected by EVM");
    }


    @GetMapping("/quotes/{quoteId}/check-inventory")
    public ResponseEntity<Map<String, Object>> checkInventoryForQuote(@PathVariable Integer quoteId) {
        boolean hasSufficientInventory = quoteApprovalService.checkFactoryInventoryForQuote(quoteId);

        Map<String, Object> response = new HashMap<>();
        response.put("quoteId", quoteId);
        response.put("hasSufficientInventory", hasSufficientInventory);
        response.put("message", hasSufficientInventory ?
                "Kho hãng đủ hàng" : "Kho hãng không đủ mẫu xe đang được đặt");

        return ResponseEntity.ok(response);
    }

    // Dealer tạo order từ quote đã approved
    @PostMapping("/orders/create-from-approved-quote")
    public ResponseEntity<OrderResponseDTO> createOrderFromApprovedQuote(@RequestBody OrderDTO orderDTO) {
        OrderResponseDTO order = orderWorkflowService.createOrderFromApprovedQuote(orderDTO);
        return ResponseEntity.ok(order);
    }

    // Lấy quotes chờ EVM duyệt
    @GetMapping("/quotes/pending-approval")
    public ResponseEntity<List<Quote>> getQuotesPendingEVMApproval() {
        List<Quote> quotes = quoteApprovalService.getQuotesPendingEVMApproval();
        return ResponseEntity.ok(quotes);
    }

    // Lấy quotes đã approved sẵn sàng tạo order
    @GetMapping("/quotes/approved-ready")
    public ResponseEntity<List<Quote>> getApprovedQuotesReadyForOrder() {
        List<Quote> quotes = quoteApprovalService.getApprovedQuotesReadyForOrder();
        return ResponseEntity.ok(quotes);
    }


    @GetMapping("/quotes/{quoteId}/can-create-order")
    public ResponseEntity<Boolean> canCreateOrderFromQuote(@PathVariable Integer quoteId) {
        boolean canCreate = orderWorkflowService.canCreateOrderFromQuote(quoteId);
        return ResponseEntity.ok(canCreate);
    }

    @PostMapping("/orders/{orderId}/approve")
    public ResponseEntity<String> approveOrder(
            @PathVariable Integer orderId,
            @RequestParam Integer approvedBy,
            @RequestParam(required = false) String notes) {
        orderWorkflowService.approveOrder(orderId, approvedBy, notes);
        return ResponseEntity.ok("Order approved successfully with inventory processing");
    }

    @PostMapping("/orders/{orderId}/reject")
    public ResponseEntity<String> rejectOrder(
            @PathVariable Integer orderId,
            @RequestParam Integer rejectedBy,
            @RequestParam String reason) {
        orderWorkflowService.rejectOrder(orderId, rejectedBy, reason);
        return ResponseEntity.ok("Order rejected");
    }


    @GetMapping("/orders/pending-approval")
    public ResponseEntity<List<com.example.demo.entity.Order>> getOrdersPendingApproval() {
        List<com.example.demo.entity.Order> orders = orderWorkflowService.getOrdersPendingApproval();
        return ResponseEntity.ok(orders);
    }


    @GetMapping("/orders/{orderId}/can-approve")
    public ResponseEntity<Boolean> canApproveOrder(@PathVariable Integer orderId) {
        boolean canApprove = orderWorkflowService.canApproveOrder(orderId);
        return ResponseEntity.ok(canApprove);
    }
}