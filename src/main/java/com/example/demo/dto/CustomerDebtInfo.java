package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDebtInfo {
    private Integer customerId;
    private String fullName;
    private String phone;
    private String email;
    private BigDecimal totalSpent;
    private BigDecimal totalDebt;
    private Boolean isVip;
    private String dealerName;
    private Integer dealerId;

}
