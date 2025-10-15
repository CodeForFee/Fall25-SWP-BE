package com.example.demo.service.impl;

import com.example.demo.dto.PromotionDTO;
import com.example.demo.dto.PromotionResponseDTO;
import com.example.demo.entity.Promotion;
import com.example.demo.entity.PromotionStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.PromotionRepository;
import com.example.demo.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceIMPL implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDTO getPromotionById(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại với ID: " + id));
        return convertToResponseDTO(promotion);
    }

    @Override
    @Transactional
    public PromotionResponseDTO createPromotion(PromotionDTO promotionDTO) {
        try {
            log.info("Creating new promotion: {}", promotionDTO.getProgramName());

            // Check if program name already exists
            if (promotionRepository.existsByProgramName(promotionDTO.getProgramName())) {
                throw new RuntimeException("Tên chương trình khuyến mãi đã tồn tại");
            }

            // Validate dates
            if (promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
            }

            // Get user who creates the promotion
            User creator = userRepository.findById(promotionDTO.getCreatedBy())
                    .orElseThrow(() -> new RuntimeException("Người tạo không tồn tại"));

            Promotion promotion = new Promotion();
            promotion.setProgramName(promotionDTO.getProgramName());
            promotion.setDescription(promotionDTO.getDescription());
            promotion.setStartDate(promotionDTO.getStartDate());
            promotion.setEndDate(promotionDTO.getEndDate());
            promotion.setConditions(promotionDTO.getConditions());
            promotion.setDiscountValue(promotionDTO.getDiscountValue());
            promotion.setStatus(promotionDTO.getStatus() != null ? promotionDTO.getStatus() : PromotionStatus.DRAFT);
            promotion.setCreatedBy(creator);

            Promotion savedPromotion = promotionRepository.save(promotion);
            return convertToResponseDTO(savedPromotion);

        } catch (Exception e) {
            log.error("Error creating promotion: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi tạo khuyến mãi: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PromotionResponseDTO updatePromotion(Integer id, PromotionDTO promotionDTO) {
        try {
            Promotion existingPromotion = promotionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại với ID: " + id));

            // Check if program name is duplicated (excluding current promotion)
            if (!existingPromotion.getProgramName().equals(promotionDTO.getProgramName()) &&
                    promotionRepository.existsByProgramName(promotionDTO.getProgramName())) {
                throw new RuntimeException("Tên chương trình khuyến mãi đã tồn tại");
            }

            // Validate dates
            if (promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
            }

            // Update user if changed
            if (existingPromotion.getCreatedBy().getUserId() != promotionDTO.getCreatedBy()) {
                User newCreator = userRepository.findById(promotionDTO.getCreatedBy())
                        .orElseThrow(() -> new RuntimeException("Người tạo mới không tồn tại"));
                existingPromotion.setCreatedBy(newCreator);
            }

            existingPromotion.setProgramName(promotionDTO.getProgramName());
            existingPromotion.setDescription(promotionDTO.getDescription());
            existingPromotion.setStartDate(promotionDTO.getStartDate());
            existingPromotion.setEndDate(promotionDTO.getEndDate());
            existingPromotion.setConditions(promotionDTO.getConditions());
            existingPromotion.setDiscountValue(promotionDTO.getDiscountValue());

            // Only update status if provided
            if (promotionDTO.getStatus() != null) {
                existingPromotion.setStatus(promotionDTO.getStatus());
            }

            Promotion updatedPromotion = promotionRepository.save(existingPromotion);
            return convertToResponseDTO(updatedPromotion);

        } catch (Exception e) {
            log.error("Error updating promotion: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi cập nhật khuyến mãi: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deletePromotion(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại với ID: " + id));

        promotionRepository.delete(promotion);
        log.info("Deleted promotion with ID: {}", id);
    }

    private PromotionResponseDTO convertToResponseDTO(Promotion promotion) {
        PromotionResponseDTO dto = new PromotionResponseDTO();
        dto.setId(promotion.getId());
        dto.setProgramName(promotion.getProgramName());
        dto.setDescription(promotion.getDescription());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setConditions(promotion.getConditions());
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setStatus(promotion.getStatus());

        // Set user information
        if (promotion.getCreatedBy() != null) {
            dto.setCreatedBy(promotion.getCreatedBy().getUserId());
            dto.setCreatedByName(promotion.getCreatedBy().getFullName());
            dto.setCreatedByEmail(promotion.getCreatedBy().getEmail());
        }

        return dto;
    }
}