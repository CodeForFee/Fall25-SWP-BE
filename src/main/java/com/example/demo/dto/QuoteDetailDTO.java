package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDetailDTO {
    private Integer quoteId;
    private Integer vehicleId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal promotionDiscount;
}