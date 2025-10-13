package com.example.demo.dto;

import lombok.Data;

@Data
public class VehicleTypeResponse {
    private Integer id;
    private String typeName;
    private String description;
    private String status;
}
