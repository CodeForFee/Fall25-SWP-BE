package com.example.demo.repository;

import com.example.demo.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrderId(Integer orderId);

    void deleteByOrderId(Integer orderId);

    @Query("SELECT od.vehicleId, SUM(od.quantity) FROM OrderDetail od WHERE od.orderId IN (SELECT o.id FROM Order o WHERE o.orderDate BETWEEN :from AND :to) GROUP BY od.vehicleId")
    List<Object[]> totalSoldByVehicleBetween(LocalDate from, LocalDate to);
}