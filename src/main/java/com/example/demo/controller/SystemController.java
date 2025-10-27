package com.example.demo.controller;

import com.example.demo.dto.SystemStatusDTO;
import com.example.demo.dto.DealerPerformanceDTO;
import com.example.demo.service.SystemService;
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
  CONTROLLER CHO EVM STAFF (màu vàng)
  - API 1: Tổng hợp trạng thái đơn hàng toàn hệ thống
  - API 2: Tổng hợp doanh số toàn hệ thống
  - API 3: Tổng hợp hiệu suất từng dealer
*/

@RestController
@RequestMapping("/api/evm")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "EVM System Summary", description = "API tổng hợp dữ liệu toàn hệ thống cho EVM Staff")
@SecurityRequirement(name = "bearer-jwt")
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/system-status")
    @Operation(
            summary = "Tổng hợp trạng thái & doanh số toàn hệ thống",
            description = "Trả về số lượng đơn hàng theo trạng thái và tổng doanh số giữa khoảng thời gian from/to"
    )
    public ResponseEntity<SystemStatusDTO> getSystemStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // Gọi service xử lý và trả kết quả cho FE
        SystemStatusDTO result = systemService.aggregateSystemStatus(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dealer-performance")
    @Operation(
            summary = "Tổng hợp hiệu suất từng đại lý",
            description = "Trả về danh sách hiệu suất hoạt động và doanh số theo từng đại lý trong khoảng thời gian from/to"
    )
    public ResponseEntity<List<DealerPerformanceDTO>> dealerPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<DealerPerformanceDTO> result = systemService.dealerPerformance(from, to);
        return ResponseEntity.ok(result);
    }
}