package com.example.demo.dto;

import com.example.demo.entity.Inventory;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryResponseDTO {
    private Integer id;
    private Integer vehicleId;
    private String vehicleModelName;
    private String vehicleBrand;
    private Integer dealerId;
    private String dealerName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Inventory.InventoryType inventoryType;
    private LocalDateTime lastUpdated;

    public static InventoryResponseDTO fromEntity(Inventory inventory) {
        InventoryResponseDTO dto = new InventoryResponseDTO();
        dto.setId(inventory.getId());
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        dto.setReservedQuantity(inventory.getReservedQuantity());
        dto.setInventoryType(inventory.getInventoryType());
        dto.setLastUpdated(inventory.getLastUpdated());

        // Vehicle info
        if (inventory.getVehicle() != null) {
            dto.setVehicleId(inventory.getVehicle().getId());
            dto.setVehicleModelName(inventory.getVehicle().getModelName());
            dto.setVehicleBrand(inventory.getVehicle().getBrand());
        }

        // Dealer info (chỉ cho dealer inventory)
        if (inventory.getDealer() != null) {
            dto.setDealerId(inventory.getDealer().getDealerId());
            dto.setDealerName(inventory.getDealer().getName());
        }

        return dto;
    }
}