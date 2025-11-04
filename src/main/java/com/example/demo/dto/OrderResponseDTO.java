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
public class OrderResponseDTO {
    private Integer id;
    private Integer quoteId;
    private Integer customerId;
    private Integer dealerId;
    private Integer userId;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String status;
    private String paymentMethod;
    private String notes;
    private List<OrderDetailResponseDTO> orderDetails;
    private BigDecimal totalDiscount;
    private String approvalStatus;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    private String approvalNotes;
    private String quoteStatus;
    private String quoteApprovalStatus;


}