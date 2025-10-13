package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class VehicleResponse {
    private Integer id;
    private String modelName;
    private String brand;
    private Integer yearOfManufacture;
    private VehicleTypeResponse vehicleType;
    private Map<String, Object> specifications;
    private String status;
    private BigDecimal batteryCapacity;
    private BigDecimal listedPrice;
    private String versionJson;
    private String availableColorsJson;
}
