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

    // Kết hợp: Sử dụng optional=false và thêm @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_id", nullable = false)
    @JsonIgnore
    private Dealer dealer;

    // Kết hợp: Sử dụng optional=false và giữ nguyên
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    // Kết hợp: Giữ nullable=false và thêm helper methods
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // Kết hợp: Giữ enum với tên EVM/FACTORY và DEALER
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_type", nullable = false, length = 20)
    private InventoryType inventoryType;

    public enum InventoryType {
        EVM,        // Kho tổng của hãng (tương đương FACTORY)
        DEALER      // Kho thuộc đại lý
    }

    // 🔥 THÊM CÁC HELPER METHODS
    public boolean isFactoryInventory() {
        return inventoryType == InventoryType.EVM;
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
        return availableQuantity <= 5;
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
        return Inventory.builder()
                .vehicle(vehicle)
                .dealer(null)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .inventoryType(InventoryType.EVM)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    public static Inventory createDealerInventory(Vehicle vehicle, Dealer dealer, Integer initialQuantity) {
        return Inventory.builder()
                .vehicle(vehicle)
                .dealer(dealer)
                .availableQuantity(initialQuantity)
                .reservedQuantity(0)
                .inventoryType(InventoryType.DEALER)
                .lastUpdated(LocalDateTime.now())
                .build();
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