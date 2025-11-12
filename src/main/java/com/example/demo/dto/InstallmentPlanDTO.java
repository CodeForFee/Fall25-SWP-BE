package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO trả về thông tin tổng thể gói trả góp (bao gồm chi tiết từng kỳ)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentPlanDTO {
    private BigDecimal totalAmount;          // Giá xe gốc (chưa VAT, chưa lãi)
    private BigDecimal vatAmount;            // Thuế VAT 10%
    private BigDecimal interestAmount;       // Tổng lãi phải trả
    private BigDecimal totalPayable;         // Tổng số tiền phải trả (gốc + VAT + lãi)
    private BigDecimal monthlyPayment;       // Số tiền trung bình mỗi tháng
    private int months;                      // Số kỳ trả góp
    private LocalDate firstDueDate;          // Ngày đến hạn đầu tiên
    private List<InstallmentScheduleDTO> schedule; // Danh sách chi tiết các kỳ
}
