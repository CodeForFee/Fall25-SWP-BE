package com.example.demo.controller;

import com.example.demo.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/debts")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Quản lý công nợ", description = "API xử lý công nợ cho Khách hàng và Đại lý")
@SecurityRequirement(name = "bearer-jwt")
public class DebtController {

    private final DebtService debtService;

    // ====================== KHÁCH HÀNG ======================

    @PostMapping("/customer/{id}/add")
    @Operation(summary = "Thêm công nợ cho khách hàng")
    public ResponseEntity<String> addCustomerDebt(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        debtService.addCustomerDebt(id, amount);
        return ResponseEntity.ok("✅ Đã thêm công nợ cho khách hàng ID: " + id + " - Số tiền: " + amount + " VNĐ");
    }

    @PostMapping("/customer/{id}/pay")
    @Operation(summary = "Khách hàng thanh toán nợ")
    public ResponseEntity<String> reduceCustomerDebt(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal payment = body.get("payment");
        debtService.reduceCustomerDebt(id, payment);
        return ResponseEntity.ok("💰 Khách hàng ID: " + id + " đã thanh toán: " + payment + " VNĐ");
    }

    // ====================== ĐẠI LÝ ======================

    @PostMapping("/dealer/{id}/add")
    @Operation(summary = "Thêm công nợ cho đại lý")
    public ResponseEntity<String> addDealerDebt(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        debtService.addDealerDebt(id, amount);
        return ResponseEntity.ok("✅ Đã thêm công nợ cho đại lý ID: " + id + " - Số tiền: " + amount + " VNĐ");
    }

    @PostMapping("/dealer/{id}/pay")
    @Operation(summary = "Đại lý thanh toán công nợ")
    public ResponseEntity<String> reduceDealerDebt(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal payment = body.get("payment");
        debtService.reduceDealerDebt(id, payment);
        return ResponseEntity.ok("💰 Đại lý ID: " + id + " đã thanh toán: " + payment + " VNĐ");
    }
}
