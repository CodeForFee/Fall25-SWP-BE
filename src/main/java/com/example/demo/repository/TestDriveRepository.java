package com.example.demo.repository;

import com.example.demo.entity.TestDriveRequest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TestDriveRepository extends JpaRepository<TestDriveRequest, Long> {
    @Query("SELECT t FROM TestDriveRequest t WHERE t.dealer.id = :dealerId")
    List<TestDriveRequest> findByDealerId(Integer dealerId);
}