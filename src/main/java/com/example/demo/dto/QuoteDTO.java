package com.example.demo.dto;

import com.example.demo.entity.User;
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
    private User.Role creatorRole;
    private Integer dealerId;
    private LocalDate createdDate;
    private String status = "DRAFT";
    private LocalDate validUntil;
    private List<QuoteDetailDTO> quoteDetails;
}