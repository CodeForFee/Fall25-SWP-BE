package com.example.demo.repository;

import com.example.demo.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {

    Optional<Contract> findByContractNumber(String contractNumber);

    List<Contract> findByOrderId(Integer orderId);

    List<Contract> findByVin(String vin);

    @Query("SELECT c FROM Contract c WHERE c.signedDate BETWEEN :startDate AND :endDate")
    List<Contract> findContractsBySignedDateRange(@Param("startDate") java.time.LocalDate startDate,
                                                  @Param("endDate") java.time.LocalDate endDate);

    boolean existsByContractNumber(String contractNumber);
}