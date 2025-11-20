package com.example.demo.service;

import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.CustomerPortalResponse;
import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.PaymentDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerPortalService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public CustomerPortalResponse getPortalData(Integer customerId, String citizenId) {

        // Kiểm tra khách hàng hợp lệ
        Customer customer = customerRepository.findByIdAndCitizenId(customerId, citizenId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với thông tin đã cung cấp"));

        // Convert Customer -> DTO
        CustomerDTO customerDTO = toCustomerDto(customer);

        // Lấy toàn bộ đơn hàng của khách
        List<Order> orders = orderRepository.findByCustomerId(customerId);

        // Convert Order -> OrderDTO
        List<OrderDTO> orderDTOs = orders.stream()
                .map(this::toOrderDto)
                .collect(Collectors.toList());

        // Trả về kết quả
        return new CustomerPortalResponse(customerDTO, orderDTOs);
    }

    // ============================
    // Convert Customer -> CustomerDTO
    // ============================
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

    // ============================
    // Convert Order -> OrderDTO
    // ============================
    private OrderDTO toOrderDto(Order o) {

        // Lấy danh sách payment của order
        List<Payment> payments = paymentRepository.findByOrderId(o.getId());

        // Convert Payment -> PaymentDTO
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

        // Convert Order -> OrderDTO
        OrderDTO dto = new OrderDTO();

        
        dto.setCustomerId(o.getCustomerId());
        dto.setDealerId(o.getDealerId());
        dto.setOrderDate(o.getOrderDate());
        dto.setPaymentPercentage(o.getPaymentPercentage());
        dto.setPaymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null);
        dto.setPaymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null);
        dto.setPaidAmount(o.getPaidAmount());


        // ====== FIELD MỚI ======
        dto.setOrderId(o.getId());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setRemainingAmount(o.getRemainingAmount());
        dto.setLastPaymentDate(o.getLastPaymentDate());
        dto.setInstallmentMonths(o.getInstallmentMonths());
        dto.setPayments(paymentDTOs);

        return dto;
    }
}
