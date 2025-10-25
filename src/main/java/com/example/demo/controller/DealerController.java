package com.example.demo.controller;

import com.example.demo.dto.DealerDTO;
import com.example.demo.dto.DealerResponseDTO;
import com.example.demo.service.DealerService;
import com.example.demo.service.AuditLogService;
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
    private final AuditLogService auditLogService;

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
        DealerResponseDTO result = dealerService.createDealer(dealerDTO);

        // Ghi audit log
        auditLogService.log("CREATE", "Dealer", result.getDealerId().toString(), dealerDTO);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật dealer")
    public ResponseEntity<DealerResponseDTO> updateDealer(@PathVariable Integer id, @RequestBody DealerDTO dealerDTO) {
        DealerResponseDTO result = dealerService.updateDealer(id, dealerDTO);

        // Ghi audit log
        auditLogService.log("UPDATE", "Dealer", id.toString(), dealerDTO);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa dealer")
    public ResponseEntity<String> deleteDealer(@PathVariable Integer id) {
        DealerResponseDTO dealer = dealerService.getDealerById(id);
        dealerService.deleteDealer(id);

        // Ghi audit log
        auditLogService.log("DELETE", "Dealer", id.toString(), dealer);

        return ResponseEntity.ok("Dealer deleted successfully");
    }
}