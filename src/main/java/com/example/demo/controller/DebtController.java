package com.example.demo.controller;

import com.example.demo.dto.CustomerDebtInfo;
import com.example.demo.dto.DealerDebtInfo;
import com.example.demo.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Debt Management", description = "API quản lý công nợ khách hàng và đại lý")
@SecurityRequirement(name = "bearer-jwt")
public class DebtController {

    private final DebtService debtService;

    @GetMapping("/customers")
    @Operation(summary = "Lấy danh sách công nợ khách hàng")
    public ResponseEntity<List<CustomerDebtInfo>> getCustomerDebts() {
        return ResponseEntity.ok(debtService.getCustomerDebts());
    }

    @GetMapping("/dealers")
    @Operation(summary = "Lấy danh sách công nợ đại lý")
    public ResponseEntity<List<DealerDebtInfo>> getDealerDebts() {
        return ResponseEntity.ok(debtService.getDealerDebts());
    }
}
