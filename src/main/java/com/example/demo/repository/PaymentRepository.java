package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);

    List<Payment> findByOrderId(Integer orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByStatus(Payment.Status status);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.paymentMethod = :paymentMethod")
    List<Payment> findByStatusAndPaymentMethod(
            @Param("status") Payment.Status status,
            @Param("paymentMethod") Payment.PaymentMethod paymentMethod
    );

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.orderId = :orderId AND p.status = 'COMPLETED'")
    Long countSuccessfulPaymentsByOrderId(@Param("orderId") Integer orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.orderId = :orderId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaidAmountByOrderId(@Param("orderId") Integer orderId);
}