package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id")
    @JsonIgnore
    private Dealer dealer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_type")
    private InventoryType inventoryType;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Enum phân loại kho
    public enum InventoryType {
        FACTORY,    // Kho hãng
        DEALER      // Kho đại lý
    }

    // Helper methods
    public boolean isFactoryInventory() {
        return inventoryType == InventoryType.FACTORY;
    }

    public boolean isDealerInventory() {
        return inventoryType == InventoryType.DEALER;
    }

    public boolean hasSufficientQuantity(Integer requiredQuantity) {
        return availableQuantity >= requiredQuantity;
    }

    public Integer getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }

    public boolean isOutOfStock() {
        return availableQuantity <= 0;
    }

    public boolean isLowStock() {
        return availableQuantity <= 5; // Ngưỡng low stock
    }

    // Business operations
    public void reserveQuantity(Integer quantity) {
        if (quantity != null && quantity > 0 && this.hasSufficientQuantity(quantity)) {
            this.availableQuantity -= quantity;
            this.reservedQuantity += quantity;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public void releaseReservedQuantity(Integer quantity) {
        if (quantity != null && quantity > 0 && this.reservedQuantity >= quantity) {
            this.availableQuantity += quantity;
            this.reservedQuantity -= quantity;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public void addStock(Integer quantity) {
        if (quantity != null && quantity > 0) {
            this.availableQuantity += quantity;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public void deductStock(Integer quantity) {
        if (quantity != null && quantity > 0 && this.hasSufficientQuantity(quantity)) {
            this.availableQuantity -= quantity;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    // Static factory methods
    public static Inventory createFactoryInventory(Vehicle vehicle, Integer initialQuantity) {
        Inventory inventory = new Inventory();
        inventory.setVehicle(vehicle);
        inventory.setDealer(null); // Factory inventory has no dealer
        inventory.setAvailableQuantity(initialQuantity);
        inventory.setReservedQuantity(0);
        inventory.setInventoryType(InventoryType.FACTORY);
        inventory.setLastUpdated(LocalDateTime.now());
        return inventory;
    }

    public static Inventory createDealerInventory(Vehicle vehicle, Dealer dealer, Integer initialQuantity) {
        Inventory inventory = new Inventory();
        inventory.setVehicle(vehicle);
        inventory.setDealer(dealer);
        inventory.setAvailableQuantity(initialQuantity);
        inventory.setReservedQuantity(0);
        inventory.setInventoryType(InventoryType.DEALER);
        inventory.setLastUpdated(LocalDateTime.now());
        return inventory;
    }

    // Validation
    public boolean isValid() {
        return this.vehicle != null &&
                this.availableQuantity != null && this.availableQuantity >= 0 &&
                this.reservedQuantity != null && this.reservedQuantity >= 0 &&
                this.inventoryType != null;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", vehicleId=" + (vehicle != null ? vehicle.getId() : "null") +
                ", dealerId=" + (dealer != null ? dealer.getDealerId() : "null") +
                ", availableQuantity=" + availableQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", inventoryType=" + inventoryType +
                '}';
    }
}