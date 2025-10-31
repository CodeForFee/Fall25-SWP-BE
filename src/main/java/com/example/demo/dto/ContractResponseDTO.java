package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponseDTO {
    private Integer id;
    private Integer orderId;
    private LocalDate signedDate;
    private String customerSignature;
    private String dealerRepresentative;
}