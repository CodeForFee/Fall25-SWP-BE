package com.example.demo.entity;

import lombok.*;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "VehicleType")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "status", nullable = false)
    private String status; // e.g. ACTIVE, INACTIVE

    // Quan hệ ngược với Vehicle
    @OneToMany(mappedBy = "vehicleType", fetch = FetchType.LAZY)
    private List<Vehicle> vehicles;
}
