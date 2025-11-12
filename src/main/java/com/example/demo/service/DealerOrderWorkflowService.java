package com.example.demo.service;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealerOrderWorkflowService {

    private final OrderWorkflowService orderWorkflowService;
    private final OrderService orderService;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final AuditLogService auditLogService;


    public OrderResponseDTO createOrderFromApprovedQuote(OrderDTO orderDTO, Integer staffId) {
        log.info("=== DEALER WORKFLOW - CREATE ORDER WITH PAYMENT - quoteId: {}, staffId: {}, paymentMethod: {}, paymentPercentage: {}%",
                orderDTO.getQuoteId(), staffId, orderDTO.getPaymentMethod(), orderDTO.getPaymentPercentage());

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new RuntimeException("Staff not found: " + staffId));

            if (staff.getRole() != User.Role.DEALER_STAFF) {
                throw new RuntimeException("Only DEALER_STAFF can create orders from quotes");
            }

            Optional<Order> existingOrder = orderRepository.findByQuoteId(orderDTO.getQuoteId());
            if (existingOrder.isPresent()) {
                Order existing = existingOrder.get();
                if (existing.getStatus() != Order.OrderStatus.CANCELLED) {
                    throw new RuntimeException("Order already exists for this quote: " + existing.getId());
                }
            }

            Quote quote = quoteRepository.findById(orderDTO.getQuoteId())
                    .orElseThrow(() -> new RuntimeException("Quote not found: " + orderDTO.getQuoteId()));

            if (!quote.canCreateOrder(staff)) {
                throw new RuntimeException("Staff can only create orders from their own approved quotes. " +
                        "Quote creator: " + quote.getUserId() + ", Current staff: " + staffId);
            }

            if (!staff.getDealerId().equals(quote.getDealerId())) {
                throw new RuntimeException("Staff does not belong to quote's dealer. " +
                        "Quote dealer: " + quote.getDealerId() + ", Staff dealer: " + staff.getDealerId());
            }

            if (!quote.canCreateOrder()) {
                throw new RuntimeException("Cannot create order from quote. Approval status: " +
                        quote.getApprovalStatus() + ", Quote status: " + quote.getStatus());
            }

            validatePaymentPercentage(orderDTO.getPaymentPercentage());
            orderDTO.setUserId(staffId); // Đảm bảo order được tạo bởi staff
            orderDTO.setStatus("PENDING");

            OrderResponseDTO orderResponse = orderService.createOrder(orderDTO);

            Order orderEntity = orderRepository.findById(orderResponse.getId())
                    .orElseThrow(() -> new RuntimeException("Order not found after creation: " + orderResponse.getId()));

            if (orderDTO.getPaymentPercentage() != null && orderDTO.getPaymentPercentage() > 0) {
                BigDecimal paidAmount = orderEntity.getTotalAmount()
                        .multiply(BigDecimal.valueOf(orderDTO.getPaymentPercentage()))
                        .divide(BigDecimal.valueOf(100));

                BigDecimal remainingAmount = orderEntity.getTotalAmount().subtract(paidAmount);

                orderEntity.setPaidAmount(paidAmount);
                orderEntity.setRemainingAmount(remainingAmount);
                orderEntity.setPaymentPercentage(orderDTO.getPaymentPercentage());

                if (orderDTO.getPaymentPercentage() == 100) {
                    orderEntity.setPaymentStatus(Order.PaymentStatus.PAID);
                } else {
                    orderEntity.setPaymentStatus(Order.PaymentStatus.PARTIALLY_PAID);
                }
            }

            orderEntity.setApprovalStatus(Order.OrderApprovalStatus.PENDING_APPROVAL);
            orderRepository.save(orderEntity);

            updateCustomerAfterOrder(quote, orderEntity);

            log.info("DEALER WORKFLOW - Order created with payment - Total: {}, Paid: {} ({}%), Remaining: {}, Staff: {}",
                    orderEntity.getTotalAmount(), orderEntity.getPaidAmount(),
                    orderDTO.getPaymentPercentage(), orderEntity.getRemainingAmount(), staffId);

            // 🔥 CẬP NHẬT: Gọi service xử lý thanh toán THỰC TẾ
            if (orderDTO.getPaymentPercentage() != null && orderDTO.getPaymentPercentage() > 0) {
                processDealerPayment(orderEntity, orderDTO);
            }

            orderResponse.setPaymentPercentage(orderDTO.getPaymentPercentage());
            orderResponse.setPaidAmount(orderEntity.getPaidAmount());
            orderResponse.setRemainingAmount(orderEntity.getRemainingAmount());
            orderResponse.setPaymentStatus(String.valueOf(orderEntity.getPaymentStatus()));
            orderResponse.setUserId(staffId);

            auditLogService.log("DEALER_ORDER_CREATED_FROM_APPROVED_QUOTE", "ORDER", orderResponse.getId().toString(),
                    Map.ofEntries(
                            Map.entry("quoteId", quote.getId()),
                            Map.entry("dealerId", orderDTO.getDealerId()),
                            Map.entry("staffId", staffId),
                            Map.entry("paymentPercentage", orderDTO.getPaymentPercentage()),
                            Map.entry("paymentMethod", orderDTO.getPaymentMethod()),
                            Map.entry("totalAmount", orderEntity.getTotalAmount()),
                            Map.entry("paidAmount", orderEntity.getPaidAmount()),
                            Map.entry("remainingAmount", orderEntity.getRemainingAmount()),
                            Map.entry("workflowType", "DEALER"),
                            Map.entry("approvalStatus", "PENDING_APPROVAL"),
                            Map.entry("quoteCreator", quote.getUserId()),
                            Map.entry("orderCreator", staffId)
                    ));

            log.info("DEALER WORKFLOW - Order created from approved quote - Order: {}, Quote: {}, Payment: {}%, Staff: {}",
                    orderResponse.getId(), quote.getId(), orderDTO.getPaymentPercentage(), staffId);

            return orderResponse;

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Error creating order from approved quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order from quote in dealer workflow: " + e.getMessage(), e);
        }
    }

    private void validatePaymentPercentage(Integer paymentPercentage) {
        if (paymentPercentage != null &&
                paymentPercentage != 0 &&
                paymentPercentage != 30 &&
                paymentPercentage != 50 &&
                paymentPercentage != 70 &&
                paymentPercentage != 100) {
            throw new RuntimeException("Invalid payment percentage. Must be 0, 30, 50, 70, or 100");
        }
    }

    private void updateCustomerAfterOrder(Quote quote, Order order) {
        try {
            Customer customer = customerRepository.findById(quote.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + quote.getCustomerId()));
            log.info("Before update - Customer ID: {}, TotalSpent: {}, TotalDebt: {}",
                    customer.getId(), customer.getTotalSpent(), customer.getTotalDebt());
            if (order.getPaidAmount() != null && order.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                customer.addToTotalSpent(order.getPaidAmount());
                log.info("Added to total spent: {}", order.getPaidAmount());
            }
            if (order.getRemainingAmount() != null && order.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                customer.addDebt(order.getRemainingAmount());
                log.info("Added to total debt: {}", order.getRemainingAmount());
            }

            customerRepository.save(customer);

            log.info("After update - Customer ID: {}, TotalSpent: {}, TotalDebt: {}, IsVIP: {}",
                    customer.getId(), customer.getTotalSpent(), customer.getTotalDebt(), customer.getIsVip());

        } catch (Exception e) {
            log.error("Error updating customer after order: {}", e.getMessage(), e);
        }
    }


    private void processDealerPayment(Order order, OrderDTO orderDTO) {
        try {
            if (orderDTO.getPaymentMethod() != null &&
                    orderDTO.getPaymentPercentage() != null &&
                    orderDTO.getPaymentPercentage() > 0) {

                log.info("Processing dealer payment - Order: {}, Method: {}, Percentage: {}%",
                        order.getId(), orderDTO.getPaymentMethod(), orderDTO.getPaymentPercentage());

                // Tạo payment request
                PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
                paymentRequest.setOrderId(order.getId());
                paymentRequest.setPaymentMethod(orderDTO.getPaymentMethod());
                paymentRequest.setPaymentPercentage(orderDTO.getPaymentPercentage());
                paymentRequest.setPaymentNotes("Payment at order creation - Staff: " + orderDTO.getUserId());

                // 🔥 GỌI SERVICE THỰC TẾ THEO PHƯƠNG THỨC THANH TOÁN
                if ("CASH".equalsIgnoreCase(orderDTO.getPaymentMethod())) {
                    paymentProcessingService.processCashPayment(paymentRequest);
                } else if ("TRANSFER".equalsIgnoreCase(orderDTO.getPaymentMethod())) {
                    paymentProcessingService.processBankTransferPayment(paymentRequest);
                } else if ("VNPAY".equalsIgnoreCase(orderDTO.getPaymentMethod())) {
                    // VNPay sẽ được xử lý riêng qua VNPayService
                    log.info("VNPay payment will be processed separately via VNPayService");
                } else {
                    throw new RuntimeException("Unsupported payment method: " + orderDTO.getPaymentMethod());
                }

                log.info("Successfully processed {} payment for order: {}, percentage: {}%",
                        orderDTO.getPaymentMethod(), order.getId(), orderDTO.getPaymentPercentage());
            }
        } catch (Exception e) {
            log.error("Error processing dealer payment: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    public void handleVNPayPaymentSuccess(Integer orderId, String vnpayTxnRef) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            log.info("VNPay payment success for order: {}, transaction: {}", orderId, vnpayTxnRef);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setVnpayTransactionRef(vnpayTxnRef);
            orderRepository.save(order);

            auditLogService.log("VNPAY_PAYMENT_SUCCESS", "ORDER", orderId.toString(),
                    Map.of("transactionRef", vnpayTxnRef, "paymentStatus", "PAID"));

        } catch (Exception e) {
            log.error("Error handling VNPay payment success: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle VNPay payment success: " + e.getMessage());
        }
    }
}