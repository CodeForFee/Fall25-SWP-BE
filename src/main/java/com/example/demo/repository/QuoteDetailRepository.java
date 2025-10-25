package com.example.demo.repository;

import com.example.demo.entity.QuoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteDetailRepository extends JpaRepository<QuoteDetail, Integer> {

    List<QuoteDetail> findByQuoteId(Integer quoteId);

    void deleteByQuoteId(Integer quoteId);
}