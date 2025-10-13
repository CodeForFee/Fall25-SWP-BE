package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);
    Page<ActivityLog> findByActorIdAndActionOrderByCreatedAtDesc(Long actorId, String action, Pageable pageable);
  }
  