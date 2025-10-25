package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerDTO {
    private String fullName;
    private String phone;
    private String email;
    private String citizenId;
    private Integer dealerId;
}