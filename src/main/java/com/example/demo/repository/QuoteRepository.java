package com.example.demo.repository;

import com.example.demo.entity.Quote;
import com.example.demo.entity.QuoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Integer> {

    List<Quote> findByCustomerId(Integer customerId);

    List<Quote> findByUserId(Integer userId);

    List<Quote> findByStatus(QuoteDetail.QuoteStatus status);

    @Query("SELECT q FROM Quote q WHERE q.validUntil < :currentDate AND q.status = 'SENT'")
    List<Quote> findExpiredQuotes(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT q FROM Quote q WHERE q.customerId = :customerId AND q.status = 'ACCEPTED'")
    List<Quote> findAcceptedQuotesByCustomer(@Param("customerId") Integer customerId);
}