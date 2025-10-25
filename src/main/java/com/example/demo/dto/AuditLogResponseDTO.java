package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponseDTO {
    private Long logId;
    private String action;
    private String entityType;
    private String entityId;
    private String username;
    private String ipAddress;
    private Object details; // Parsed JSON
    private LocalDateTime createdAt;
}