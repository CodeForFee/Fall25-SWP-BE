package com.example.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test_drive_requests")
@Getter
@Setter
public class TestDriveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String customerEmail;
    private String phoneNumber;
    private String carModel;
    private LocalDate date;
    @Column(name = "time")
    private OffsetDateTime requestTime;
    private String note;
    
    @Enumerated(EnumType.STRING)
    private TestDriveStatus status;

    @ManyToOne
    @JoinColumn(name = "dealerId") // Tên cột khóa ngoại trong CSDL
    private Dealer dealer;

    public enum TestDriveStatus {
        PENDING,   
        CONFIRMED,  
        REJECTED, 
    }
}