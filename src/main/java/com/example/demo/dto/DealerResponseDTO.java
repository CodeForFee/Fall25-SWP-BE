package com.example.demo.dto;

import com.example.demo.entity.Dealer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DealerResponseDTO {
    private Integer dealerId;
    private String name;
    private String address;
    private String phone;
    private String representativeName;
    private String region;
    private Dealer.DealerStatus status;
}