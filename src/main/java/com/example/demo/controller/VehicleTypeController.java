package com.example.demo.controller;

import com.example.demo.dto.VehicleTypeDTO;
import com.example.demo.dto.VehicleTypeResponseDTO;
import com.example.demo.service.VehicleTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vehicle-types")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Vehicle Type Management", description = "APIs for vehicle type management")
@SecurityRequirement(name = "bearer-jwt")
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    @Operation(summary = "Lấy tất cả vehicle types")
    public ResponseEntity<List<VehicleTypeResponseDTO>> getAllVehicleTypes() {
        return ResponseEntity.ok(vehicleTypeService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy vehicle type theo ID")
    public ResponseEntity<VehicleTypeResponseDTO> getVehicleTypeById(@PathVariable Integer id) {
        return ResponseEntity.ok(vehicleTypeService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo vehicle type mới")
    public ResponseEntity<VehicleTypeResponseDTO> createVehicleType(@RequestBody VehicleTypeDTO req) {
        return ResponseEntity.ok(vehicleTypeService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật vehicle type")
    public ResponseEntity<VehicleTypeResponseDTO> updateVehicleType(@PathVariable Integer id, @RequestBody VehicleTypeDTO req) {
        return ResponseEntity.ok(vehicleTypeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa vehicle type")
    public ResponseEntity<String> deleteVehicleType(@PathVariable Integer id) {
        vehicleTypeService.delete(id);
        return ResponseEntity.ok("Vehicle type deleted successfully");
    }
}
