package com.example.demo.controller;

import com.example.demo.dto.DealerDebtRiskDTO;
import com.example.demo.dto.InventorySpeedDTO;
import com.example.demo.service.DealerAggregateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/*
  CONTROLLER CHO DEALER MANAGER (màu hồng)
  - API 4: Tổng hợp công nợ, xử lý logic rủi ro
  - API 5: Tổng hợp dữ liệu bán hàng theo đại lý
  - API 6: Query tồn kho, tốc độ bán
*/

@RestController
@RequestMapping("/api/dealer")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Dealer Summary", description = "API tổng hợp dữ liệu, công nợ và tốc độ bán dành cho Dealer Manager")
@SecurityRequirement(name = "bearer-jwt")
public class DealerAggregateController {

    private final DealerAggregateService service;

    @GetMapping("/debt-risk")
    @Operation(
            summary = "Báo cáo công nợ & rủi ro",
            description = "Trả về danh sách công nợ của các đại lý và mức độ rủi ro trong khoảng thời gian from/to"
    )
    public ResponseEntity<List<DealerDebtRiskDTO>> debtRiskReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<DealerDebtRiskDTO> result = service.aggregateDebtRisk(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sales-by-dealer")
    @Operation(
            summary = "Tổng hợp dữ liệu bán hàng theo đại lý",
            description = "Trả về tổng doanh số và các chỉ số bán hàng của từng đại lý trong khoảng thời gian from/to"
    )
    public ResponseEntity<List<?>> salesByDealer(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<?> result = service.salesDataByDealer(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/inventory-speed")
    @Operation(
            summary = "Thống kê tồn kho & tốc độ bán",
            description = "Trả về dữ liệu tồn kho hiện tại và tốc độ tiêu thụ của các dòng xe trong khoảng thời gian from/to"
    )
    public ResponseEntity<List<InventorySpeedDTO>> inventorySpeed(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<InventorySpeedDTO> result = service.inventoryAndSellSpeed(from, to);
        return ResponseEntity.ok(result);
    }
}
