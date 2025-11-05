package com.example.demo.service;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealerOrderWorkflowService {

    private final OrderRepository orderRepository;
    private final QuoteRepository quoteRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final QuoteCalculationService quoteCalculationService;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final QuoteDetailRepository quoteDetailRepository;
    private final CustomerRepository customerRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final VNPayService vnPayService;

    /**
     * Tạo order từ approved quote với hỗ trợ thanh toán đa phương thức
     */
    public OrderResponseDTO createOrderFromApprovedQuote(OrderDTO orderDTO) {
        log.info("=== DEALER WORKFLOW - CREATE ORDER WITH PAYMENT - quoteId: {}, paymentMethod: {}, paymentPercentage: {}%",
                orderDTO.getQuoteId(), orderDTO.getPaymentMethod(), orderDTO.getPaymentPercentage());

        try {
            Optional<Order> existingOrder = orderRepository.findByQuoteId(orderDTO.getQuoteId());
            if (existingOrder.isPresent()) {
                Order existing = existingOrder.get();
                if (existing.getStatus() != Order.OrderStatus.CANCELLED) {
                    throw new RuntimeException("Order already exists for this quote: " + existing.getId());
                }
            }

            Quote quote = quoteRepository.findById(orderDTO.getQuoteId())
                    .orElseThrow(() -> new RuntimeException("Quote not found: " + orderDTO.getQuoteId()));

            if (!quote.canCreateOrder()) {
                throw new RuntimeException("Cannot create order from quote. Approval status: " +
                        quote.getApprovalStatus() + ", Quote status: " + quote.getStatus());
            }
            validatePaymentPercentage(orderDTO.getPaymentPercentage());

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

            // 🔥 CẬP NHẬT CUSTOMER: TOTAL SPENT, TOTAL DEBT, VIP STATUS
            updateCustomerAfterOrder(quote, orderEntity);

            log.info("DEALER WORKFLOW - Order created with payment - Total: {}, Paid: {} ({}%), Remaining: {}",
                    orderEntity.getTotalAmount(), orderEntity.getPaidAmount(),
                    orderDTO.getPaymentPercentage(), orderEntity.getRemainingAmount());

            // 🔥 XỬ LÝ THANH TOÁN NẾU CÓ paymentPercentage
            if (orderDTO.getPaymentPercentage() != null && orderDTO.getPaymentPercentage() > 0) {
                processDealerPayment(orderEntity, orderDTO);
            }

            orderResponse.setPaymentPercentage(orderDTO.getPaymentPercentage());
            orderResponse.setPaidAmount(orderEntity.getPaidAmount());
            orderResponse.setRemainingAmount(orderEntity.getRemainingAmount());
            orderResponse.setPaymentStatus(String.valueOf(orderEntity.getPaymentStatus()));

            auditLogService.log("DEALER_ORDER_CREATED_FROM_APPROVED_QUOTE", "ORDER", orderResponse.getId().toString(),
                    Map.of("quoteId", quote.getId(),
                            "dealerId", orderDTO.getDealerId(),
                            "paymentPercentage", orderDTO.getPaymentPercentage(),
                            "paymentMethod", orderDTO.getPaymentMethod(),
                            "totalAmount", orderEntity.getTotalAmount(),
                            "paidAmount", orderEntity.getPaidAmount(),
                            "remainingAmount", orderEntity.getRemainingAmount(),
                            "workflowType", "DEALER",
                            "approvalStatus", "PENDING_APPROVAL"));

            log.info("DEALER WORKFLOW - Order created from approved quote - Order: {}, Quote: {}, Payment: {}%",
                    orderResponse.getId(), quote.getId(), orderDTO.getPaymentPercentage());

            return orderResponse;

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Error creating order from approved quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order from quote in dealer workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật thông tin customer sau khi tạo order
     */
    private void updateCustomerAfterOrder(Quote quote, Order order) {
        try {
            Customer customer = customerRepository.findById(quote.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + quote.getCustomerId()));

            BigDecimal totalAmount = order.getTotalAmount();
            BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal remainingAmount = order.getRemainingAmount() != null ? order.getRemainingAmount() : BigDecimal.ZERO;
            BigDecimal currentTotalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
            customer.setTotalSpent(currentTotalSpent.add(paidAmount));
            BigDecimal currentTotalDebt = customer.getTotalDebt() != null ? customer.getTotalDebt() : BigDecimal.ZERO;
            customer.setTotalDebt(currentTotalDebt.add(remainingAmount));
            BigDecimal vipThreshold = new BigDecimal("5000000000");
            if (totalAmount.compareTo(vipThreshold) >= 0) {
                customer.setIsVip(true);
                log.info("Customer {} upgraded to VIP - Order total: {}", customer.getId(), totalAmount);
            }

            customerRepository.save(customer);

            log.info("Customer updated - ID: {}, TotalSpent: {} (+{}), TotalDebt: {} (+{}), IsVip: {}",
                    customer.getId(),
                    customer.getTotalSpent(), paidAmount,
                    customer.getTotalDebt(), remainingAmount,
                    customer.getIsVip());

            auditLogService.log("CUSTOMER_UPDATED_AFTER_ORDER", "CUSTOMER", customer.getId().toString(),
                    Map.of("orderId", order.getId(),
                            "totalAmount", totalAmount,
                            "paidAmount", paidAmount,
                            "remainingAmount", remainingAmount,
                            "newTotalSpent", customer.getTotalSpent(),
                            "newTotalDebt", customer.getTotalDebt(),
                            "isVip", customer.getIsVip()));

        } catch (Exception e) {
            log.error("Error updating customer after order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update customer: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý thanh toán cho dealer workflow (hỗ trợ VNPay và tiền mặt)
     */
    private void processDealerPayment(Order order, OrderDTO orderDTO) {
        try {
            log.info("DEALER WORKFLOW - Processing payment for order {} with method: {}",
                    order.getId(), orderDTO.getPaymentMethod());

            if ("TRANSFER".equalsIgnoreCase(orderDTO.getPaymentMethod())) {
                processVNPayPayment(order, orderDTO);
            } else {
                processCashPayment(order, orderDTO);
            }

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Payment processing failed for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý thanh toán VNPay - Tạo payment pending
     */
    private void processVNPayPayment(Order order, OrderDTO orderDTO) {
        try {
            log.info("DEALER WORKFLOW - Processing VNPay payment for order {}", order.getId());

            PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setPaymentPercentage(orderDTO.getPaymentPercentage());
            paymentRequest.setPaymentMethod("VNPAY");
            paymentRequest.setPaymentNotes("Dealer workflow - VNPay payment");
            Payment payment = createPendingPayment(paymentRequest);

            log.info("DEALER WORKFLOW - VNPay payment initiated - Order: {}, Payment: {}, TxnRef: {}, Amount: {}",
                    order.getId(), payment.getId(), payment.getVnpayTxnRef(), payment.getAmount());

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - VNPay payment failed: {}", e.getMessage(), e);
            throw new RuntimeException("VNPay payment initiation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo payment với status PENDING
     */
    private Payment createPendingPayment(PaymentRequestDTO paymentRequest) {
        try {
            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));
            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());
            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .amount(paymentAmount)
                    .paymentMethod(Payment.PaymentMethod.VNPAY)
                    .paymentPercentage(paymentRequest.getPaymentPercentage())
                    .status(Payment.Status.PENDING)
                    .notes(paymentRequest.getPaymentNotes())
                    .paymentDate(LocalDate.now())
                    .vnpayTxnRef(generateTxnRef())
                    .createdAt(java.time.LocalDateTime.now())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            log.info("Created pending payment - ID: {}, Order: {}, Amount: {}, Percentage: {}%",
                    payment.getId(), order.getId(), paymentAmount, paymentRequest.getPaymentPercentage());

            return payment;

        } catch (Exception e) {
            log.error("Error creating pending payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create pending payment: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý thanh toán tiền mặt - Hoàn tất ngay
     */
    private void processCashPayment(Order order, OrderDTO orderDTO) {
        try {
            log.info("DEALER WORKFLOW - Processing cash payment for order {}", order.getId());

            PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setPaymentPercentage(orderDTO.getPaymentPercentage());
            paymentRequest.setPaymentMethod("CASH");
            paymentRequest.setPaymentNotes("Dealer workflow - Cash payment");
            Payment payment = paymentProcessingService.processPaymentWithPercentage(paymentRequest);
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setApprovalStatus(Order.OrderApprovalStatus.APPROVED);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setPaidAmount(payment.getAmount());
            order.setRemainingAmount(order.getTotalAmount().subtract(payment.getAmount()));
            orderRepository.save(order);
            updateCustomerAfterPayment(order, payment.getAmount());
            updateDealerInventoryAfterPayment(order);

            log.info("DEALER WORKFLOW - Cash payment completed - Order: {}, Payment: {}, Amount: {}",
                    order.getId(), payment.getId(), payment.getAmount());

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Cash payment failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cash payment processing failed: " + e.getMessage(), e);
        }
    }

    private void updateCustomerAfterPayment(Order order, BigDecimal paymentAmount) {
        try {
            Customer customer = customerRepository.findById(order.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + order.getCustomerId()));

            BigDecimal currentTotalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
            customer.setTotalSpent(currentTotalSpent.add(paymentAmount));

            BigDecimal currentTotalDebt = customer.getTotalDebt() != null ? customer.getTotalDebt() : BigDecimal.ZERO;
            BigDecimal newTotalDebt = currentTotalDebt.subtract(paymentAmount);
            if (newTotalDebt.compareTo(BigDecimal.ZERO) < 0) {
                newTotalDebt = BigDecimal.ZERO; // Đảm bảo không âm
            }
            customer.setTotalDebt(newTotalDebt);

            customerRepository.save(customer);

            log.info("Customer updated after payment - ID: {}, TotalSpent: {} (+{}), TotalDebt: {} (-{})",
                    customer.getId(),
                    customer.getTotalSpent(), paymentAmount,
                    customer.getTotalDebt(), paymentAmount);

            auditLogService.log("CUSTOMER_UPDATED_AFTER_PAYMENT", "CUSTOMER", customer.getId().toString(),
                    Map.of("orderId", order.getId(),
                            "paymentAmount", paymentAmount,
                            "newTotalSpent", customer.getTotalSpent(),
                            "newTotalDebt", customer.getTotalDebt()));

        } catch (Exception e) {
            log.error("Error updating customer after payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update customer after payment: " + e.getMessage(), e);
        }
    }


    private BigDecimal calculatePaymentAmount(Order order, Integer paymentPercentage) {
        if (paymentPercentage == null || paymentPercentage <= 0) {
            throw new RuntimeException("Invalid payment percentage: " + paymentPercentage);
        }

        BigDecimal orderTotal = order.getTotalAmount();
        if (orderTotal == null) {
            throw new RuntimeException("Order total amount is null");
        }

        return orderTotal.multiply(BigDecimal.valueOf(paymentPercentage))
                .divide(BigDecimal.valueOf(100));
    }

    /**
     * Generate transaction reference
     */
    private String generateTxnRef() {
        return "DEALER_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }


    public void updateDealerInventoryAfterPayment(Order order) {
        try {
            log.info("DEALER WORKFLOW - Updating dealer inventory for order {}", order.getId());

            List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(order.getQuoteId());

            if (quoteDetails.isEmpty()) {
                throw new RuntimeException("No quote details found for order: " + order.getId());
            }

            Quote quote = quoteRepository.findById(order.getQuoteId())
                    .orElseThrow(() -> new RuntimeException("Quote not found: " + order.getQuoteId()));

            Customer customer = customerRepository.findById(quote.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + quote.getCustomerId()));

            Integer dealerId = customer.getDealerId();

            for (QuoteDetail detail : quoteDetails) {
                inventoryService.deductDealerInventory(
                        dealerId,
                        detail.getVehicleId(),
                        detail.getQuantity()
                );

                log.info("DEALER WORKFLOW - Deducted dealer inventory - Dealer: {}, Vehicle: {}, Quantity: {}",
                        dealerId, detail.getVehicleId(), detail.getQuantity());
            }
            Integer paymentPercentage = order.getPaymentPercentage();
            if (paymentPercentage == null) {
                paymentPercentage = 0;
                log.warn("Payment percentage is null for order {}, using default value 0", order.getId());
            }

            auditLogService.log("DEALER_INVENTORY_UPDATED", "ORDER", order.getId().toString(),
                    Map.of("dealerId", dealerId,
                            "paymentPercentage", paymentPercentage,
                            "vehiclesProcessed", quoteDetails.size()));

            log.info("DEALER WORKFLOW - Dealer inventory updated successfully for order {}", order.getId());

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Error updating dealer inventory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update dealer inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý VNPay return callback - Cập nhật payment và inventory
     */
    public void handleVNPayPaymentSuccess(Integer orderId, String txnRef) {
        try {
            log.info("DEALER WORKFLOW - Handling VNPay payment success - Order: {}, TxnRef: {}", orderId, txnRef);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            Payment payment = paymentRepository.findByVnpayTxnRef(txnRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + txnRef));
            payment.markAsCompleted(payment.getVnpayTransactionNo());
            paymentRepository.save(payment);
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setApprovalStatus(Order.OrderApprovalStatus.APPROVED);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setPaidAmount(payment.getAmount());
            order.setRemainingAmount(order.getTotalAmount().subtract(payment.getAmount()));
            orderRepository.save(order);
            updateCustomerAfterPayment(order, payment.getAmount());
            updateDealerInventoryAfterPayment(order);

            log.info("DEALER WORKFLOW - VNPay payment completed successfully - Order: {}, Payment: {}, Amount: {}",
                    orderId, payment.getId(), payment.getAmount());

        } catch (Exception e) {
            log.error("DEALER WORKFLOW - Error handling VNPay payment success: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle VNPay payment success: " + e.getMessage(), e);
        }
    }

    /**
     * Validate payment percentage theo business rules
     */
    private void validatePaymentPercentage(Integer paymentPercentage) {
        if (paymentPercentage == null) {
            throw new RuntimeException("Payment percentage is required");
        }
        boolean isValid = paymentPercentage == 30 || paymentPercentage == 50 ||
                paymentPercentage == 70 || paymentPercentage == 100;

        if (!isValid) {
            throw new RuntimeException("Invalid payment percentage. Allowed values: 30%, 50%, 70%, 100%");
        }
    }
}