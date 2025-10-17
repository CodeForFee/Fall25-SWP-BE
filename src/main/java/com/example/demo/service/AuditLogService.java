package com.example.demo.service;

import com.example.demo.dto.AuditLogResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AuditLogService {
    void log(String action, String entityType, String entityId, Object details);
    Page<AuditLogResponseDTO> getAllLogs(int page, int size);
    Page<AuditLogResponseDTO> getLogsByEntityType(String entityType, int page, int size);
    Page<AuditLogResponseDTO> getLogsByAction(String action, int page, int size);
    List<AuditLogResponseDTO> getEntityHistory(String entityType, String entityId);
}