package com.example.demo.dto;

import lombok.Data;

@Data
public class VehicleTypeResponseDTO {
    private Integer id;
    private String typeName;
    private String description;
    private String status;
}
