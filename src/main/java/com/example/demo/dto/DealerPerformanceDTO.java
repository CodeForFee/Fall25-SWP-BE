package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DealerPerformanceDTO {
    private Integer dealerId;
    private String dealerName;
    private BigDecimal totalSales;
    private Long totalOrders;
}
