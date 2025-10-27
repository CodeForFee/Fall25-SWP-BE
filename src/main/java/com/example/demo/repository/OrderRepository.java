package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByCustomerId(Integer customerId);

    List<Order> findByDealerId(Integer dealerId);

    List<Order> findByUserId(Integer userId);

    List<Order> findByStatus(Order.OrderStatus status);

    Optional<Order> findByQuoteId(Integer quoteId);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT o FROM Order o WHERE o.remainingAmount > 0")
    List<Order> findOrdersWithPendingPayments();

    // Số lượng đơn theo trạng thái toàn hệ thống
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();

    // Tổng doanh thu (tổng totalAmount) toàn hệ thống
    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.orderDate BETWEEN :from AND :to")
    BigDecimal totalSalesBetween(LocalDate from, LocalDate to);

    // Doanh số theo dealer (tổng totalAmount)
    @Query("SELECT o.dealerId, COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.orderDate BETWEEN :from AND :to GROUP BY o.dealerId")
    List<Object[]> salesByDealerBetween(LocalDate from, LocalDate to);

    List<Order> findByDealer_DealerIdAndOrderDateBetween(Integer dealerId, LocalDate start, LocalDate end);

    // Lấy orders cho dealer trong thời gian
    List<Order> findByDealerIdAndOrderDateBetween(Long dealerId, LocalDate from, LocalDate to);
}