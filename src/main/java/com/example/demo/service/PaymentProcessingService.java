package com.example.demo.service;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
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

                log.info("✅ VNPay payment completed - Order: {}, Amount: {}, Inventory deduction postponed until delivery",
                        payment.getOrderId(), payment.getAmount());

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

    public Payment getPaymentByTxnRef(String txnRef) {
        return paymentRepository.findByVnpayTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found with TxnRef: " + txnRef));
    }

    public List<Payment> getPaymentsByOrder(Integer orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public BigDecimal getTotalPaidAmountByOrder(Integer orderId) {
        return paymentRepository.getTotalPaidAmountByOrderId(orderId);
    }

    public Payment processPaymentWithPercentage(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing payment with percentage - Order: {}, Method: {}, Percentage: {}%",
                    paymentRequest.getOrderId(), paymentRequest.getPaymentMethod(), paymentRequest.getPaymentPercentage());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + paymentRequest.getOrderId()));

            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());

            Payment.Status paymentStatus = Payment.PaymentMethod.CASH.name().equals(paymentRequest.getPaymentMethod())
                    ? Payment.Status.COMPLETED
                    : Payment.Status.PENDING;

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

            log.info("Payment processed successfully - ID: {}, Order: {}, Method: {}, Amount: {}, Status: {}",
                    payment.getId(), order.getId(), paymentRequest.getPaymentMethod(), paymentAmount, paymentStatus);

            return payment;

        } catch (Exception e) {
            log.error("Error processing payment with percentage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage(), e);
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

    public Payment processCashPayment(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing cash payment - Order: {}, Percentage: {}%",
                    paymentRequest.getOrderId(), paymentRequest.getPaymentPercentage());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());
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

            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());
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

            return payment;

        } catch (Exception e) {
            log.error("Error processing bank transfer payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process bank transfer payment: " + e.getMessage(), e);
        }
    }

    public Payment createPaymentRecordOnly(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Creating payment record only (no inventory deduction) - Order: {}, Method: {}, Percentage: {}%",
                    paymentRequest.getOrderId(), paymentRequest.getPaymentMethod(), paymentRequest.getPaymentPercentage());

            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            BigDecimal paymentAmount = calculatePaymentAmount(order, paymentRequest.getPaymentPercentage());

            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .amount(paymentAmount)
                    .paymentMethod(Payment.PaymentMethod.valueOf(paymentRequest.getPaymentMethod()))
                    .paymentPercentage(paymentRequest.getPaymentPercentage())
                    .status(Payment.Status.PENDING)
                    .notes("Payment recorded at order creation - Inventory deduction postponed until delivery")
                    .paymentDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            log.info("Payment record created (no inventory) - ID: {}, Order: {}, Method: {}, Amount: {}",
                    payment.getId(), order.getId(), paymentRequest.getPaymentMethod(), paymentAmount);

            return payment;

        } catch (Exception e) {
            log.error("Error creating payment record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment record: " + e.getMessage(), e);
        }
    }
}