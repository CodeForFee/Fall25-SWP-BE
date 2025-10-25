package com.example.demo.repository;

import com.example.demo.entity.Promotion;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    List<Promotion> findByProgramNameContainingIgnoreCase(String programName);

    List<Promotion> findByStatus(Promotion.PromotionStatus status);

    // Tìm promotions theo user
    List<Promotion> findByCreatedBy(User user);

    @Query("SELECT p FROM Promotion p WHERE p.startDate <= :date AND p.endDate >= :date AND p.status = 'ACTIVE'")
    List<Promotion> findActivePromotions(@Param("date") LocalDate date);

    @Query("SELECT p FROM Promotion p WHERE p.endDate < :currentDate AND p.status = 'ACTIVE'")
    List<Promotion> findExpiredPromotions(@Param("currentDate") LocalDate currentDate);

    boolean existsByProgramName(String programName);

    @Query("SELECT p FROM Promotion p WHERE p.programName = :programName AND p.id != :id")
    List<Promotion> findByProgramNameAndIdNot(@Param("programName") String programName, @Param("id") Integer id);

    // Tìm promotions theo dealer thông qua user
    @Query("SELECT p FROM Promotion p WHERE p.createdBy.dealerId = :dealerId")
    List<Promotion> findByDealerId(@Param("dealerId") Integer dealerId);
}