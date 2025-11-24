package com.example.demo.dto;

import lombok.Data;

@Data
public class VehicleInventoryDetailDTO {
    private Integer vehicleId;
    private String vin;
    private String engineNumber;
    private String status;
    private Integer inventoryId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
}