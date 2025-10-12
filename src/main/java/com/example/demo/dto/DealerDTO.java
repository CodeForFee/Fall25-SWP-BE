package com.example.demo.dto;

import com.example.demo.entity.DealerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DealerDTO {
    private String name;
    private String address;
    private String phone;
    private String representativeName;
    private String region;
    private DealerStatus status;
}