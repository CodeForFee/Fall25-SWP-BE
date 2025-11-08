package com.example.demo.entity;

import jakarta.persistence.Entity;
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

    private String CustomerName;
    private String CustomerEmail;
    private String PhoneNumber;
    private String Time;
    
    @ManyToOne
    @JoinColumn(name = "dealer") // Tên cột khóa ngoại trong CSDL
    private Dealer dealer;
}