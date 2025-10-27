package com.example.demo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class VehicleDTO {
    @NotBlank
    private String modelName;
    @NotBlank
    private String brand;
    private Integer yearOfManufacture;
    private Integer vehicleTypeId;
    private Map<String, Object> specifications; // key-values
    private String status;
    private BigDecimal batteryCapacity;
    private BigDecimal listedPrice;
    private String versionJson; // JSON string for versions
    private String availableColorsJson; // JSON string array
}
