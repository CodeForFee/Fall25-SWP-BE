package com.example.demo.controller;

import com.example.demo.dto.CustomerPortalResponse;
import com.example.demo.service.CustomerPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/customer/portal")
@RequiredArgsConstructor
@Tag(
        name = "Customer Portal",
        description = "API dành cho khách hàng tra cứu thông tin cá nhân + đơn hàng + thanh toán"
)
public class CustomerPortalController {

    private final CustomerPortalService portalService;

    @GetMapping
    @Operation(
            summary = "Tra cứu thông tin khách hàng",
            description = """
                Khách hàng nhập customerId + citizenId (CCCD) để xem:
                - Thông tin cá nhân
                - Danh sách đơn hàng
                - Lịch sử thanh toán (trả góp + công nợ)
                """
    )
    public ResponseEntity<?> getCustomerPortal(
            @RequestParam Integer customerId,
            @RequestParam String citizenId
    ) {
        try {
            CustomerPortalResponse response = portalService.getPortalData(customerId, citizenId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(
                    java.util.Map.of("error", ex.getMessage())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(
                    java.util.Map.of("error", "Lỗi hệ thống")
            );
        }
    }
}
