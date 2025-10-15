package com.example.demo.controller;

import com.example.demo.dto.DealerDTO;
import com.example.demo.dto.DealerResponseDTO;
import com.example.demo.service.DealerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dealers")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Dealer Management", description = "APIs for dealer management")
@SecurityRequirement(name = "bearer-jwt")
public class DealerController {

    private final DealerService dealerService;

    @GetMapping
    @Operation(summary = "Lấy tất cả dealers")
    public ResponseEntity<List<DealerResponseDTO>> getAllDealers() {
        return ResponseEntity.ok(dealerService.getAllDealers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy dealer theo ID")
    public ResponseEntity<DealerResponseDTO> getDealerById(@PathVariable Integer id) {
        return ResponseEntity.ok(dealerService.getDealerById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo dealer mới")
    public ResponseEntity<DealerResponseDTO> createDealer(@RequestBody DealerDTO dealerDTO) {
        return ResponseEntity.ok(dealerService.createDealer(dealerDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật dealer")
    public ResponseEntity<DealerResponseDTO> updateDealer(@PathVariable Integer id, @RequestBody DealerDTO dealerDTO) {
        return ResponseEntity.ok(dealerService.updateDealer(id, dealerDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa dealer")
    public ResponseEntity<String> deleteDealer(@PathVariable Integer id) {
        dealerService.deleteDealer(id);
        return ResponseEntity.ok("Dealer deleted successfully");
    }
}