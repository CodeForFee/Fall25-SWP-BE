package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.entity.InstallmentSchedule;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.InstallmentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerPortalService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InstallmentScheduleRepository installmentScheduleRepository;

    public CustomerPortalResponse getPortalData(Integer customerId, String citizenId) {

        // 1. Kiểm tra khách hàng hợp lệ
        Customer customer = customerRepository.findByIdAndCitizenId(customerId, citizenId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với thông tin đã cung cấp"));

        // 2. Convert Customer -> DTO
        CustomerDTO customerDTO = toCustomerDto(customer);

        // 3. Lấy toàn bộ đơn hàng
        List<Order> orders = orderRepository.findByCustomerId(customerId);

        // 4. Convert Order -> OrderDTO (CÓ TRẢ GÓP)
        List<OrderDTO> orderDTOs = orders.stream()
                .map(this::toOrderDto)
                .collect(Collectors.toList());

        return new CustomerPortalResponse(customerDTO, orderDTOs);
    }

    // ============================================
    // Convert Customer -> CustomerDTO
    // ============================================
    private CustomerDTO toCustomerDto(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(c.getId());
        dto.setFullName(c.getFullName());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setCitizenId(c.getCitizenId());
        dto.setDealerId(c.getDealerId());
        dto.setIsVip(c.getIsVip());
        dto.setTotalSpent(c.getTotalSpent());
        dto.setTotalDebt(c.getTotalDebt());
        return dto;
    }

    // ============================================
    // Convert Order -> OrderDTO (thêm trả góp)
    // ============================================
    private OrderDTO toOrderDto(Order o) {

        // ====== 1. Lịch sử thanh toán ======
        List<Payment> payments = paymentRepository.findByOrderId(o.getId());

        List<PaymentDTO> paymentDTOs = payments.stream()
                .map(p -> {
                    PaymentDTO dto = new PaymentDTO();
                    dto.setPaymentId(p.getId());
                    dto.setAmount(p.getAmount());
                    dto.setPaymentDate(p.getPaymentDate());
                    dto.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
                    dto.setPaymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null);
                    dto.setPaymentPercentage(p.getPaymentPercentage());
                    dto.setNotes(p.getNotes());
                    dto.setCreatedAt(p.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());


        // ====== 2. Lịch trả góp ======
        List<InstallmentSchedule> schedules =
                installmentScheduleRepository.findByOrderId(o.getId());

        int totalInstallments = schedules.size();
        int paidInstallments = (int) schedules.stream()
                .filter(s -> s.getStatus() == InstallmentSchedule.InstallmentStatus.PAID)
                .count();
        int overdueInstallments = (int) schedules.stream()
                .filter(s -> s.getStatus() == InstallmentSchedule.InstallmentStatus.OVERDUE)
                .count();

        int remainingInstallments = totalInstallments - paidInstallments;

        // Kỳ tiếp theo cần trả
        InstallmentSchedule next = schedules.stream()
                .filter(s -> s.getStatus() == InstallmentSchedule.InstallmentStatus.PENDING)
                .min(Comparator.comparing(InstallmentSchedule::getDueDate))
                .orElse(null);

        // Convert từng kỳ sang DTO
        List<InstallmentScheduleDTO> installmentDTOs = schedules.stream().map(s -> {
            InstallmentScheduleDTO dto = new InstallmentScheduleDTO();
            dto.setInstallmentNumber(s.getInstallmentNumber());
            dto.setAmount(s.getAmount());
            dto.setDueDate(s.getDueDate());
            dto.setStatus(s.getStatus().name());
            dto.setPaidDate(s.getPaidDate());
            dto.setNote(s.getNote());
            return dto;
        }).collect(Collectors.toList());


        // ====== 3. Đổ vào OrderDTO ======
        OrderDTO dto = new OrderDTO();

        dto.setOrderId(o.getId());
        dto.setCustomerId(o.getCustomerId());
        dto.setDealerId(o.getDealerId());
        dto.setOrderDate(o.getOrderDate());

        dto.setStatus(o.getStatus() != null ? o.getStatus().name() : null);
        dto.setPaymentPercentage(o.getPaymentPercentage());
        dto.setPaymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null);
        dto.setPaymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null);




        dto.setPaidAmount(o.getPaidAmount());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setRemainingAmount(o.getRemainingAmount());
        dto.setLastPaymentDate(o.getLastPaymentDate());
        dto.setInstallmentMonths(o.getInstallmentMonths());

        dto.setPayments(paymentDTOs);

        // ====== THÊM TRẢ GÓP ======
        dto.setTotalInstallments(totalInstallments);
        dto.setPaidInstallments(paidInstallments);
        dto.setRemainingInstallments(remainingInstallments);
        dto.setOverdueInstallments(overdueInstallments);
        dto.setNextDueDate(next != null ? next.getDueDate() : null);
        dto.setNextInstallmentNumber(next != null ? next.getInstallmentNumber() : null);

        dto.setInstallmentSchedules(installmentDTOs);

        return dto;
    }
}
