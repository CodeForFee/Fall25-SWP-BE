package com.example.demo.service;

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

    @Transactional
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

    @Transactional
    protected void updateInventoryAfterSuccessfulPayment(Payment payment) {
        try {
            log.info("Updating inventory after successful payment for order: {}", payment.getOrder().getId());

            Order order = payment.getOrder();

            // ✅ SỬ DỤNG PHƯƠNG THỨC CÓ SẴN: transferFactoryToDealer
            transferInventoryFromFactoryToDealer(order);

            // ✅ CẬP NHẬT TRẠNG THÁI ORDER
            updateOrderStatusAfterPayment(order);

            auditLogService.log("INVENTORY_UPDATED_AFTER_PAYMENT", "ORDER", order.getId().toString(),
                    Map.of("paymentId", payment.getId(),
                            "amount", payment.getAmount(),
                            "dealerId", order.getDealerId()));

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
            orderRepository.save(order);
            log.info("Order status updated to COMPLETED: {}", order.getId());
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
}
