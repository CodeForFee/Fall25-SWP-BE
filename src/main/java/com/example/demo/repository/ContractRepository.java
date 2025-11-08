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

    List<Contract> findByCustomerId(Integer customerId);
    List<Contract> findByDealerId(Integer dealerId);
    List<Contract> findByOrderId(Integer orderId);
    @Query("SELECT c FROM Contract c JOIN c.customer cust WHERE cust.fullName LIKE %:customerName%")
    List<Contract> findByCustomerNameContaining(@Param("customerName") String customerName);
    @Query("SELECT c FROM Contract c JOIN c.customer cust WHERE c.dealerId = :dealerId AND cust.fullName LIKE %:customerName%")
    List<Contract> findByDealerIdAndCustomerNameContaining(@Param("dealerId") Integer dealerId,
                                                           @Param("customerName") String customerName);
    boolean existsByIdAndDealerId(Integer id, Integer dealerId);
}