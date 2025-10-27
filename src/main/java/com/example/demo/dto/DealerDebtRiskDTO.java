package com.example.demo.dto;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DealerDebtRiskDTO {
    private Integer  dealerId;
    private String dealerName;
    private BigDecimal totalOutstanding;
    private String riskLevel;
}
