package com.example.demo.service.impl;

import com.example.demo.dto.InstallmentPlanDTO;
import com.example.demo.dto.InstallmentRequest;
import com.example.demo.dto.InstallmentScheduleDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Dealer;
import com.example.demo.entity.InstallmentSchedule;
import com.example.demo.entity.Order;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.repository.InstallmentScheduleRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    private static final BigDecimal VAT_RATE = new BigDecimal("0.10"); // 10%

    //  1. Xem trước kế hoạch trả góp (chưa lưu DB)
    @Override
    public InstallmentPlanDTO previewInstallmentPlan(InstallmentRequest req) {
        BigDecimal basePrice = req.getTotalAmount();
        int months = req.getMonths();
        BigDecimal annualRate = req.getAnnualInterestRate()
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        LocalDate firstDue = req.getFirstDueDate();

        // Tính VAT
        BigDecimal vat = basePrice.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAfterVat = basePrice.add(vat);

        // Lãi suất tháng
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);
        BigDecimal principalPerMonth = totalAfterVat.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);

        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal remainingPrincipal = totalAfterVat;

        List<InstallmentScheduleDTO> schedule = new ArrayList<>();

        for (int i = 1; i <= months; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPayment = principalPerMonth.add(interest);
            totalInterest = totalInterest.add(interest);

            schedule.add(InstallmentScheduleDTO.builder()
                    .transactionId(null)
                    .installmentNumber(i)
                    .amount(totalPayment)
                    .dueDate(firstDue.plusMonths(i - 1))
                    .status("PENDING")
                    .build());

            remainingPrincipal = remainingPrincipal.subtract(principalPerMonth);
        }

        BigDecimal totalPayable = totalAfterVat.add(totalInterest);
        BigDecimal monthlyPayment = totalPayable.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);

        return InstallmentPlanDTO.builder()
                .totalAmount(basePrice)
                .vatAmount(vat)
                .interestAmount(totalInterest)
                .totalPayable(totalPayable)
                .monthlyPayment(monthlyPayment)
                .months(months)
                .firstDueDate(firstDue)
                .schedule(schedule)
                .build();
    }

    //  2. Sinh lịch trả góp thật trong DB khi Order được tạo
    @Override
    @Transactional
    public List<InstallmentSchedule> generateSchedule(Integer orderId, InstallmentRequest req) {
        InstallmentPlanDTO plan = previewInstallmentPlan(req);

        List<InstallmentSchedule> entities = new ArrayList<>();
        for (InstallmentScheduleDTO dto : plan.getSchedule()) {
            InstallmentSchedule entity = InstallmentSchedule.builder()
                    .orderId(orderId)
                    .installmentNumber(dto.getInstallmentNumber())
                    .amount(dto.getAmount())
                    .dueDate(dto.getDueDate())
                    .status(InstallmentSchedule.InstallmentStatus.PENDING)
                    .build();
            entities.add(entity);
        }

        List<InstallmentSchedule> saved = installmentScheduleRepository.saveAll(entities);
        log.info(" Đã tạo {} kỳ trả góp cho đơn hàng ID: {}", saved.size(), orderId);
        return saved;
    }

    //  3. Đánh dấu kỳ trả góp đã thanh toán + Cập nhật công nợ tự động
    @Override
    @Transactional
    public InstallmentSchedule markInstallmentPaid(Integer scheduleId) {
        InstallmentSchedule schedule = installmentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kỳ trả góp với ID: " + scheduleId));

        // Cập nhật trạng thái kỳ trả góp
        schedule.setStatus(InstallmentSchedule.InstallmentStatus.PAID);
        schedule.setPaidDate(LocalDate.now());
        schedule.setNote("Thanh toán thành công vào " + LocalDate.now());

        // Lấy thông tin đơn hàng liên quan
        Order order = orderRepository.findById(schedule.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng liên quan."));

        BigDecimal paymentAmount = schedule.getAmount();

        //  Cập nhật công nợ khách hàng
        if (order.getCustomerId() != null) {
            customerRepository.findById(order.getCustomerId()).ifPresent(customer -> {
                customer.reduceDebt(paymentAmount);
                customerRepository.save(customer);
                log.info(" Cập nhật công nợ khách hàng [{}]: -{} VNĐ → Còn lại: {} VNĐ",
                        customer.getFullName(), paymentAmount, customer.getTotalDebt());
            });
        }

        //  Cập nhật công nợ đại lý
        if (order.getDealerId() != null) {
            dealerRepository.findById(order.getDealerId()).ifPresent(dealer -> {
                if (dealer.getOutstandingDebt() == null) {
                    dealer.setOutstandingDebt(BigDecimal.ZERO);
                }

                BigDecimal newDebt = dealer.getOutstandingDebt().subtract(paymentAmount);
                if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
                    newDebt = BigDecimal.ZERO;
                }

                dealer.setOutstandingDebt(newDebt);
                dealerRepository.save(dealer);

                log.info(" Cập nhật công nợ đại lý [{}]: -{} VNĐ → Còn lại: {} VNĐ",
                        dealer.getName(), paymentAmount, newDebt);
            });
        }

        //  Lưu kỳ trả góp
        InstallmentSchedule updatedSchedule = installmentScheduleRepository.save(schedule);
        log.info("Kỳ trả góp #{} đã thanh toán thành công (Order ID: {}).",
                updatedSchedule.getInstallmentNumber(), schedule.getOrderId());

        return updatedSchedule;
    }

    //  4. Lấy danh sách các kỳ trả góp theo Order
    @Override
    public List<InstallmentSchedule> getSchedulesByOrderId(Integer orderId) {
        return installmentScheduleRepository.findByOrderId(orderId);
    }
}
