package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {
    private String documentImage;
    private Integer customerId;
    private Integer orderId;
    private Integer dealerId;
}