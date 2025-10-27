package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.order = :order AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedByOrder(@Param("order") Order order);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p JOIN p.order o WHERE o.dealer.dealerId = :dealerId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedByDealer(@Param("dealerId") Integer dealerId);
}
