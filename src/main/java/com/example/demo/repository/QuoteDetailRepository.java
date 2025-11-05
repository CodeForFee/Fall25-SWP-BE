package com.example.demo.repository;

import com.example.demo.entity.QuoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteDetailRepository extends JpaRepository<QuoteDetail, Integer> {

    List<QuoteDetail> findByQuoteId(Integer quoteId);



    @Query("SELECT qd FROM QuoteDetail qd WHERE qd.quoteId = :quoteId AND qd.id IN (" +
            "SELECT MIN(qd2.id) FROM QuoteDetail qd2 WHERE qd2.quoteId = :quoteId GROUP BY qd2.vehicleId)")
    List<QuoteDetail> findUniqueByQuoteId(@Param("quoteId") Integer quoteId);

    @Query("SELECT qd FROM QuoteDetail qd WHERE qd.quoteId = :quoteId AND qd.id IN (" +
            "SELECT MAX(qd2.id) FROM QuoteDetail qd2 WHERE qd2.quoteId = :quoteId GROUP BY qd2.vehicleId)")
    List<QuoteDetail> findUniqueLatestByQuoteId(@Param("quoteId") Integer quoteId);


    @Query("SELECT qd FROM QuoteDetail qd WHERE qd.quoteId = :quoteId AND qd.vehicleId = :vehicleId")
    Optional<QuoteDetail> findByQuoteIdAndVehicleId(@Param("quoteId") Integer quoteId, @Param("vehicleId") Integer vehicleId);

    @Modifying
    @Query("DELETE FROM QuoteDetail qd WHERE qd.quoteId = :quoteId")
    void deleteByQuoteId(@Param("quoteId") Integer quoteId);

    @Modifying
    @Query("DELETE FROM QuoteDetail qd WHERE qd.quoteId = :quoteId AND qd.vehicleId = :vehicleId")
    int deleteByQuoteIdAndVehicleId(@Param("quoteId") Integer quoteId, @Param("vehicleId") Integer vehicleId);

    @Query("SELECT COUNT(qd) > 0 FROM QuoteDetail qd WHERE qd.quoteId = :quoteId AND qd.vehicleId = :vehicleId")
    boolean existsByQuoteIdAndVehicleId(@Param("quoteId") Integer quoteId, @Param("vehicleId") Integer vehicleId);

    @Query("SELECT qd.vehicleId FROM QuoteDetail qd WHERE qd.quoteId = :quoteId " +
            "GROUP BY qd.vehicleId HAVING COUNT(qd.vehicleId) > 1")
    List<Integer> findDuplicateVehicleIds(@Param("quoteId") Integer quoteId);


    @Modifying
    @Query("DELETE FROM QuoteDetail qd WHERE qd.quoteId = :quoteId AND qd.id NOT IN (" +
            "SELECT MIN(qd2.id) FROM QuoteDetail qd2 WHERE qd2.quoteId = :quoteId GROUP BY qd2.vehicleId)")
    int deleteDuplicateDetails(@Param("quoteId") Integer quoteId);

}