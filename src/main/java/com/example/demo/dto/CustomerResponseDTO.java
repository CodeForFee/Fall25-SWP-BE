package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerResponseDTO {
    private Integer id;
    private String fullName;
    private String phone;
    private String email;
    private String citizenId;
    private Integer dealerId;
    private String dealerName;
}