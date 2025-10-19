package com.example.demo.dto;


import com.example.demo.entity.Promotion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PromotionResponseDTO {
    private Integer id;
    private String programName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String conditions;
    private Double discountValue;
    private Promotion.PromotionStatus status;

    // Thông tin user tạo promotion
    private int createdBy;
    private String createdByName;
    private String createdByEmail;
}