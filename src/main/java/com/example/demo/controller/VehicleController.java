package com.example.demo.controller;

import com.example.demo.dto.VehicleDTO;
import com.example.demo.dto.VehicleResponseDTO;
import com.example.demo.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Vehicle Management", description = "Public APIs for vehicle management and configuration")
public class VehicleController {

    private final VehicleService vehicleService;

    // Lấy tất cả vehicles
    @GetMapping
    @Operation(summary = "Lấy tất cả vehicles (public)")
    public ResponseEntity<List<VehicleResponseDTO>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAll());
    }

    // Lấy vehicle theo ID
    @GetMapping("/{id}")
    @Operation(summary = "Lấy vehicle theo ID (public)")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Integer id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    // Tạo vehicle mới
    @PostMapping
    @Operation(summary = "Tạo vehicle mới (public)")
    public ResponseEntity<VehicleResponseDTO> createVehicle(@Valid @RequestBody VehicleDTO vehicleDTO) {
        return ResponseEntity.ok(vehicleService.create(vehicleDTO));
    }

    // Cập nhật vehicle
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật vehicle (public)")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(@PathVariable Integer id, @RequestBody VehicleDTO vehicleDTO) {
        return ResponseEntity.ok(vehicleService.update(id, vehicleDTO));
    }

    // Xóa vehicle
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa vehicle (public)")
    public ResponseEntity<String> deleteVehicle(@PathVariable Integer id) {
        vehicleService.delete(id);
        return ResponseEntity.ok("Vehicle deleted successfully");
    }

    // Cập nhật giá và thông số kỹ thuật
    @PatchMapping("/{id}/price-config")
    @Operation(summary = "Cập nhật giá và thông số kỹ thuật của vehicle (public)")
    public ResponseEntity<VehicleResponseDTO> updatePriceAndConfig(
            @PathVariable Integer id,
            @RequestParam(required = false) Double price,
            @RequestBody(required = false) String specificationsJson
    ) {
        return ResponseEntity.ok(vehicleService.updatePriceAndConfig(id, price, specificationsJson));
    }

    // Cập nhật phiên bản và màu sắc
    @PatchMapping("/{id}/versions-colors")
    @Operation(summary = "Cập nhật phiên bản và màu sắc của vehicle (public)")
    public ResponseEntity<VehicleResponseDTO> updateVersionsAndColors(
            @PathVariable Integer id,
            @RequestBody String bodyJson
    ) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(bodyJson, java.util.Map.class);
            String versionJson = map.get("versionJson") == null ? null : map.get("versionJson").toString();
            String colorsJson = map.get("colorsJson") == null ? null : map.get("colorsJson").toString();
            return ResponseEntity.ok(vehicleService.updateVersionsAndColors(id, versionJson, colorsJson));
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON body");
        }
    }
}
