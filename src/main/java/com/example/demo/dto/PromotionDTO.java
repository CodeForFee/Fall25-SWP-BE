package com.example.demo.dto;

import com.example.demo.entity.PromotionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PromotionDTO {
    private String programName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String conditions;
    private Double discountValue;
    private PromotionStatus status;
    private Integer createdBy; // User ID
}