package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "AuditLogs")
public class AuditLog {

    @Id
    @Column(name = "logId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "entityType", nullable = false, length = 50)
    private String entityType; // Dealer, Promotion, User

    @Column(name = "entityId", length = 100)
    private String entityId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "ipAddress", length = 50)
    private String ipAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON string

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}