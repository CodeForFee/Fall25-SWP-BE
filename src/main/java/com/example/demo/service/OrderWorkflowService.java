package com.example.demo.service;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.repository.QuoteDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private final QuoteRepository quoteRepository;
    private final QuoteDetailRepository quoteDetailRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;


    public OrderResponseDTO createOrderFromApprovedQuote(OrderDTO orderDTO) {
        log.info("=== START createOrderFromApprovedQuote - quoteId: {}", orderDTO.getQuoteId());

        try {
            Quote quote = quoteRepository.findById(orderDTO.getQuoteId())
                    .orElseThrow(() -> new RuntimeException("Quote not found: " + orderDTO.getQuoteId()));

            if (!quote.canCreateOrder()) {
                throw new RuntimeException("Cannot create order from quote. Approval status: " +
                        quote.getApprovalStatus() + ", Quote status: " + quote.getStatus());
            }

            orderDTO.setStatus("PENDING");
            OrderResponseDTO order = orderService.createOrder(orderDTO);

            Order orderEntity = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new RuntimeException("Order not found after creation: " + order.getId()));

            orderEntity.setApprovalStatus(Order.OrderApprovalStatus.PENDING_APPROVAL);
            orderRepository.save(orderEntity);

            auditLogService.log("ORDER_CREATED_FROM_APPROVED_QUOTE", "ORDER", order.getId().toString(),
                    Map.of("quoteId", quote.getId(), "dealerId", orderDTO.getDealerId(),
                            "approvalStatus", "PENDING_APPROVAL"));

            log.info("Order created from approved quote - Order: {}, Quote: {}, Status: PENDING_APPROVAL",
                    order.getId(), quote.getId());

            return order;

        } catch (Exception e) {
            log.error("Error creating order from approved quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order from quote: " + e.getMessage(), e);
        }
    }


    public void approveOrder(Integer orderId, Integer approvedBy, String notes) {
        log.info("=== START approveOrder - orderId: {}", orderId);

        try {
            if (!canApproveOrder(orderId)) {
                log.warn("Cannot approve order {} - conditions not met", orderId);
                handleInsufficientInventory(orderId);
                return;
            }
            performOrderApproval(orderId, approvedBy, notes);

            log.info("=== END approveOrder successfully - orderId: {}", orderId);

        } catch (Exception e) {
            log.error("Error approving order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to approve order: " + e.getMessage(), e);
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleInsufficientInventory(Integer orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            order.setApprovalStatus(Order.OrderApprovalStatus.INSUFFICIENT_INVENTORY);
            order.setApprovalNotes("Không thể duyệt order: Kho hãng không đủ hàng");
            orderRepository.save(order);

            auditLogService.log("ORDER_INSUFFICIENT_INVENTORY", "ORDER", orderId.toString(),
                    Map.of("reason", "Insufficient factory inventory"));

            log.info("Order {} marked as INSUFFICIENT_INVENTORY", orderId);
        } catch (Exception e) {
            log.error("Error handling insufficient inventory for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void performOrderApproval(Integer orderId, Integer approvedBy, String notes) {
        log.info("Starting order approval transaction - orderId: {}", orderId);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            if (!order.canBeApproved()) {
                throw new RuntimeException("Order cannot be approved. Current status: " +
                        order.getApprovalStatus() + ", " + order.getStatus());
            }

            log.info("Updating order status to APPROVED");

            order.setApprovalStatus(Order.OrderApprovalStatus.APPROVED);
            order.setStatus(Order.OrderStatus.APPROVED);
            order.setApprovedBy(approvedBy);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovalNotes(notes);

            orderRepository.save(order);

            auditLogService.log("ORDER_APPROVED", "ORDER", orderId.toString(),
                    Map.of("approvedBy", approvedBy, "notes", notes));

            log.info("Order {} approved by user {}", orderId, approvedBy);

        } catch (Exception e) {
            log.error("Error in performOrderApproval for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectOrder(Integer orderId, Integer rejectedBy, String reason) {
        log.info("Rejecting order {} by user {}", orderId, rejectedBy);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            order.setApprovalStatus(Order.OrderApprovalStatus.REJECTED);
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setApprovedBy(rejectedBy);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovalNotes(reason);
            orderRepository.save(order);

            auditLogService.log("ORDER_REJECTED", "ORDER", orderId.toString(),
                    Map.of("rejectedBy", rejectedBy, "reason", reason));

            log.info("Order {} rejected by user {}", orderId, rejectedBy);

        } catch (Exception e) {
            log.error("Error rejecting order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private boolean checkInventoryAvailability(List<QuoteDetail> quoteDetails) {
        for (QuoteDetail detail : quoteDetails) {
            try {
                if (!inventoryService.checkFactoryInventory(detail.getVehicleId(), detail.getQuantity())) {
                    log.warn("Factory insufficient inventory - Vehicle: {}, Required: {}",
                            detail.getVehicleId(), detail.getQuantity());
                    return false;
                }
            } catch (Exception e) {
                log.error("Error checking inventory for vehicle {}: {}",
                        detail.getVehicleId(), e.getMessage());
                return false;
            }
        }
        return true;
    }

    private boolean checkFactoryInventoryForOrder(Quote quote) {
        List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(quote.getId());
        return checkInventoryAvailability(quoteDetails);
    }

    public List<Order> getOrdersPendingApproval() {
        return orderRepository.findOrdersPendingApproval();
    }

    public boolean canApproveOrder(Integer orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            if (!order.canBeApproved()) {
                log.warn("Order {} cannot be approved due to status", orderId);
                return false;
            }

            Quote quote = quoteRepository.findById(order.getQuoteId())
                    .orElseThrow(() -> new RuntimeException("Quote not found for order: " + order.getQuoteId()));

            boolean inventoryOk = checkFactoryInventoryForOrder(quote);

            if (!inventoryOk) {
                log.warn("Order {} cannot be approved due to insufficient inventory", orderId);
            }

            return inventoryOk;

        } catch (Exception e) {
            log.error("Error checking if order can be approved: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean canCreateOrderFromQuote(Integer quoteId) {
        return quoteRepository.findById(quoteId)
                .map(Quote::canCreateOrder)
                .orElse(false);
    }

    public void checkTransactionStatus(Integer orderId) {
        log.info("Checking transaction status for order: {}", orderId);

        boolean canApprove = canApproveOrder(orderId);
        Order order = orderRepository.findById(orderId).orElse(null);

        log.info("Order {} - canApprove: {}, current status: {}, approval status: {}",
                orderId, canApprove,
                order != null ? order.getStatus() : "NOT_FOUND",
                order != null ? order.getApprovalStatus() : "NOT_FOUND");
    }
}