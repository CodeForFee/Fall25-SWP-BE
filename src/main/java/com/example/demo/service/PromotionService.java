package com.example.demo.service;

import com.example.demo.dto.PromotionDTO;
import com.example.demo.dto.PromotionResponseDTO;
import com.example.demo.entity.PromotionStatus;


import java.time.LocalDate;
import java.util.List;

public interface PromotionService {

    PromotionResponseDTO createPromotion(PromotionDTO promotionDTO);

    List<PromotionResponseDTO> getAllPromotions();

    PromotionResponseDTO getPromotionById(Integer id);

    PromotionResponseDTO updatePromotion(Integer id, PromotionDTO promotionDTO);

    void deletePromotion(Integer id);

    PromotionResponseDTO updatePromotionStatus(Integer id, PromotionStatus status);

    List<PromotionResponseDTO> getPromotionsByStatus(PromotionStatus status);

    List<PromotionResponseDTO> searchPromotionsByName(String programName);

    List<PromotionResponseDTO> getActivePromotions(LocalDate date);

    List<PromotionResponseDTO> getExpiredPromotions();

    List<PromotionResponseDTO> getPromotionsByUser(Integer userId);

    List<PromotionResponseDTO> getPromotionsByDealer(Integer dealerId);
}