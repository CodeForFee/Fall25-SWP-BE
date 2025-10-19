package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDTO {
    private Integer customerId;
    private Integer userId;
    private LocalDate createdDate;
    private BigDecimal totalAmount;
    private String status;
    private LocalDate validUntil;
    private List<QuoteDetailDTO> quoteDetails;
}