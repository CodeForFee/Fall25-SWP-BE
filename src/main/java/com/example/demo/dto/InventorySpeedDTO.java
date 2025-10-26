package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class InventorySpeedDTO {
    private Integer dealerId;
    private String dealerName;
    private Integer availableQuantity;
    private Integer soldLastPeriod;
    private BigDecimal sellThroughRate; // sold / (available + sold) nếu có
}
