package com.example.demo.repository;

import com.example.demo.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Integer> {
    Optional<Dealer> findByName(String name);
    boolean existsByName(String name);
    boolean existsByPhone(String phone);

    @Query("SELECT d FROM Dealer d WHERE d.status = :status")
    List<Dealer> findByStatus(@Param("status") Dealer.DealerStatus status);

    @Query("SELECT d FROM Dealer d WHERE d.region = :region")
    List<Dealer> findByRegion(@Param("region") String region);

    @Query("SELECT d FROM Dealer d WHERE d.representativeName LIKE %:representativeName%")
    List<Dealer> findByRepresentativeNameContaining(@Param("representativeName") String representativeName);

    @Query("SELECT d FROM Dealer d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Dealer> findByNameContainingIgnoreCase(@Param("name") String name);
}