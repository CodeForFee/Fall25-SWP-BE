package com.example.demo.service.impl;

import com.example.demo.dto.QuoteDTO;
import com.example.demo.dto.QuoteDetailDTO;
import com.example.demo.dto.QuoteDetailResponseDTO;
import com.example.demo.dto.QuoteResponseDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.QuoteDetailRepository;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteDetailRepository quoteDetailRepository;

    @Override
    public List<QuoteResponseDTO> getAllQuotes() {
        return quoteRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public QuoteResponseDTO getQuoteById(Integer id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá với ID: " + id));
        return convertToResponseDTO(quote);
    }

    @Override
    public List<QuoteResponseDTO> getQuotesByCustomerId(Integer customerId) {
        return quoteRepository.findByCustomerId(customerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuoteResponseDTO> getQuotesByUserId(Integer userId) {
        return quoteRepository.findByUserId(userId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuoteResponseDTO createQuote(QuoteDTO quoteDTO) {
        try {
            log.info("=== START CREATE QUOTE ===");

            Quote quote = new Quote();
            quote.setCustomerId(quoteDTO.getCustomerId());
            quote.setUserId(quoteDTO.getUserId());
            quote.setCreatedDate(quoteDTO.getCreatedDate() != null ? quoteDTO.getCreatedDate() : LocalDate.now());
            quote.setTotalAmount(quoteDTO.getTotalAmount());
            quote.setStatus(Quote.QuoteStatus.valueOf(quoteDTO.getStatus().toUpperCase()));
            quote.setValidUntil(quoteDTO.getValidUntil());

            Quote savedQuote = quoteRepository.save(quote);

            // Save quote details
            if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
                List<QuoteDetail> quoteDetails = quoteDTO.getQuoteDetails().stream()
                        .map(detailDTO -> convertToQuoteDetail(detailDTO, savedQuote.getId()))
                        .collect(Collectors.toList());
                quoteDetailRepository.saveAll(quoteDetails);
            }

            log.info("=== QUOTE CREATED SUCCESSFULLY ===");
            return convertToResponseDTO(savedQuote);

        } catch (Exception e) {
            log.error("!!! ERROR IN CREATE QUOTE !!!", e);
            throw new RuntimeException("Lỗi server khi tạo báo giá: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuoteResponseDTO updateQuote(Integer id, QuoteDTO quoteDTO) {
        Quote existingQuote = quoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá với ID: " + id));

        existingQuote.setCustomerId(quoteDTO.getCustomerId());
        existingQuote.setUserId(quoteDTO.getUserId());
        existingQuote.setTotalAmount(quoteDTO.getTotalAmount());
        existingQuote.setStatus(Quote.QuoteStatus.valueOf(quoteDTO.getStatus().toUpperCase()));
        existingQuote.setValidUntil(quoteDTO.getValidUntil());

        // Update quote details
        if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
            // Delete existing details
            quoteDetailRepository.deleteByQuoteId(id);

            // Save new details
            List<QuoteDetail> quoteDetails = quoteDTO.getQuoteDetails().stream()
                    .map(detailDTO -> convertToQuoteDetail(detailDTO, id))
                    .collect(Collectors.toList());
            quoteDetailRepository.saveAll(quoteDetails);
        }

        Quote updatedQuote = quoteRepository.save(existingQuote);
        log.info("=== QUOTE UPDATED SUCCESSFULLY ===");
        return convertToResponseDTO(updatedQuote);
    }


    @Override
    @Transactional
    public void deleteQuote(Integer id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá với ID: " + id));

        // Delete quote details first
        quoteDetailRepository.deleteByQuoteId(id);

        // Delete quote
        quoteRepository.delete(quote);
        log.info("=== QUOTE DELETED SUCCESSFULLY ===");
    }

    @Override
    public void expireOldQuotes() {
        LocalDate today = LocalDate.now();
        List<Quote> expiredQuotes = quoteRepository.findExpiredQuotes(today);

        for (Quote quote : expiredQuotes) {
            quote.setStatus(Quote.QuoteStatus.EXPIRED);
        }

        quoteRepository.saveAll(expiredQuotes);
        log.info("=== EXPIRED {} OLD QUOTES ===", expiredQuotes.size());
    }

    private QuoteDetail convertToQuoteDetail(QuoteDetailDTO dto, Integer quoteId) {
        QuoteDetail detail = new QuoteDetail();
        detail.setQuoteId(quoteId);
        detail.setVehicleId(dto.getVehicleId());
        detail.setQuantity(dto.getQuantity());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setPromotionDiscount(dto.getPromotionDiscount());
        detail.setTotalAmount(dto.getTotalAmount());
        return detail;
    }

    private QuoteResponseDTO convertToResponseDTO(Quote quote) {
        QuoteResponseDTO dto = new QuoteResponseDTO();
        dto.setId(quote.getId());
        dto.setCustomerId(quote.getCustomerId());
        dto.setUserId(quote.getUserId());
        dto.setCreatedDate(quote.getCreatedDate());
        dto.setTotalAmount(quote.getTotalAmount());
        dto.setStatus(quote.getStatus().name());
        dto.setValidUntil(quote.getValidUntil());

        // Load quote details
        List<QuoteDetail> details = quoteDetailRepository.findByQuoteId(quote.getId());
        List<QuoteDetailResponseDTO> detailDTOs = details.stream()
                .map(this::convertToDetailResponseDTO)
                .collect(Collectors.toList());
        dto.setQuoteDetails(detailDTOs);

        return dto;
    }

    private QuoteDetailResponseDTO convertToDetailResponseDTO(QuoteDetail detail) {
        QuoteDetailResponseDTO dto = new QuoteDetailResponseDTO();
        dto.setId(detail.getId());
        dto.setQuoteId(detail.getQuoteId());
        dto.setVehicleId(detail.getVehicleId());
        dto.setQuantity(detail.getQuantity());
        dto.setUnitPrice(detail.getUnitPrice());
        dto.setPromotionDiscount(detail.getPromotionDiscount());
        dto.setTotalAmount(detail.getTotalAmount());
        return dto;
    }
}