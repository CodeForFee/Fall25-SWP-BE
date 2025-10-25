package com.example.demo.service.impl;

import com.example.demo.entity.ActivityLog;
import com.example.demo.repository.ActivityLogRepository;
import com.example.demo.service.ActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityServiceIMPL implements ActivityService {

    private final ActivityLogRepository repo;
    private final ObjectMapper objectMapper; // inject từ Spring

    @Override
    @Transactional(noRollbackFor = Exception.class) // log lỗi không rollback nghiệp vụ chính
    public void record(Long actorId, String actorEmail,
                       String action, String targetType, String targetId,
                       String message, Map<String, Object> metadata) {
        try {
            String json = (metadata == null || metadata.isEmpty())
                    ? null
                    : objectMapper.writeValueAsString(metadata);

            var log = ActivityLog.builder()
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .message(message)
                    .metadataJson(json)      // NVARCHAR(MAX) cho SQL Server
                    .build();

            repo.save(log);
        } catch (Exception e) {
            // Không để logging làm hỏng flow - chỉ log error nếu cần thiết
            log.debug("Activity log record failed: {}", e.getMessage());
        }
    }

    
    @Override
    public Page<ActivityLog> listMine(Integer actorId, String action, Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listMine'");
    }
}
