package com.example.demo.repository;

import com.example.demo.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByStatus(String status);
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'AVAILABLE' AND EXISTS (" +
            "SELECT 1 FROM Inventory i WHERE i.vehicle.id = v.id AND i.availableQuantity > 0) " +
            "ORDER BY v.id ASC LIMIT 1")
    Optional<Vehicle> findFirstAvailableWithInventory();
}