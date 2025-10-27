package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO nhận yêu cầu tạo gói trả góp
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentRequest {
    private Integer paymentId;
    private BigDecimal totalAmount;
    private int months; // số kỳ trả góp
    private BigDecimal annualInterestRate; // lãi suất (%/năm)
    private LocalDate firstDueDate; // ngày đến hạn kỳ đầu tiên
}
