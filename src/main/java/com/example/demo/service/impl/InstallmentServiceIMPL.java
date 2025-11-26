package com.example.demo.service.impl;

import com.example.demo.dto.InstallmentPlanDTO;
import com.example.demo.dto.InstallmentRequest;
import com.example.demo.dto.InstallmentScheduleDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Dealer;
import com.example.demo.entity.InstallmentSchedule;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.repository.InstallmentScheduleRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.InstallmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentServiceIMPL implements InstallmentService {

    private final InstallmentScheduleRepository installmentScheduleRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 1. Xem trước kế hoạch trả góp (Không lưu DB)
     */
    @Override
    public InstallmentPlanDTO previewInstallmentPlan(InstallmentRequest req) {

        BigDecimal totalAmount = req.getTotalAmount();
        int months = req.getMonths();
        BigDecimal annualRate = req.getAnnualInterestRate(); // 0–100
        LocalDate firstDue = req.getFirstDueDate();

        // Lãi suất đơn giản (flat rate)
        BigDecimal totalInterest = totalAmount
                .multiply(annualRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalPayable = totalAmount.add(totalInterest);

        // Trả đều theo tháng
        BigDecimal monthlyPayment = totalPayable
                .divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        List<InstallmentScheduleDTO> schedule = new ArrayList<>();

        for (int i = 1; i <= months; i++) {
            schedule.add(InstallmentScheduleDTO.builder()
                    .installmentNumber(i)
                    .amount(monthlyPayment)
                    .dueDate(firstDue.plusMonths(i - 1))
                    .paidDate(null)
                    .status("PENDING")
                    .build());
        }

        return InstallmentPlanDTO.builder()
                .totalAmount(totalAmount)
                .interestAmount(totalInterest)
                .totalPayable(totalPayable)
                .monthlyPayment(monthlyPayment)
                .months(months)
                .firstDueDate(firstDue)
                .schedule(schedule)
                .build();
    }

    /**
     * 2. Tạo lịch trả góp thực tế vào DB
     */
    @Override
    @Transactional
    public List<InstallmentSchedule> generateSchedule(Integer orderId, InstallmentRequest req) {

        InstallmentPlanDTO plan = previewInstallmentPlan(req);
        List<InstallmentSchedule> entities = new ArrayList<>();

        for (InstallmentScheduleDTO dto : plan.getSchedule()) {
            entities.add(InstallmentSchedule.builder()
                    .orderId(orderId)
                    .installmentNumber(dto.getInstallmentNumber())
                    .amount(dto.getAmount())
                    .dueDate(dto.getDueDate())
                    .status(InstallmentSchedule.InstallmentStatus.PENDING)
                    .build());
        }

        List<InstallmentSchedule> saved = installmentScheduleRepository.saveAll(entities);
        log.info("Đã tạo {} kỳ trả góp cho Order ID: {}", saved.size(), orderId);

        return saved;
    }

    /**
     * 3. Thanh toán 1 kỳ trả góp + cập nhật công nợ + tạo Payment record
     */
    @Override
    @Transactional
    public InstallmentSchedule markInstallmentPaid(Integer scheduleId) {

        InstallmentSchedule schedule = installmentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kỳ trả góp."));

        // Đánh dấu đã thanh toán
        schedule.setStatus(InstallmentSchedule.InstallmentStatus.PAID);
        schedule.setPaidDate(LocalDate.now());
        schedule.setNote("Thanh toán kỳ trả góp #" + schedule.getInstallmentNumber());

        BigDecimal paymentAmount = schedule.getAmount();

        // Lấy Order
        Order order = orderRepository.findById(schedule.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        // ======= UPDATE CUSTOMER DEBT =======
        if (order.getCustomerId() != null) {
            customerRepository.findById(order.getCustomerId()).ifPresent(customer -> {

                if (customer.getTotalDebt() == null)
                    customer.setTotalDebt(BigDecimal.ZERO);

                BigDecimal newDebt = customer.getTotalDebt().subtract(paymentAmount);
                if (newDebt.compareTo(BigDecimal.ZERO) < 0) newDebt = BigDecimal.ZERO;

                customer.setTotalDebt(newDebt);
                customerRepository.save(customer);

                log.info("Cập nhật công nợ khách hàng {}: -{} → {}",
                        customer.getFullName(), paymentAmount, newDebt);
            });
        }

        // ======= UPDATE DEALER DEBT =======
        if (order.getDealerId() != null) {
            dealerRepository.findById(order.getDealerId()).ifPresent(dealer -> {

                if (dealer.getOutstandingDebt() == null)
                    dealer.setOutstandingDebt(BigDecimal.ZERO);

                BigDecimal newDebt = dealer.getOutstandingDebt().subtract(paymentAmount);
                if (newDebt.compareTo(BigDecimal.ZERO) < 0) newDebt = BigDecimal.ZERO;

                dealer.setOutstandingDebt(newDebt);
                dealerRepository.save(dealer);

                log.info("Cập nhật công nợ đại lý {}: -{} → {}",
                        dealer.getName(), paymentAmount, newDebt);
            });
        }

        // ======= UPDATE ORDER PAID AMOUNT =======
        BigDecimal oldPaid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal newPaid = oldPaid.add(paymentAmount);
        order.setPaidAmount(newPaid);

        BigDecimal remaining = order.getTotalAmount().subtract(newPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        order.setRemainingAmount(remaining);

        if (remaining.compareTo(BigDecimal.ZERO) == 0)
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        else
            order.setPaymentStatus(Order.PaymentStatus.PARTIALLY_PAID);

        order.setLastPaymentDate(LocalDateTime.now());
        orderRepository.save(order);

        // ======= CREATE PAYMENT HISTORY RECORD =======
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(paymentAmount)
                .paymentMethod(Payment.PaymentMethod.CASH) // hoặc INSTALLMENT nếu bạn muốn thêm enum
                .paymentPercentage(null)
                .status(Payment.Status.COMPLETED)
                .notes("Thanh toán kỳ trả góp #" + schedule.getInstallmentNumber())
                .paymentDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("Đã tạo Payment record cho kỳ trả góp #{}", schedule.getInstallmentNumber());

        return installmentScheduleRepository.save(schedule);
    }

    /**
     * 4. Lấy danh sách lịch trả góp theo Order
     */
    @Override
    public List<InstallmentSchedule> getSchedulesByOrderId(Integer orderId) {
        return installmentScheduleRepository.findByOrderId(orderId);
    }
}
