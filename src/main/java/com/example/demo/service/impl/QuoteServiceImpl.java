package com.example.demo.service.impl;

import com.example.demo.dto.QuoteDTO;
import com.example.demo.dto.QuoteDetailDTO;
import com.example.demo.dto.QuoteDetailResponseDTO;
import com.example.demo.dto.QuoteResponseDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.QuoteDetailRepository;
import com.example.demo.repository.QuoteRepository;
import com.example.demo.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

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
        if (customerId == null) {
            return quoteRepository.findByCustomerIdIsNull().stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());
        }
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
            log.debug("Creating quote - Customer: {}, Creator Role: {}, Dealer: {}",
                    quoteDTO.getCustomerId(), quoteDTO.getCreatorRole(), quoteDTO.getDealerId());

            validateUniqueVehicleIds(quoteDTO);

            // Validate user và dealer
            User creator = validateUserAndDealer(quoteDTO);

            // Validate customer (cho phép null)
            validateCustomer(quoteDTO);

            // Tạo quote entity
            Quote quote = createQuoteEntity(quoteDTO);

            // Xử lý quote details và tính toán tổng tiền
            processQuoteDetails(quoteDTO, quote);

            // Lưu quote
            Quote savedQuote = quoteRepository.save(quote);

            log.info("Quote created successfully - ID: {}, Creator Role: {}, User ID: {}, Customer ID: {}, Total Amount: {}",
                    savedQuote.getId(), quoteDTO.getCreatorRole(), quoteDTO.getUserId(),
                    quoteDTO.getCustomerId(), quote.getTotalAmount());

            return convertToResponseDTO(savedQuote);

        } catch (Exception e) {
            log.error("Error creating quote: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi server khi tạo báo giá: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuoteResponseDTO updateQuote(Integer id, QuoteDTO quoteDTO) {
        try {
            Quote existingQuote = quoteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá với ID: " + id));

            validateUniqueVehicleIds(quoteDTO);
            validateUserAndDealer(quoteDTO);
            validateCustomer(quoteDTO);

            // Cập nhật thông tin cơ bản
            updateBasicQuoteInfo(existingQuote, quoteDTO);

            // Xử lý quote details
            processQuoteDetailsForUpdate(id, quoteDTO, existingQuote);

            Quote updatedQuote = quoteRepository.save(existingQuote);
            log.debug("Quote updated successfully - ID: {}, Total Amount: {}", id, existingQuote.getTotalAmount());

            return convertToResponseDTO(updatedQuote);

        } catch (Exception e) {
            log.error("Error updating quote ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Lỗi server khi cập nhật báo giá: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteQuote(Integer id) {
        try {
            Quote quote = quoteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá với ID: " + id));

            // Delete quote details first
            quoteDetailRepository.deleteByQuoteId(id);

            // Delete quote
            quoteRepository.delete(quote);
            log.info("Quote deleted successfully - ID: {}", id);

        } catch (Exception e) {
            log.error("Error deleting quote ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Lỗi server khi xóa báo giá: " + e.getMessage());
        }
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

    // ========================= PRIVATE METHODS =========================

    private User validateUserAndDealer(QuoteDTO quoteDTO) {
        User creator = null;
        if (quoteDTO.getUserId() != null) {
            creator = userRepository.findById(quoteDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + quoteDTO.getUserId()));

            if (!creator.getRole().equals(quoteDTO.getCreatorRole())) {
                throw new RuntimeException("User role does not match creator role");
            }

            if (!creator.getDealerId().equals(quoteDTO.getDealerId())) {
                throw new RuntimeException("User does not belong to specified dealer");
            }
        } else {
            if (quoteDTO.getCreatorRole() != User.Role.DEALER_MANAGER) {
                throw new RuntimeException("Only DEALER_MANAGER can create quotes without user_id");
            }
        }
        return creator;
    }

    private void validateCustomer(QuoteDTO quoteDTO) {
        if (quoteDTO.getCustomerId() != null) {
            Customer customer = customerRepository.findById(quoteDTO.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + quoteDTO.getCustomerId()));

            if (!customer.getDealerId().equals(quoteDTO.getDealerId())) {
                throw new RuntimeException("Customer does not belong to this dealer");
            }
        }
        // Cho phép customerId null - không cần validate
    }

    private Quote createQuoteEntity(QuoteDTO quoteDTO) {
        Quote quote = new Quote();
        quote.setCustomerId(quoteDTO.getCustomerId()); // Có thể là null
        quote.setUserId(quoteDTO.getUserId());
        quote.setCreatorRole(quoteDTO.getCreatorRole());
        quote.setDealerId(quoteDTO.getDealerId());
        quote.setCurrentApproverRole(null);
        quote.setCreatedDate(quoteDTO.getCreatedDate() != null ? quoteDTO.getCreatedDate() : LocalDate.now());
        quote.setStatus(Quote.QuoteStatus.DRAFT);
        quote.setApprovalStatus(Quote.QuoteApprovalStatus.DRAFT);
        quote.setValidUntil(quoteDTO.getValidUntil());
        return quote;
    }

    private void processQuoteDetails(QuoteDTO quoteDTO, Quote quote) {
        if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
            List<QuoteDetail> quoteDetails = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (QuoteDetailDTO detailDTO : quoteDTO.getQuoteDetails()) {
                QuoteDetail detail = createQuoteDetail(detailDTO);
                quoteDetails.add(detail);
                totalAmount = totalAmount.add(detail.getTotalAmount());
            }

            quote.setTotalAmount(totalAmount);

            // Lưu quote trước để có ID
            Quote savedQuote = quoteRepository.save(quote);

            // Set quoteId cho các detail và lưu
            for (QuoteDetail detail : quoteDetails) {
                detail.setQuoteId(savedQuote.getId());
            }
            quoteDetailRepository.saveAll(quoteDetails);
        } else {
            quote.setTotalAmount(BigDecimal.ZERO);
        }
    }

    private void processQuoteDetailsForUpdate(Integer quoteId, QuoteDTO quoteDTO, Quote existingQuote) {
        // Xóa details cũ
        quoteDetailRepository.deleteByQuoteId(quoteId);

        if (quoteDTO.getQuoteDetails() != null && !quoteDTO.getQuoteDetails().isEmpty()) {
            List<QuoteDetail> quoteDetails = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (QuoteDetailDTO detailDTO : quoteDTO.getQuoteDetails()) {
                QuoteDetail detail = createQuoteDetail(detailDTO);
                detail.setQuoteId(quoteId);
                quoteDetails.add(detail);
                totalAmount = totalAmount.add(detail.getTotalAmount());
            }

            quoteDetailRepository.saveAll(quoteDetails);
            existingQuote.setTotalAmount(totalAmount);
        } else {
            existingQuote.setTotalAmount(BigDecimal.ZERO);
        }
    }

    private QuoteDetail createQuoteDetail(QuoteDetailDTO detailDTO) {
        QuoteDetail detail = new QuoteDetail();
        detail.setVehicleId(detailDTO.getVehicleId());
        detail.setQuantity(detailDTO.getQuantity());
        detail.setUnitPrice(detailDTO.getUnitPrice());
        detail.setPromotionDiscount(detailDTO.getPromotionDiscount() != null ?
                detailDTO.getPromotionDiscount() : BigDecimal.ZERO);

        // Tính toán tổng tiền
        BigDecimal grossAmount = detailDTO.getUnitPrice()
                .multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
        BigDecimal netAmount = grossAmount;

        if (detail.getPromotionDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountPercent = detail.getPromotionDiscount()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discountAmount = grossAmount.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
            netAmount = grossAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        }

        detail.setTotalAmount(netAmount);
        return detail;
    }

    private void updateBasicQuoteInfo(Quote quote, QuoteDTO quoteDTO) {
        quote.setCustomerId(quoteDTO.getCustomerId());
        quote.setUserId(quoteDTO.getUserId());
        quote.setCreatorRole(quoteDTO.getCreatorRole());
        quote.setDealerId(quoteDTO.getDealerId());
        quote.setValidUntil(quoteDTO.getValidUntil());

        if (quoteDTO.getStatus() != null) {
            quote.setStatus(Quote.QuoteStatus.valueOf(quoteDTO.getStatus().toUpperCase()));
        }
    }

    private void validateUniqueVehicleIds(QuoteDTO quoteDTO) {
        if (quoteDTO.getQuoteDetails() == null || quoteDTO.getQuoteDetails().isEmpty()) {
            return;
        }

        List<Integer> vehicleIds = quoteDTO.getQuoteDetails().stream()
                .map(QuoteDetailDTO::getVehicleId)
                .collect(Collectors.toList());

        boolean hasDuplicates = vehicleIds.size() != vehicleIds.stream().distinct().count();

        if (hasDuplicates) {
            throw new RuntimeException("Duplicate vehicle IDs found in quote details. Each vehicle can only appear once.");
        }
    }

    private QuoteResponseDTO convertToResponseDTO(Quote quote) {
        QuoteResponseDTO dto = new QuoteResponseDTO();
        dto.setId(quote.getId());
        dto.setCustomerId(quote.getCustomerId());
        dto.setUserId(quote.getUserId());
        dto.setCreatedDate(quote.getCreatedDate());
        dto.setTotalAmount(quote.getTotalAmount());
        dto.setStatus(quote.getStatus().name());
        dto.setApprovalStatus(quote.getApprovalStatus().name());
        dto.setValidUntil(quote.getValidUntil());
        dto.setApprovedBy(quote.getApprovedBy());
        dto.setApprovedAt(quote.getApprovedAt());
        dto.setApprovalNotes(quote.getApprovalNotes());

        // Lấy quote details
        List<QuoteDetail> uniqueDetails = quoteDetailRepository.findUniqueByQuoteId(quote.getId());
        List<QuoteDetailResponseDTO> detailDTOs = uniqueDetails.stream()
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