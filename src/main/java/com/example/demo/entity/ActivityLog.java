package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity 
@Table(name = "activity_log")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ActivityLog {
  
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) 
    private Long actorId;

    @Column(length=128) 
    private String actorEmail;

    @Column(nullable=false, length=64) 
    private String action;     // PASSWORD_CHANGED, LOGOUT, ...

    @Column(length=64) 
    private String targetType;                 // USER, ORDER, ...

    @Column(length=64) 
    private String targetId;

    @Column(columnDefinition="TEXT")
    private String message;      // câu hiển thị

    @Lob
    @Column(columnDefinition="TEXT")
    private String metadataJson; // JSON string

    @CreationTimestamp 
    @Column(nullable=false, updatable=false)
    private Instant createdAt;
}

