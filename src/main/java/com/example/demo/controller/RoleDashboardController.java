package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Role Dashboards", description = "Role-specific dashboard APIs")
public class RoleDashboardController {

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard cho ADMIN")
    public Map<String, String> adminDashboard() {
        return Map.of(
                "message", "Chào mừng đến ADMIN Dashboard",
                "role", "ADMIN",
                "description", "Đây là trang dashboard dành cho ADMIN"
        );
    }

    @GetMapping("/dealer_manager/dashboard")
    @PreAuthorize("hasRole('DEALER_MANAGER')")
    @Operation(summary = "Dashboard cho DEALER MANAGER")
    public Map<String, String> dealerManagerDashboard() {
        return Map.of(
                "message", "Chào mừng đến DEALER MANAGER Dashboard",
                "role", "DEALER_MANAGER",
                "description", "Đây là trang dashboard dành cho DEALER MANAGER"
        );
    }

    @GetMapping("/dealer_staff/dashboard")
    @PreAuthorize("hasRole('DEALER_STAFF')")
    @Operation(summary = "Dashboard cho DEALER STAFF")
    public Map<String, String> dealerStaffDashboard() {
        return Map.of(
                "message", "Chào mừng đến DEALER STAFF Dashboard",
                "role", "DEALER_STAFF",
                "description", "Đây là trang dashboard dành cho DEALER STAFF"
        );
    }

    @GetMapping("/evm_manager/dashboard")
    @PreAuthorize("hasRole('EVM_MANAGER')")
    @Operation(summary = "Dashboard cho EVM MANAGER")
    public Map<String, String> evmManagerDashboard() {
        return Map.of(
                "message", "Chào mừng đến EVM MANAGER Dashboard",
                "role", "EVM_MANAGER",
                "description", "Đây là trang dashboard dành cho EVM MANAGER"
        );
    }

    @GetMapping("/user/dashboard")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Dashboard cho USER")
    public Map<String, String> userDashboard() {
        return Map.of(
                "message", "Chào mừng đến USER Dashboard",
                "role", "USER",
                "description", "Đây là trang dashboard dành cho USER"
        );
    }
}