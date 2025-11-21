package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class VehicleResponseDTO {
    private Integer id;
    private String modelName;
    private String brand;
    private Integer yearOfManufacture;
    private VehicleTypeResponseDTO vehicleType;
    private String vin;
    private String engineNumber;
    private Map<String, Object> specifications;
    private String status;
    private BigDecimal batteryCapacity;
    private BigDecimal listedPrice;
    private String versionJson;
    private String availableColorsJson;
}