package com.example.demo.controller;

import com.example.demo.dto.PromotionDTO;
import com.example.demo.dto.PromotionResponseDTO;
import com.example.demo.entity.PromotionStatus;
import com.example.demo.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Promotion Management", description = "APIs for managing promotions and discount programs")
@SecurityRequirement(name = "bearerAuth") // THÊM DÒNG NÀY
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @Operation(summary = "Tạo khuyến mãi mới", description = "Tạo một chương trình khuyến mãi mới trong hệ thống")
    public PromotionResponseDTO createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return promotionService.createPromotion(promotionDTO);
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả khuyến mãi", description = "Lấy danh sách tất cả các chương trình khuyến mãi")
    public List<PromotionResponseDTO> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy khuyến mãi theo ID", description = "Lấy thông tin chi tiết của một khuyến mãi theo ID")
    public PromotionResponseDTO getPromotionById(@PathVariable Integer id) {
        return promotionService.getPromotionById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật khuyến mãi", description = "Cập nhật thông tin của một khuyến mãi")
    public PromotionResponseDTO updatePromotion(@PathVariable Integer id, @RequestBody PromotionDTO promotionDTO) {
        return promotionService.updatePromotion(id, promotionDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa khuyến mãi", description = "Xóa một khuyến mãi khỏi hệ thống")
    public ResponseEntity<String> deletePromotion(@PathVariable Integer id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok("Khuyến mãi đã được xóa thành công");
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái khuyến mãi", description = "Cập nhật trạng thái của khuyến mãi (ACTIVE, INACTIVE, DRAFT, EXPIRED)")
    public PromotionResponseDTO updatePromotionStatus(@PathVariable Integer id, @RequestParam PromotionStatus status) {
        return promotionService.updatePromotionStatus(id, status);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy khuyến mãi theo trạng thái", description = "Lấy danh sách khuyến mãi theo trạng thái")
    public List<PromotionResponseDTO> getPromotionsByStatus(@PathVariable PromotionStatus status) {
        return promotionService.getPromotionsByStatus(status);
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm khuyến mãi theo tên", description = "Tìm kiếm khuyến mãi theo tên chương trình")
    public List<PromotionResponseDTO> searchPromotionsByName(@RequestParam String programName) {
        return promotionService.searchPromotionsByName(programName);
    }

    @GetMapping("/active")
    @Operation(summary = "Lấy khuyến mãi đang hoạt động", description = "Lấy danh sách khuyến mãi đang hoạt động")
    public List<PromotionResponseDTO> getActivePromotions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return promotionService.getActivePromotions(date);
    }

    @GetMapping("/expired")
    @Operation(summary = "Lấy khuyến mãi đã hết hạn", description = "Lấy danh sách khuyến mãi đã hết hạn")
    public List<PromotionResponseDTO> getExpiredPromotions() {
        return promotionService.getExpiredPromotions();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy khuyến mãi theo user", description = "Lấy danh sách khuyến mãi được tạo bởi user")
    public List<PromotionResponseDTO> getPromotionsByUser(@PathVariable Integer userId) {
        return promotionService.getPromotionsByUser(userId);
    }

    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Lấy khuyến mãi theo dealer", description = "Lấy danh sách khuyến mãi theo dealer")
    public List<PromotionResponseDTO> getPromotionsByDealer(@PathVariable Integer dealerId) {
        return promotionService.getPromotionsByDealer(dealerId);
    }
}