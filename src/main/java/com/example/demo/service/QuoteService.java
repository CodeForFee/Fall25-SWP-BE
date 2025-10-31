package com.example.demo.service;

import com.example.demo.dto.QuoteDTO;
import com.example.demo.dto.QuoteResponseDTO;

import java.util.List;

public interface QuoteService {
    List<QuoteResponseDTO> getAllQuotes();
    QuoteResponseDTO getQuoteById(Integer id);
    List<QuoteResponseDTO> getQuotesByCustomerId(Integer customerId);
    List<QuoteResponseDTO> getQuotesByUserId(Integer userId);
    List<QuoteResponseDTO> getQuotesByStatus(String status);
    List<QuoteResponseDTO> getApprovedQuotes();
    QuoteResponseDTO createQuote(QuoteDTO quoteDTO);
    QuoteResponseDTO updateQuote(Integer id, QuoteDTO quoteDTO);
    QuoteResponseDTO approveQuote(Integer id);
    QuoteResponseDTO rejectQuote(Integer id);
    void deleteQuote(Integer id);
    void expireOldQuotes();
}