package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Integer quoteId;
    private Integer customerId;
    private Integer dealerId;
    private Integer userId;
    private LocalDate orderDate;
    private String status;
    private Integer paymentPercentage;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentNotes;
    private String notes;
    private BigDecimal paidAmount;
    private String createdByRole;
    private List<OrderDetailDTO> orderDetails;

    // ====== FIELD MÌNH THÊM ======

    // ID đơn hàng – cần cho portal
    private Integer orderId;

    // Tổng giá trị đơn
    private BigDecimal totalAmount;

    // Số tiền còn lại phải trả
    private BigDecimal remainingAmount;

    // Ngày thanh toán gần nhất
    private LocalDateTime lastPaymentDate;

    // Số tháng trả góp (nếu phương thức là INSTALLMENT)
    private Integer installmentMonths;

    // Danh sách lịch sử thanh toán
    private List<PaymentDTO> payments;

    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer remainingInstallments;
    private Integer overdueInstallments;

    private LocalDate nextDueDate;
    private Integer nextInstallmentNumber;

    // Danh sách đầy đủ các kỳ trả góp
    private List<InstallmentScheduleDTO> installmentSchedules;

}
