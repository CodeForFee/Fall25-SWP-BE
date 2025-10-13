package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.entity.ActivityLog;

import java.util.Map;

public interface ActivityService {
    void record(Long actorId, String actorEmail,
                String action, String targetType, String targetId,
                String message, Map<String, Object> metadata);

    Page<ActivityLog> listMine(Integer actorId, String action, Pageable pageable);
}
