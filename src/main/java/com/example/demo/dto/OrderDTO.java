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
public class OrderDTO {
    private Integer quoteId;
    private Integer customerId;
    private Integer dealerId;
    private Integer userId;
    private LocalDate orderDate;
    private String status;
    private String paymentMethod;
    private String notes;
    private BigDecimal paidAmount;
    private List<OrderDetailDTO> orderDetails;

}