package com.example.demo.service;

import com.example.demo.dto.QuoteDTO;
import com.example.demo.dto.QuoteResponseDTO;

import java.util.List;

public interface QuoteService {
    List<QuoteResponseDTO> getAllQuotes();
    QuoteResponseDTO getQuoteById(Integer id);
    List<QuoteResponseDTO> getQuotesByCustomerId(Integer customerId);
    List<QuoteResponseDTO> getQuotesByUserId(Integer userId);
    QuoteResponseDTO createQuote(QuoteDTO quoteDTO);
    QuoteResponseDTO updateQuote(Integer id, QuoteDTO quoteDTO);
    void deleteQuote(Integer id);
    void expireOldQuotes();
}