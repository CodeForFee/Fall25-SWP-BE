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
import java.math.RoundingMode;
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
            log.debug("Creating quote for customer: {}", quoteDTO.getCustomerId());

            Quote quote = new Quote();
            quote.setCustomerId(quoteDTO.getCustomerId());
            quote.setUserId(quoteDTO.getUserId());
            quote.setCreatedDate(quoteDTO.getCreatedDate() != null ? quoteDTO.getCreatedDate() : LocalDate.now());
            quote.setStatus(Quote.QuoteStatus.valueOf(quoteDTO.getStatus().toUpperCase()));
            quote.setApprovalStatus(Quote.QuoteApprovalStatus.DRAFT);

            quote.setValidUntil(quoteDTO.getValidUntil());

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalDiscount = BigDecimal.ZERO;

            if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
                List<QuoteDetail> quoteDetails = new ArrayList<>();

                for (QuoteDetailDTO detailDTO : quoteDTO.getQuoteDetails()) {
                    QuoteDetail detail = new QuoteDetail();
                    detail.setVehicleId(detailDTO.getVehicleId());
                    detail.setQuantity(detailDTO.getQuantity());
                    detail.setUnitPrice(detailDTO.getUnitPrice());
                    detail.setPromotionDiscount(detailDTO.getPromotionDiscount() != null ?
                            detailDTO.getPromotionDiscount() : BigDecimal.ZERO);


                    BigDecimal grossAmount = detailDTO.getUnitPrice()
                            .multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
                    BigDecimal discountAmount = BigDecimal.ZERO;
                    BigDecimal netAmount = grossAmount;

                    if (detail.getPromotionDiscount().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discountPercent = detail.getPromotionDiscount()
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        discountAmount = grossAmount.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
                        netAmount = grossAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
                    }

                    detail.setTotalAmount(netAmount);
                    totalAmount = totalAmount.add(netAmount);
                    totalDiscount = totalDiscount.add(discountAmount);
                    quoteDetails.add(detail);
                }

                quote.setTotalAmount(totalAmount);
                Quote savedQuote = quoteRepository.save(quote);

                for (QuoteDetail detail : quoteDetails) {
                    detail.setQuoteId(savedQuote.getId());
                }
                quoteDetailRepository.saveAll(quoteDetails);

                log.info("Quote created successfully - Total Amount: {}, Total Discount: {}", totalAmount, totalDiscount);
                return convertToResponseDTO(savedQuote);
            } else {
                quote.setTotalAmount(BigDecimal.ZERO);
                Quote savedQuote = quoteRepository.save(quote);
                log.info("Quote created successfully - No details");
                return convertToResponseDTO(savedQuote);
            }

        } catch (Exception e) {
            log.error("Error creating quote: {}", e.getMessage(), e);
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
                BigDecimal grossAmount = detailDTO.getUnitPrice()
                        .multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
                BigDecimal discountAmount = BigDecimal.ZERO;
                BigDecimal netAmount = grossAmount;

                if (detail.getPromotionDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountPercent = detail.getPromotionDiscount()
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    discountAmount = grossAmount.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
                    netAmount = grossAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
                }

                detail.setTotalAmount(netAmount);
                totalAmount = totalAmount.add(netAmount);

                quoteDetails.add(detail);
            }

            quoteDetailRepository.saveAll(quoteDetails);
            existingQuote.setTotalAmount(totalAmount);
        } else {
            existingQuote.setTotalAmount(BigDecimal.ZERO);
        }

        Quote updatedQuote = quoteRepository.save(existingQuote);
        log.debug("Quote updated successfully - Total Amount: {}", totalAmount);
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
        log.debug("Quote deleted successfully");
    }

    @Override
    public void expireOldQuotes() {
        LocalDate today = LocalDate.now();
        List<Quote> expiredQuotes = quoteRepository.findExpiredQuotes(today);

        for (Quote quote : expiredQuotes) {
            quote.setStatus(Quote.QuoteStatus.EXPIRED);
        }

        quoteRepository.saveAll(expiredQuotes);
        log.info("Expired {} old quotes", expiredQuotes.size());
    }

    private QuoteResponseDTO convertToResponseDTO(Quote quote) {
        QuoteResponseDTO dto = new QuoteResponseDTO();
        dto.setId(quote.getId());
        dto.setCustomerId(quote.getCustomerId());
        dto.setUserId(quote.getUserId());
        dto.setCreatedDate(quote.getCreatedDate());
        dto.setTotalAmount(quote.getTotalAmount());
        dto.setStatus(quote.getStatus().name());
        dto.setApprovalStatus(quote.getApprovalStatus().name()); // 🔥 THÊM APPROVAL STATUS
        dto.setValidUntil(quote.getValidUntil());
        if (quote.getApprovedBy() != null) {
            dto.setApprovedBy(quote.getApprovedBy());
        }
        if (quote.getApprovedAt() != null) {
            dto.setApprovedAt(quote.getApprovedAt());
        }
        if (quote.getApprovalNotes() != null) {
            dto.setApprovalNotes(quote.getApprovalNotes());
        }

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