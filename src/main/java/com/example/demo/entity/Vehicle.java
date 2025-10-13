package com.example.demo.entity;

import com.example.demo.config.JsonConverter;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "Vehicle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "year_of_manufacture")
    private Integer yearOfManufacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @Convert(converter = JsonConverter.class)
    @Column(name = "specifications", columnDefinition = "NVARCHAR(MAX)")
    private Map<String, Object> specifications;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "battery_capacity", precision = 18, scale = 2)
    private BigDecimal batteryCapacity;

    @Column(name = "listed_price", precision = 18, scale = 2)
    private BigDecimal listedPrice;

    @Column(name = "version", columnDefinition = "NVARCHAR(MAX)")
    private String versionJson; // JSON text for versions / trims

    @Column(name = "available_colors", columnDefinition = "NVARCHAR(MAX)")
    private String availableColorsJson; // JSON array text
}
