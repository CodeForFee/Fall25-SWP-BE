package com.example.demo.controller;

import com.example.demo.dto.AuditLogResponseDTO;
import com.example.demo.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Audit Log Management", description = "APIs for audit log and system monitoring")
@SecurityRequirement(name = "bearer-jwt")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả audit logs", description = "Lấy danh sách tất cả các log hoạt động trong hệ thống")
    public ResponseEntity<Page<AuditLogResponseDTO>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditLogService.getAllLogs(page, size));
    }

    @GetMapping("/entity-type/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy logs theo loại entity", description = "Lấy log theo loại entity (Dealer, Promotion, User)")
    public ResponseEntity<Page<AuditLogResponseDTO>> getLogsByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditLogService.getLogsByEntityType(entityType, page, size));
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy logs theo action", description = "Lấy log theo hành động (CREATE, UPDATE, DELETE)")
    public ResponseEntity<Page<AuditLogResponseDTO>> getLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditLogService.getLogsByAction(action, page, size));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy lịch sử thay đổi của entity", description = "Lấy lịch sử thay đổi của một entity cụ thể")
    public ResponseEntity<List<AuditLogResponseDTO>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId
    ) {
        return ResponseEntity.ok(auditLogService.getEntityHistory(entityType, entityId));
    }
}