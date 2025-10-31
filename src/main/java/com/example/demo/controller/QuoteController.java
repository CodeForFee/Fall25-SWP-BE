package com.example.demo.controller;

import com.example.demo.dto.QuoteDTO;
import com.example.demo.dto.QuoteResponseDTO;
import com.example.demo.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Quote Management", description = "APIs for quote management")
@SecurityRequirement(name = "bearer-jwt")
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping
    @Operation(summary = "Lấy tất cả báo giá")
    public ResponseEntity<List<QuoteResponseDTO>> getAllQuotes() {
        return ResponseEntity.ok(quoteService.getAllQuotes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy báo giá theo ID")
    public ResponseEntity<QuoteResponseDTO> getQuoteById(@PathVariable Integer id) {
        return ResponseEntity.ok(quoteService.getQuoteById(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Lấy báo giá theo ID khách hàng")
    public ResponseEntity<List<QuoteResponseDTO>> getQuotesByCustomerId(@PathVariable Integer customerId) {
        return ResponseEntity.ok(quoteService.getQuotesByCustomerId(customerId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy báo giá theo ID người dùng")
    public ResponseEntity<List<QuoteResponseDTO>> getQuotesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(quoteService.getQuotesByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy báo giá theo trạng thái")
    public ResponseEntity<List<QuoteResponseDTO>> getQuotesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(quoteService.getQuotesByStatus(status));
    }

    @GetMapping("/approved")
    @Operation(summary = "Lấy danh sách báo giá đã được duyệt")
    public ResponseEntity<List<QuoteResponseDTO>> getApprovedQuotes() {
        return ResponseEntity.ok(quoteService.getApprovedQuotes());
    }

    @PostMapping
    @Operation(summary = "Tạo báo giá mới")
    public ResponseEntity<QuoteResponseDTO> createQuote(@RequestBody QuoteDTO quoteDTO) {
        return ResponseEntity.ok(quoteService.createQuote(quoteDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật báo giá")
    public ResponseEntity<QuoteResponseDTO> updateQuote(@PathVariable Integer id, @RequestBody QuoteDTO quoteDTO) {
        return ResponseEntity.ok(quoteService.updateQuote(id, quoteDTO));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Phê duyệt báo giá")
    public ResponseEntity<QuoteResponseDTO> approveQuote(@PathVariable Integer id) {
        return ResponseEntity.ok(quoteService.approveQuote(id));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Từ chối báo giá")
    public ResponseEntity<QuoteResponseDTO> rejectQuote(@PathVariable Integer id) {
        return ResponseEntity.ok(quoteService.rejectQuote(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa báo giá")
    public ResponseEntity<String> deleteQuote(@PathVariable Integer id) {
        quoteService.deleteQuote(id);
        return ResponseEntity.ok("Báo giá đã được xóa thành công");
    }

    @PostMapping("/expire-old")
    @Operation(summary = "Cập nhật trạng thái hết hạn cho báo giá cũ")
    public ResponseEntity<String> expireOldQuotes() {
        quoteService.expireOldQuotes();
        return ResponseEntity.ok("Đã cập nhật trạng thái hết hạn cho báo giá cũ");
    }
}