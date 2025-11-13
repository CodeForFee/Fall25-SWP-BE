package com.example.demo.service.impl;

import com.example.demo.dto.CustomerDebtInfo;
import com.example.demo.dto.DealerDebtInfo;
import com.example.demo.entity.Customer;
import com.example.demo.entity.Dealer;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DealerRepository;
import com.example.demo.service.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtServiceImpl implements DebtService {

    private final CustomerRepository customerRepository;
    private final DealerRepository dealerRepository;

    @Override
    public List<CustomerDebtInfo> getCustomerDebts() {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(c -> CustomerDebtInfo.builder()
                        .customerId(c.getId())
                        .fullName(c.getFullName())
                        .phone(c.getPhone())
                        .email(c.getEmail())
                        .totalSpent(c.getTotalSpent())
                        .totalDebt(c.getTotalDebt())
                        .isVip(c.getIsVip())
                        .dealerName(c.getDealer() != null ? c.getDealer().getName() : "Không xác định")
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DealerDebtInfo> getDealerDebts() {
        List<Dealer> dealers = dealerRepository.findAll();

        return dealers.stream()
                .map(d -> DealerDebtInfo.builder()
                        .dealerId(d.getDealerId())
                        .name(d.getName())
                        .phone(d.getPhone())
                        .region(d.getRegion())
                        .outstandingDebt(d.getOutstandingDebt())
                        .status(d.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }
}
