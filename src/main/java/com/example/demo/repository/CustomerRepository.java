package com.example.demo.repository;

import com.example.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByCitizenId(String citizenId);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByCitizenId(String citizenId);

    List<Customer> findByDealerId(Integer dealerId);

    @Query("SELECT c FROM Customer c WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.citizenId) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:phone IS NULL OR c.phone = :phone) " +
            "AND (:email IS NULL OR c.email = :email) " +
            "AND (:dealerId IS NULL OR c.dealerId = :dealerId)")
    List<Customer> searchCustomers(
            @Param("keyword") String keyword,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("dealerId") Integer dealerId);
}