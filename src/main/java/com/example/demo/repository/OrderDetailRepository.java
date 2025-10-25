package com.example.demo.repository;

import com.example.demo.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrderId(Integer orderId);

    void deleteByOrderId(Integer orderId);
}