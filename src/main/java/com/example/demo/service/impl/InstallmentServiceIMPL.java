package com.example.demo.service.impl;

import com.example.demo.dto.InstallmentRequest;
import com.example.demo.dto.InstallmentScheduleDTO;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Triển khai logic xử lý chia nhỏ thanh toán thành các kỳ trả góp
 */
@Service
@RequiredArgsConstructor
public class InstallmentServiceIMPL implements InstallmentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Tạo kế hoạch trả góp (chia tiền thành nhiều kỳ)
     */
    @Override
    public List<InstallmentScheduleDTO> createInstallmentPlan(InstallmentRequest req) {
        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment ID: " + req.getPaymentId()));

        BigDecimal total = req.getTotalAmount();
        int months = req.getMonths();
        BigDecimal rate = Optional.ofNullable(req.getAnnualInterestRate()).orElse(BigDecimal.ZERO);
        LocalDate firstDue = Optional.ofNullable(req.getFirstDueDate()).orElse(LocalDate.now().plusDays(30));

        // Tính lãi suất hàng tháng
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment;

        // Nếu không có lãi -> chia đều
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        } else {
            // Công thức tính số tiền trả góp có lãi
            BigDecimal onePlusRPowerN = BigDecimal.ONE.add(monthlyRate).pow(months);
            monthlyPayment = total.multiply(monthlyRate).multiply(onePlusRPowerN)
                    .divide(onePlusRPowerN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        }

        List<InstallmentScheduleDTO> schedules = new ArrayList<>();

        for (int i = 1; i <= months; i++) {
            LocalDate dueDate = firstDue.plusMonths(i - 1);

            Transaction transaction = Transaction.builder()
                    .payment(payment)
                    .installmentNumber(i)
                    .amount(monthlyPayment)
                    .dueDate(dueDate)
                    .status(Transaction.Status.PENDING)
                    .method("INSTALLMENT")
                    .note("Kỳ " + i + "/" + months)
                    .build();

            transactionRepository.save(transaction);

            schedules.add(
                    InstallmentScheduleDTO.builder()
                            .transactionId(transaction.getId())
                            .installmentNumber(i)
                            .amount(monthlyPayment)
                            .dueDate(dueDate)
                            .status(transaction.getStatus().name())
                            .build()
            );
        }

        return schedules;
    }

    /**
     * Lấy danh sách các kỳ trả góp theo Payment ID
     */
    @Override
    public List<InstallmentScheduleDTO> getInstallments(Integer paymentId) {
        List<Transaction> list = transactionRepository.findByPaymentIdOrderByInstallmentNumber(paymentId);
        List<InstallmentScheduleDTO> result = new ArrayList<>();
        for (Transaction t : list) {
            result.add(InstallmentScheduleDTO.builder()
                    .transactionId(t.getId())
                    .installmentNumber(t.getInstallmentNumber())
                    .amount(t.getAmount())
                    .dueDate(t.getDueDate())
                    .status(t.getStatus().name())
                    .build());
        }
        return result;
    }

    /**
     * Đánh dấu một kỳ là đã thanh toán
     */
    @Override
    public void markAsPaid(Integer transactionId, String method) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Transaction ID: " + transactionId));

        tx.setStatus(Transaction.Status.PAID);
        tx.setPaymentDate(LocalDateTime.now());
        tx.setMethod(method);
        transactionRepository.save(tx);

        // Nếu tất cả kỳ đều đã thanh toán -> cập nhật Payment thành COMPLETED
        List<Transaction> all = transactionRepository.findByPaymentIdOrderByInstallmentNumber(tx.getPayment().getId());
        boolean allPaid = all.stream().allMatch(t -> t.getStatus() == Transaction.Status.PAID);

        if (allPaid) {
            Payment payment = tx.getPayment();
            payment.setStatus(Payment.Status.COMPLETED);
            paymentRepository.save(payment);
        }
    }
}
