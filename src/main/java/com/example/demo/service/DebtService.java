package com.example.demo.service;

import java.math.BigDecimal;

public interface DebtService {

    // Cập nhật nợ của khách hàng
    void addCustomerDebt(Integer customerId, BigDecimal amount);

    // Khách hàng thanh toán nợ
    void reduceCustomerDebt(Integer customerId, BigDecimal paymentAmount);

    // Cập nhật nợ của đại lý
    void addDealerDebt(Integer dealerId, BigDecimal amount);

    // Đại lý thanh toán nợ
    void reduceDealerDebt(Integer dealerId, BigDecimal paymentAmount);
}
