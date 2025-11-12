package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerDebtInfo {
    private Integer dealerId;
    private String name;
    private String phone;
    private String region;
    private BigDecimal outstandingDebt;
    private String status;
}
