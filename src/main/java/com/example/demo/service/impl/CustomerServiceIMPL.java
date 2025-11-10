package com.example.demo.service.impl;

import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.CustomerResponseDTO;
import com.example.demo.entity.Customer;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceIMPL implements CustomerService {

    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;

    @Override
    public List<CustomerResponseDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponseDTO getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        return convertToResponseDTO(customer);
    }

    @Override
    public CustomerResponseDTO createCustomer(CustomerDTO customerDTO) {
        try {
            log.debug("Creating customer: {}", customerDTO.getEmail());


            if (customerRepository.existsByEmail(customerDTO.getEmail())) {
                throw new RuntimeException("Email đã tồn tại");
            }

            if (customerRepository.existsByPhone(customerDTO.getPhone())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }

            if (customerDTO.getCitizenId() != null && customerRepository.existsByCitizenId(customerDTO.getCitizenId())) {
                throw new RuntimeException("Số CCCD đã tồn tại");
            }

            // Validate dealer exists
            if (!dealerRepository.existsById(customerDTO.getDealerId())) {
                throw new RuntimeException("Đại lý không tồn tại");
            }

            Customer customer = new Customer();
            customer.setFullName(customerDTO.getFullName());
            customer.setPhone(customerDTO.getPhone());
            customer.setEmail(customerDTO.getEmail());
            customer.setCitizenId(customerDTO.getCitizenId());
            customer.setDealerId(customerDTO.getDealerId());

            Customer savedCustomer = customerRepository.save(customer);
            log.debug("Customer created successfully");
            return convertToResponseDTO(savedCustomer);

        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi server: " + e.getMessage());
        }
    }

    @Override
    public CustomerResponseDTO updateCustomer(Integer id, CustomerDTO customerDTO) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // Check email uniqueness if changed
        if (customerDTO.getEmail() != null && !customerDTO.getEmail().equals(existingCustomer.getEmail())) {
            if (customerRepository.existsByEmail(customerDTO.getEmail())) {
                throw new RuntimeException("Email đã tồn tại");
            }
            existingCustomer.setEmail(customerDTO.getEmail());
        }

        // Check phone uniqueness if changed
        if (customerDTO.getPhone() != null && !customerDTO.getPhone().equals(existingCustomer.getPhone())) {
            if (customerRepository.existsByPhone(customerDTO.getPhone())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
            existingCustomer.setPhone(customerDTO.getPhone());
        }

        // Check citizenId uniqueness if changed
        if (customerDTO.getCitizenId() != null && !customerDTO.getCitizenId().equals(existingCustomer.getCitizenId())) {
            if (customerRepository.existsByCitizenId(customerDTO.getCitizenId())) {
                throw new RuntimeException("Số CCCD đã tồn tại");
            }
            existingCustomer.setCitizenId(customerDTO.getCitizenId());
        }

        // Update other fields
        if (customerDTO.getFullName() != null) {
            existingCustomer.setFullName(customerDTO.getFullName());
        }
        if (customerDTO.getDealerId() != null) {
            existingCustomer.setDealerId(customerDTO.getDealerId());
        }

        Customer updatedCustomer = customerRepository.save(existingCustomer);
        return convertToResponseDTO(updatedCustomer);
    }

    @Override
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        customerRepository.delete(customer);
    }

    @Override
    public List<CustomerResponseDTO> getCustomersByDealer(Integer dealerId) {
        return customerRepository.findByDealerId(dealerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    private CustomerResponseDTO convertToResponseDTO(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setPhone(customer.getPhone());
        dto.setEmail(customer.getEmail());
        dto.setCitizenId(customer.getCitizenId());
        dto.setDealerId(customer.getDealerId());
        dto.setTotalSpent(customer.getTotalSpent());
        dto.setTotalDebt(customer.getTotalDebt());
        dto.setIsVip(customer.getIsVip());
        if (customer.getDealer() != null) {
            dto.setDealerName(customer.getDealer().getName());
        }

        return dto;
    }
}