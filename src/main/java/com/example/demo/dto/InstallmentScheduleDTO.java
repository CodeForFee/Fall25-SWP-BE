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

    private Integer id;               // ID của kỳ trả góp
    private Integer installmentNumber; // Số kỳ
    private BigDecimal amount;         // Số tiền kỳ này phải trả
    private LocalDate dueDate;         // Hạn trả
    private LocalDate paidDate;        // Ngày đã trả (nếu có)
    private String status;             // PENDING | PAID | OVERDUE
    private String note;               // Ghi chú thêm
}
