package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    // Lấy danh sách giao dịch theo paymentId
    List<Transaction> findByPaymentIdOrderByInstallmentNumber(Integer paymentId);
}
