package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
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

    // Liên kết tới Dealer
    @ManyToOne(optional = false)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    // Liên kết tới Vehicle
    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * Loại kho — dùng để phân biệt giữa kho của hãng (EVM) và kho của đại lý (DEALER)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_type", nullable = false, length = 20)
    private InventoryType inventoryType;

    public enum InventoryType {
        EVM,        // Kho tổng của hãng
        DEALER      // Kho thuộc đại lý
    }
}
