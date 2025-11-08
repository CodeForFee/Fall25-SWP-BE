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
public class QuoteResponseDTO {
    private Integer id;
    private Integer customerId;
    private Integer userId;
    private LocalDate createdDate;
    private BigDecimal totalAmount;
    private String status;
    private String approvalStatus;
    private LocalDate validUntil;
    private List<QuoteDetailResponseDTO> quoteDetails;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    private String approvalNotes;
}