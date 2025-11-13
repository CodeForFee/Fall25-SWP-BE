package com.example.demo.service;

import com.example.demo.dto.CustomerDebtInfo;
import com.example.demo.dto.DealerDebtInfo;

import java.util.List;

public interface DebtService {
    List<CustomerDebtInfo> getCustomerDebts();
    List<DealerDebtInfo> getDealerDebts();
}
