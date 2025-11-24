package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class InventoryGroupResponseDTO {
    private String modelName;
    private String brand;
    private Integer yearOfManufacture;
    private BigDecimal listedPrice;
    private BigDecimal  batteryCapacity;
    private String status;
    private Map<String, Object> specifications;
    private String versionJson;
    private String availableColorsJson;
    private String vehicleType;

    private Integer totalAvailableQuantity;
    private Integer totalReservedQuantity;
    private String inventoryType;
    private LocalDateTime lastUpdated;

    private List<VehicleInventoryDetailDTO> vehicles;
}