package com.example.demo.controller;

import com.example.demo.dto.VehicleRequest;
import com.example.demo.dto.VehicleResponse;
import com.example.demo.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Vehicle Management", description = "APIs for vehicle management and configuration")
@SecurityRequirement(name = "bearer-jwt")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Lấy tất cả vehicles")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy vehicle theo ID")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Integer id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo vehicle mới")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest vehicleRequest) {
        return ResponseEntity.ok(vehicleService.create(vehicleRequest));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật vehicle")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Integer id, @RequestBody VehicleRequest vehicleRequest) {
        return ResponseEntity.ok(vehicleService.update(id, vehicleRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa vehicle")
    public ResponseEntity<String> deleteVehicle(@PathVariable Integer id) {
        vehicleService.delete(id);
        return ResponseEntity.ok("Vehicle deleted successfully");
    }

    @PatchMapping("/{id}/price-config")
    @Operation(summary = "Cập nhật giá và thông số kỹ thuật của vehicle")
    public ResponseEntity<VehicleResponse> updatePriceAndConfig(
            @PathVariable Integer id,
            @RequestParam(required = false) Double price,
            @RequestBody(required = false) String specificationsJson
    ) {
        return ResponseEntity.ok(vehicleService.updatePriceAndConfig(id, price, specificationsJson));
    }

    @PatchMapping("/{id}/versions-colors")
    @Operation(summary = "Cập nhật phiên bản và màu sắc của vehicle")
    public ResponseEntity<VehicleResponse> updateVersionsAndColors(
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
