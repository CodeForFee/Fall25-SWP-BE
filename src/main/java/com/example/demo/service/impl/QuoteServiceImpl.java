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
import java.util.ArrayList;
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
            quote.setStatus(Quote.QuoteStatus.valueOf(quoteDTO.getStatus().toUpperCase()));
            quote.setValidUntil(quoteDTO.getValidUntil());

            // TÍNH TOÁN
            BigDecimal totalAmount = BigDecimal.ZERO;

            if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
                List<QuoteDetail> quoteDetails = new ArrayList<>();

                for (QuoteDetailDTO detailDTO : quoteDTO.getQuoteDetails()) {
                    QuoteDetail detail = new QuoteDetail();
                    detail.setVehicleId(detailDTO.getVehicleId());
                    detail.setQuantity(detailDTO.getQuantity());
                    detail.setUnitPrice(detailDTO.getUnitPrice());
                    detail.setPromotionDiscount(detailDTO.getPromotionDiscount() != null ?
                            detailDTO.getPromotionDiscount() : BigDecimal.ZERO);

                    // TÍNH TOÁN
                    BigDecimal itemTotal = detailDTO.getUnitPrice()
                            .multiply(BigDecimal.valueOf(detailDTO.getQuantity()))
                            .subtract(detail.getPromotionDiscount());

                    detail.setTotalAmount(itemTotal);
                    totalAmount = totalAmount.add(itemTotal);

                    quoteDetails.add(detail);
                }

                quote.setTotalAmount(totalAmount);
                Quote savedQuote = quoteRepository.save(quote);

                // Set quoteId cho các detail và save
                for (QuoteDetail detail : quoteDetails) {
                    detail.setQuoteId(savedQuote.getId());
                }
                quoteDetailRepository.saveAll(quoteDetails);

                log.info("=== QUOTE CREATED SUCCESSFULLY - Total Amount: {} ===", totalAmount);
                return convertToResponseDTO(savedQuote);
            } else {
                quote.setTotalAmount(BigDecimal.ZERO);
                Quote savedQuote = quoteRepository.save(quote);
                log.info("=== QUOTE CREATED SUCCESSFULLY - No details ===");
                return convertToResponseDTO(savedQuote);
            }

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
        existingQuote.setStatus(Quote.QuoteStatus.valueOf(quoteDTO.getStatus().toUpperCase()));
        existingQuote.setValidUntil(quoteDTO.getValidUntil());

        // TÍNH TOÁN LẠI
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Update quote details
        if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
            // Delete existing details
            quoteDetailRepository.deleteByQuoteId(id);

            // Save new details và tính toán totalAmount
            List<QuoteDetail> quoteDetails = new ArrayList<>();
            for (QuoteDetailDTO detailDTO : quoteDTO.getQuoteDetails()) {
                QuoteDetail detail = new QuoteDetail();
                detail.setQuoteId(id);
                detail.setVehicleId(detailDTO.getVehicleId());
                detail.setQuantity(detailDTO.getQuantity());
                detail.setUnitPrice(detailDTO.getUnitPrice());
                detail.setPromotionDiscount(detailDTO.getPromotionDiscount() != null ?
                        detailDTO.getPromotionDiscount() : BigDecimal.ZERO);

                // TÍNH TOÁN
                BigDecimal itemTotal = detailDTO.getUnitPrice()
                        .multiply(BigDecimal.valueOf(detailDTO.getQuantity()))
                        .subtract(detail.getPromotionDiscount());

                detail.setTotalAmount(itemTotal);
                totalAmount = totalAmount.add(itemTotal);

                quoteDetails.add(detail);
            }

            quoteDetailRepository.saveAll(quoteDetails);
            existingQuote.setTotalAmount(totalAmount);
        } else {
            existingQuote.setTotalAmount(BigDecimal.ZERO);
        }

        Quote updatedQuote = quoteRepository.save(existingQuote);
        log.info("=== QUOTE UPDATED SUCCESSFULLY - Total Amount: {} ===", totalAmount);
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
        dto.setTotalAmount(detail.getTotalAmount()); // Đã được tính tự động
        return dto;
    }
}