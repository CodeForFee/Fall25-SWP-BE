package com.example.demo.service.impl;

import com.example.demo.dto.AuditLogResponseDTO;
import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void log(String action, String entityType, String entityId, Object details) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String username = getCurrentUsername();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .username(username)
                    .ipAddress(request != null ? getClientIp(request) : "unknown")
                    .details(objectMapper.writeValueAsString(details))
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit log created: {} {} by {}", action, entityType, username);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<AuditLogResponseDTO> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public Page<AuditLogResponseDTO> getLogsByEntityType(String entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findByEntityType(entityType, pageable).map(this::convertToDTO);
    }

    @Override
    public Page<AuditLogResponseDTO> getLogsByAction(String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findByAction(action, pageable).map(this::convertToDTO);
    }

    @Override
    public List<AuditLogResponseDTO> getEntityHistory(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AuditLogResponseDTO convertToDTO(AuditLog auditLog) {
        return AuditLogResponseDTO.builder()
                .logId(auditLog.getLogId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .username(auditLog.getUsername())
                .ipAddress(auditLog.getIpAddress())
                .details(parseJson(auditLog.getDetails()))
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}