package com.example.demo.controller;

import com.example.demo.dto.InstallmentPlanDTO;
import com.example.demo.dto.InstallmentRequest;
import com.example.demo.entity.InstallmentSchedule;
import com.example.demo.service.InstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/installments")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Installment Management", description = "APIs for managing installment payment plans")
@SecurityRequirement(name = "bearer-jwt")
public class InstallmentController {

    private final InstallmentService installmentService;

    @PostMapping("/preview")
    @Operation(summary = "Xem trước kế hoạch trả góp (Preview)",
            description = "Tính toán trước kế hoạch trả góp, bao gồm VAT, lãi suất, và chi tiết từng kỳ thanh toán.")
    public ResponseEntity<InstallmentPlanDTO> previewInstallmentPlan(@RequestBody InstallmentRequest request) {
        return ResponseEntity.ok(installmentService.previewInstallmentPlan(request));
    }

    @PostMapping("/{orderId}/generate")
    @Operation(summary = "Tạo lịch trả góp cho đơn hàng",
            description = "Tạo lịch trả góp chi tiết (Installment Schedule) trong cơ sở dữ liệu cho đơn hàng chỉ định.")
    public ResponseEntity<List<InstallmentSchedule>> generateInstallmentSchedule(
            @PathVariable Integer orderId,
            @RequestBody InstallmentRequest request) {
        return ResponseEntity.ok(installmentService.generateSchedule(orderId, request));
    }

    @PutMapping("/pay/{scheduleId}")
    @Operation(summary = "Thanh toán một kỳ trả góp",
            description = "Đánh dấu một kỳ trả góp là đã thanh toán (PAID) và tự động cập nhật công nợ khách hàng.")
    public ResponseEntity<InstallmentSchedule> markInstallmentAsPaid(@PathVariable Integer scheduleId) {
        return ResponseEntity.ok(installmentService.markInstallmentPaid(scheduleId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Lấy lịch trả góp theo Order",
            description = "Truy xuất toàn bộ lịch trả góp (các kỳ thanh toán) của một đơn hàng cụ thể.")
    public ResponseEntity<List<InstallmentSchedule>> getInstallmentsByOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(installmentService.getSchedulesByOrderId(orderId));
    }
}
