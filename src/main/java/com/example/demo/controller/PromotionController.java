package com.example.demo.controller;

import com.example.demo.dto.PromotionDTO;
import com.example.demo.dto.PromotionResponseDTO;
import com.example.demo.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Promotion Management", description = "APIs for managing promotions and discount programs")
@SecurityRequirement(name = "bearer-jwt")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "Lấy tất cả khuyến mãi", description = "Lấy danh sách tất cả các chương trình khuyến mãi")
    public ResponseEntity<List<PromotionResponseDTO>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy khuyến mãi theo ID", description = "Lấy thông tin chi tiết của một khuyến mãi theo ID")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo khuyến mãi mới", description = "Tạo một chương trình khuyến mãi mới trong hệ thống")
    public ResponseEntity<PromotionResponseDTO> createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return ResponseEntity.ok(promotionService.createPromotion(promotionDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật khuyến mãi", description = "Cập nhật thông tin của một khuyến mãi")
    public ResponseEntity<PromotionResponseDTO> updatePromotion(@PathVariable Integer id, @RequestBody PromotionDTO promotionDTO) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, promotionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa khuyến mãi", description = "Xóa một khuyến mãi khỏi hệ thống")
    public ResponseEntity<String> deletePromotion(@PathVariable Integer id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok("Khuyến mãi đã được xóa thành công");
    }
}