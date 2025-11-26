package com.example.demo.service;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Dealer;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DealerRepository;
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

    //  – để load Customer/Dealer đúng
    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;


    //  – Cập nhật công nợ + order sau khi thanh toán
    @Transactional
    public void updateDebtAndOrderAfterPayment(Payment payment) {

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + payment.getOrderId()));

        // ===== 1. UPDATE ORDER PAYMENT =====
        BigDecimal oldPaid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal newPaid = oldPaid.add(payment.getAmount());
        order.setPaidAmount(newPaid);

        BigDecimal newRemaining = order.getTotalAmount().subtract(newPaid);
        if (newRemaining.compareTo(BigDecimal.ZERO) < 0) newRemaining = BigDecimal.ZERO;

        order.setRemainingAmount(newRemaining);
        order.setLastPaymentDate(LocalDateTime.now());

        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.PARTIALLY_PAID);
        }

        orderRepository.save(order);


        // ===== 2. UPDATE CUSTOMER DEBT =====
        if (order.getCustomerId() != null) {
            Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
            if (customer != null) {
                customer.reduceDebt(payment.getAmount());
                customerRepository.save(customer);
            }
        }

        // ===== 3. UPDATE DEALER DEBT =====
        if (order.getDealerId() != null) {
            Dealer dealer = dealerRepository.findById(order.getDealerId()).orElse(null);
            if (dealer != null) {
                BigDecimal oldDebt = dealer.getOutstandingDebt() == null ?
                        BigDecimal.ZERO : dealer.getOutstandingDebt();

                BigDecimal newDebt = oldDebt.subtract(payment.getAmount());
                if (newDebt.compareTo(BigDecimal.ZERO) < 0) newDebt = BigDecimal.ZERO;

                dealer.setOutstandingDebt(newDebt);
                dealerRepository.save(dealer);
            }
        }
    }


    // =====================================================================
    // ⚡ GIỮ NGUYÊN CODE CŨ – CHỈ THÊM GỌI UPDATE DEBT
    // =====================================================================
    public Payment processVNPayReturn(Map<String, String> params) {
        try {
            log.info("PROCESSING VNPay RETURN");

            if (!vnPayService.validateResponse(params)) {
                throw new RuntimeException("Invalid signature");
            }

            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTransactionNo = params.get("vnp_TransactionNo");

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

                //  cập nhật công nợ + order
                updateDebtAndOrderAfterPayment(payment);

                log.info("Payment completed: {}", payment.getId());
            } else {
                payment.markAsFailed();
                payment.setNotes("Payment failed. Error code: " + vnpResponseCode);
            }

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Payment processing error: " + e.getMessage(), e);
        }
    }


    // ORIGINAL CODE — GIỮ NGUYÊN
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


            //  Nếu thanh toán thành công → cập nhật công nợ
            if (paymentStatus == Payment.Status.COMPLETED) {
                updateDebtAndOrderAfterPayment(payment);
            }

            return payment;

        } catch (Exception e) {
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


            //  cập nhật công nợ sau khi thanh toán
            updateDebtAndOrderAfterPayment(payment);

            return payment;

        } catch (Exception e) {
            throw new RuntimeException("Failed to process cash payment: " + e.getMessage(), e);
        }
    }

    public Payment processBankTransferPayment(PaymentRequestDTO paymentRequest) {
        try {
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

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process bank transfer payment: " + e.getMessage(), e);
        }
    }


    public Payment createPaymentRecordOnly(PaymentRequestDTO paymentRequest) {
        try {
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

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment record: " + e.getMessage(), e);
        }
    }
}
