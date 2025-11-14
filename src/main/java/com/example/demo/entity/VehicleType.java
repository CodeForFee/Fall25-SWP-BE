package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    private String status;


    @OneToMany(mappedBy = "vehicleType", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Vehicle> vehicles;

    // Helper methods
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public boolean hasDescription() {
        return this.description != null && !this.description.trim().isEmpty();
    }

    public boolean hasVehicles() {
        return this.vehicles != null && !this.vehicles.isEmpty();
    }

    public long getActiveVehiclesCount() {
        return this.vehicles != null ?
                this.vehicles.stream().filter(Vehicle::isActive).count() : 0;
    }

    // Business validation
    public boolean isValid() {
        return this.typeName != null && !this.typeName.trim().isEmpty() &&
                this.status != null && !this.status.trim().isEmpty();
    }

    // Static factory method
    public static VehicleType createVehicleType(String typeName, String description) {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setTypeName(typeName);
        vehicleType.setDescription(description);
        vehicleType.setStatus("ACTIVE");
        return vehicleType;
    }

    public void deactivate() {
        this.status = "INACTIVE";
        // Deactivate all associated vehicles
        if (this.vehicles != null) {
            this.vehicles.forEach(Vehicle::deactivate);
        }
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public boolean canBeDeleted() {
        return this.vehicles == null || this.vehicles.isEmpty() ||
                this.vehicles.stream().noneMatch(Vehicle::isActive);
    }

    @Override
    public String toString() {
        return "VehicleType{" +
                "id=" + id +
                ", typeName='" + typeName + '\'' +
                ", status='" + status + '\'' +
                ", vehiclesCount=" + (vehicles != null ? vehicles.size() : 0) +
                '}';
    }
}