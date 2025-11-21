package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

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
    @JsonIgnore
    private VehicleType vehicleType;

    @Column(name = "vin", unique = true, nullable = false, length = 7)
    private String vin;

    @Column(name = "engine_number", unique = true, nullable = false, length = 7)
    private String engineNumber;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "battery_capacity", precision = 18, scale = 2)
    private BigDecimal batteryCapacity;

    @Column(name = "listed_price", precision = 18, scale = 2)
    private BigDecimal listedPrice;

    @Column(name = "version", columnDefinition = "TEXT")
    private String versionJson;

    @Column(name = "available_colors", columnDefinition = "TEXT")
    private String availableColorsJson;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<QuoteDetail> quoteDetails;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Inventory> inventories;

    @PrePersist
    public void generateUniqueNumbers() {
        if (this.vin == null) {
            this.vin = generateRandomAlphanumeric(7);
        }
        if (this.engineNumber == null) {
            this.engineNumber = generateRandomAlphanumeric(7);
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    private String generateRandomAlphanumeric(int length) {
        String alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * alphanumeric.length());
            sb.append(alphanumeric.charAt(index));
        }
        return sb.toString();
    }

    // Helper methods
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public boolean isAvailable() {
        return isActive() && this.listedPrice != null && this.listedPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasSpecifications() {
        return this.specifications != null && !this.specifications.trim().isEmpty();
    }

    public boolean hasVersions() {
        return this.versionJson != null && !this.versionJson.trim().isEmpty();
    }

    public boolean hasColors() {
        return this.availableColorsJson != null && !this.availableColorsJson.trim().isEmpty();
    }

    // Business validation
    public boolean isValidForSale() {
        return this.isActive() &&
                this.modelName != null && !this.modelName.trim().isEmpty() &&
                this.brand != null && !this.brand.trim().isEmpty() &&
                this.listedPrice != null && this.listedPrice.compareTo(BigDecimal.ZERO) > 0 &&
                this.vin != null && !this.vin.trim().isEmpty() &&
                this.engineNumber != null && !this.engineNumber.trim().isEmpty();
    }

    // Static factory method
    public static Vehicle createVehicle(String modelName, String brand, Integer yearOfManufacture,
                                        VehicleType vehicleType, BigDecimal listedPrice) {
        Vehicle vehicle = new Vehicle();
        vehicle.setModelName(modelName);
        vehicle.setBrand(brand);
        vehicle.setYearOfManufacture(yearOfManufacture);
        vehicle.setVehicleType(vehicleType);
        vehicle.setListedPrice(listedPrice);
        vehicle.setStatus("ACTIVE");
        vehicle.setBatteryCapacity(BigDecimal.ZERO);
        return vehicle;
    }

    public void deactivate() {
        this.status = "INACTIVE";
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.listedPrice = newPrice;
        }
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", modelName='" + modelName + '\'' +
                ", brand='" + brand + '\'' +
                ", yearOfManufacture=" + yearOfManufacture +
                ", vin='" + vin + '\'' +
                ", engineNumber='" + engineNumber + '\'' +
                ", status='" + status + '\'' +
                ", listedPrice=" + listedPrice +
                '}';
    }
}