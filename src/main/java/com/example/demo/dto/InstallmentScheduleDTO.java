package com.example.demo.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
/** * DTO trả về thông tin từng kỳ trả góp */


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentScheduleDTO {
    private Integer installmentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String status;
    private LocalDate paidDate;
}

