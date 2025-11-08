package com.example.demo.controller;

import com.example.demo.dto.InstallmentRequest;
import com.example.demo.dto.InstallmentScheduleDTO;
import com.example.demo.service.InstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
  CONTROLLER CHO TRẢ GÓP (INSTALLMENT)
  - API 1: Tạo gói trả góp mới
  - API 2: Xem danh sách các kỳ trả góp
  - API 3: Đánh dấu kỳ đã thanh toán
*/

@RestController
@RequestMapping("/api/installments")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Installment Management", description = "API quản lý gói và kỳ trả góp trong hệ thống")
@SecurityRequirement(name = "bearer-jwt")
public class InstallmentController {

    private final InstallmentService installmentService;

    @PostMapping("/create")
    @Operation(
            summary = "Tạo gói trả góp mới",
            description = "API này cho phép tạo kế hoạch trả góp mới cho một thanh toán cụ thể (Payment)."
    )
    public ResponseEntity<List<InstallmentScheduleDTO>> createInstallment(
            @RequestBody InstallmentRequest req
    ) {
        // Gọi service để tạo gói trả góp
        List<InstallmentScheduleDTO> result = installmentService.createInstallmentPlan(req);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{paymentId}")
    @Operation(
            summary = "Xem danh sách các kỳ trả góp",
            description = "Trả về danh sách các kỳ trả góp tương ứng với một Payment cụ thể."
    )
    public ResponseEntity<List<InstallmentScheduleDTO>> getInstallments(
            @PathVariable Integer paymentId
    ) {
        // Gọi service lấy danh sách kỳ trả góp
        List<InstallmentScheduleDTO> result = installmentService.getInstallments(paymentId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{transactionId}/pay")
    @Operation(
            summary = "Đánh dấu kỳ trả góp đã thanh toán",
            description = "API này cho phép xác nhận một kỳ trả góp đã được thanh toán thành công."
    )
    public ResponseEntity<String> payInstallment(
            @PathVariable Integer transactionId,
            @RequestParam(defaultValue = "INSTALLMENT") String method
    ) {
        // Gọi service để đánh dấu kỳ trả góp đã thanh toán
        installmentService.markAsPaid(transactionId, method);
        return ResponseEntity.ok("Thanh toán kỳ trả góp thành công!");
    }
}
