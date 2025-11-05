package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String status  = "DRAFT";
    private LocalDate validUntil;
    private List<QuoteDetailDTO> quoteDetails;
}