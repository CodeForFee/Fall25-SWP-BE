package com.example.demo.service;

import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
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

    // THÊM CÁC METHODS BỊ THIẾU
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