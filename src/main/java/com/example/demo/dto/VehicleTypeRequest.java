package com.example.demo.dto;

import lombok.Data;

@Data
public class VehicleTypeRequest {
    private String typeName;
    private String description;
    private String status;
}
