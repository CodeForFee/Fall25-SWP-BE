package com.example.demo.repository;

import com.example.demo.entity.InstallmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallmentScheduleRepository extends JpaRepository<InstallmentSchedule, Integer> {
    List<InstallmentSchedule> findByOrderId(Integer orderId);
}
