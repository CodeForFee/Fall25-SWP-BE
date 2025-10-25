
package com.example.demo.service;

import com.example.demo.dto.PromotionDTO;
import com.example.demo.dto.PromotionResponseDTO;
import java.util.List;

public interface PromotionService {
    // CRUD Operations - 5 endpoints
    List<PromotionResponseDTO> getAllPromotions();
    PromotionResponseDTO getPromotionById(Integer id);
    PromotionResponseDTO createPromotion(PromotionDTO promotionDTO);
    PromotionResponseDTO updatePromotion(Integer id, PromotionDTO promotionDTO);
    void deletePromotion(Integer id);
}