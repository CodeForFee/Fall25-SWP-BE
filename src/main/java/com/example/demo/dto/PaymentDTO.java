package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Integer paymentId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String status;
    private String paymentMethod;
    private Integer paymentPercentage;
    private String notes;
    private LocalDateTime createdAt;
}
