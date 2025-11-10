package com.example.demo.service;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.entity.QuoteDetail;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.QuoteDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final QuoteDetailRepository quoteDetailRepository;
    private final AuditLogService auditLogService;


    public Payment processVNPayReturn(Map<String, String> params) {
        try {
            log.info("PROCESSING VNPay RETURN");

            if (!vnPayService.validateResponse(params)) {
                throw new RuntimeException("Invalid signature");
            }

            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTransactionNo = params.get("vnp_TransactionNo");

            log.info("Processing payment: TxnRef={}, ResponseCode={}", vnpTxnRef, vnpResponseCode);

            Payment payment = paymentRepository.findByVnpayTxnRef(vnpTxnRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + vnpTxnRef));

            payment.setVnpayTransactionNo(vnpTransactionNo);
            payment.setVnpayBankCode(params.get("vnp_BankCode"));
            payment.setVnpayCardType(params.get("vnp_CardType"));
            payment.setVnpayResponseCode(vnpResponseCode);

            String vnpPayDate = params.get("vnp_PayDate");
            if (vnpPayDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                payment.setVnpayPayDate(LocalDateTime.parse(vnpPayDate, formatter));
            }

            if ("00".equals(vnpResponseCode)) {
                payment.markAsCompleted(vnpTransactionNo);
                payment.setNotes("Payment successful via VNPay");
                log.info("Payment completed: {}", payment.getId());

                // ✅ CẬP NHẬT KHO KHI THANH TOÁN THÀNH CÔNG
                updateInventoryAfterSuccessfulPayment(payment);

            } else {
                payment.markAsFailed();
                payment.setNotes("Payment failed. Error code: " + vnpResponseCode);
                log.warn("Payment failed: {}", vnpResponseCode);
            }

            payment = paymentRepository.save(payment);
            log.info("PROCESSING COMPLETED");
            return payment;

        } catch (Exception e) {
            log.error("ERROR PROCESSING PAYMENT: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing error: " + e.getMessage(), e);
        }
    }


    protected void updateInventoryAfterSuccessfulPayment(Payment payment) {
        try {
            // 🔥 GIẢI PHÁP TỐI ƯU: Luôn lấy order từ repository bằng orderId
            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + payment.getOrderId()));

            log.info("Updating inventory after successful payment - Payment: {}, Order: {}, Amount: {}",
                    payment.getId(), order.getId(), payment.getAmount());

            transferInventoryFromFactoryToDealer(order);
            updateOrderStatusAfterPayment(order);

            auditLogService.log("INVENTORY_UPDATED_AFTER_PAYMENT", "ORDER", order.getId().toString(),
                    Map.of("paymentId", payment.getId(),
                            "amount", payment.getAmount(),
                            "dealerId", order.getDealerId(),
                            "paymentMethod", payment.getPaymentMethod().name()));

            log.info("Inventory updated successfully for order: {}", order.getId());

        } catch (Exception e) {
            log.error("Error updating inventory after payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update inventory: " + e.getMessage(), e);
        }
    }

    private void transferInventoryFromFactoryToDealer(Order order) {
        try {
            // Lấy quote details từ order
            List<QuoteDetail> quoteDetails = quoteDetailRepository.findByQuoteId(order.getQuoteId());

            if (quoteDetails.isEmpty()) {
                throw new RuntimeException("No quote details found for order: " + order.getId());
            }

            for (QuoteDetail detail : quoteDetails) {
                // ✅ SỬ DỤNG PHƯƠNG THỨC CÓ SẴN TRONG InventoryService
                inventoryService.transferFactoryToDealer(
                        order.getDealerId(),
                        detail.getVehicleId(),
                        detail.getQuantity()
                );
                log.info("Transferred inventory - Dealer: {}, Vehicle: {}, Quantity: {}",
                        order.getDealerId(), detail.getVehicleId(), detail.getQuantity());
            }
        } catch (Exception e) {
            log.error("Error transferring inventory from factory to dealer: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateOrderStatusAfterPayment(Order order) {
        try {
            order.setStatus(Order.OrderStatus.COMPLETED);
            if (order.getPaymentPercentage() != null && order.getPaymentPercentage() == 100) {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
            } else {
                order.setPaymentStatus(Order.PaymentStatus.PARTIALLY_PAID);
            }

            orderRepository.save(order);
            log.info("Order status updated to COMPLETED, Payment: {} - Order: {}",
                    order.getPaymentStatus(), order.getId());
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Các methods khác giữ nguyên
    public List<Payment> getPaymentsByOrder(Integer orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Payment getPaymentByTxnRef(String txnRef) {
        return paymentRepository.findByVnpayTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found with TxnRef: " + txnRef));
    }

    public BigDecimal getTotalPaidAmountByOrder(Integer orderId) {
        return paymentRepository.getTotalPaidAmountByOrderId(orderId);
    }

    @Transactional
    public Payment processPaymentWithPercentage(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing payment with percentage - Order: {}, Method: {}, Percentage: {}%",
                    paymentRequest.getOrderId(), paymentRequest.getPaymentMethod(), paymentRequest.getPaymentPercentage());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));

            // Tính toán số tiền thanh toán
            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());

            // 🔥 CẬP NHẬT: Xác định status dựa trên payment method
            Payment.Status paymentStatus = Payment.PaymentMethod.CASH.name().equals(paymentRequest.getPaymentMethod())
                    ? Payment.Status.COMPLETED  // Tiền mặt: hoàn thành ngay
                    : Payment.Status.PENDING;   // Chuyển khoản: chờ xử lý

            // Tạo payment
            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .amount(paymentAmount)
                    .paymentMethod(Payment.PaymentMethod.valueOf(paymentRequest.getPaymentMethod()))
                    .paymentPercentage(paymentRequest.getPaymentPercentage())
                    .status(paymentStatus)
                    .notes(paymentRequest.getPaymentNotes())
                    .paymentDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            // 🔥 QUAN TRỌNG: Nếu là tiền mặt, cập nhật inventory ngay
            if (Payment.PaymentMethod.CASH.name().equals(paymentRequest.getPaymentMethod())) {
                updateInventoryAfterSuccessfulPayment(payment);
            }

            log.info("Payment processed successfully - ID: {}, Order: {}, Method: {}, Amount: {}, Status: {}",
                    payment.getId(), order.getId(), paymentRequest.getPaymentMethod(), paymentAmount, paymentStatus);

            return payment;

        } catch (Exception e) {
            log.error("Error processing payment with percentage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage(), e);
        }
    }


    public Payment processCashPayment(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing cash payment - Order: {}, Percentage: {}%",
                    paymentRequest.getOrderId(), paymentRequest.getPaymentPercentage());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Tính toán số tiền thanh toán
            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());

            // Tạo payment record với status COMPLETED
            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .amount(paymentAmount)
                    .paymentMethod(Payment.PaymentMethod.CASH)
                    .paymentPercentage(paymentRequest.getPaymentPercentage())
                    .status(Payment.Status.COMPLETED)
                    .notes(paymentRequest.getPaymentNotes())
                    .paymentDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            // 🔥 CẬP NHẬT KHO NGAY (vì đã nhận tiền mặt)
            updateInventoryAfterSuccessfulPayment(payment);

            log.info("Cash payment processed successfully - ID: {}, Order: {}, Amount: {}",
                    payment.getId(), order.getId(), paymentAmount);

            return payment;

        } catch (Exception e) {
            log.error("Error processing cash payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process cash payment: " + e.getMessage(), e);
        }
    }


    public Payment processBankTransferPayment(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing bank transfer payment - Order: {}, Percentage: {}%",
                    paymentRequest.getOrderId(), paymentRequest.getPaymentPercentage());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Tính toán số tiền thanh toán
            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());

            // Tạo payment record với status PENDING (chờ xác nhận chuyển khoản)
            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .amount(paymentAmount)
                    .paymentMethod(Payment.PaymentMethod.TRANSFER)
                    .paymentPercentage(paymentRequest.getPaymentPercentage())
                    .status(Payment.Status.PENDING)
                    .notes(paymentRequest.getPaymentNotes())
                    .paymentDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            log.info("Bank transfer payment processed - ID: {}, Order: {}, Amount: {}, Status: PENDING",
                    payment.getId(), order.getId(), paymentAmount);

            // 🔥 KHÔNG cập nhật inventory ngay - chờ xác nhận chuyển khoản

            return payment;

        } catch (Exception e) {
            log.error("Error processing bank transfer payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process bank transfer payment: " + e.getMessage(), e);
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
}