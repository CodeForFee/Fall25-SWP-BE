package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentStatusResponse {

    private Integer orderId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    private boolean fullyPaid;

    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer remainingInstallments;

    private LocalDate nextDueDate;
    private Integer nextInstallmentNumber;

    private Integer overdueInstallments;
}
