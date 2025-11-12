package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TestDriveRequestDTO {

    private String customerName;
    private String customerEmail;
    private String phoneNumber;
    private String carModel;
    private Integer dealerId;
    private LocalDate date;
    private LocalDateTime requestTime;
    private String note;
}