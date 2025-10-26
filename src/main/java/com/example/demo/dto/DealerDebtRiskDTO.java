package com.example.demo.dto;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DealerDebtRiskDTO {
    private Long dealerId;
    private String dealerName;
    private BigDecimal totalOutstanding; // total remaining_amount
    private String riskLevel; //
}
