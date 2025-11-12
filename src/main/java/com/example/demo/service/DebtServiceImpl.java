package com.example.demo.service.impl;

import com.example.demo.entity.Customer;
import com.example.demo.entity.Dealer;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.service.DebtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class DebtServiceImpl implements DebtService {

    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;

    public DebtServiceImpl(CustomerRepository customerRepository, DealerRepository dealerRepository) {
        this.customerRepository = customerRepository;
        this.dealerRepository = dealerRepository;
    }

    // ====================== CUSTOMER ======================

    @Override
    public void addCustomerDebt(Integer customerId, BigDecimal amount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        customer.addDebt(amount);
        customerRepository.save(customer);
    }

    @Override
    public void reduceCustomerDebt(Integer customerId, BigDecimal paymentAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        customer.reduceDebt(paymentAmount);
        customerRepository.save(customer);
    }

    // ====================== DEALER ======================

    @Override
    public void addDealerDebt(Integer dealerId, BigDecimal amount) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with ID: " + dealerId));

        dealer.setOutstandingDebt(dealer.getOutstandingDebt().add(amount));
        dealerRepository.save(dealer);
    }

    @Override
    public void reduceDealerDebt(Integer dealerId, BigDecimal paymentAmount) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with ID: " + dealerId));

        BigDecimal newDebt = dealer.getOutstandingDebt().subtract(paymentAmount);
        if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
            newDebt = BigDecimal.ZERO;
        }

        dealer.setOutstandingDebt(newDebt);
        dealerRepository.save(dealer);
    }
}
