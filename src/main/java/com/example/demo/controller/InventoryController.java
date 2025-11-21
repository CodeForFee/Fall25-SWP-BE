package com.example.demo.controller;

import com.example.demo.entity.Inventory;
import com.example.demo.service.InventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class InventoryController {

    private final InventoryService inventoryService;

    // Tạo kho hãng
    @PostMapping("/factory")
    public ResponseEntity<Map<String, Object>> createFactoryInventory(
            @RequestParam Integer vehicleId,
            @RequestParam Integer quantity) {
        Inventory inventory = inventoryService.createFactoryInventory(vehicleId, quantity);
        return ResponseEntity.ok(convertInventoryToMap(inventory));
    }


    @PostMapping("/dealer")
    public ResponseEntity<Map<String, Object>> createDealerInventory(
            @RequestParam Integer dealerId,
            @RequestParam Integer vehicleId) {
        Inventory inventory = inventoryService.createDealerInventory(dealerId, vehicleId, 1);
        return ResponseEntity.ok(convertInventoryToMap(inventory));
    }

    // Lấy tồn kho hãng
    @GetMapping("/factory")
    public ResponseEntity<List<Map<String, Object>>> getFactoryInventory() {
        List<Inventory> inventory = inventoryService.getFactoryInventory();
        List<Map<String, Object>> response = inventory.stream()
                .map(this::convertInventoryToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Lấy tồn kho đại lý
    @GetMapping("/dealer/{dealerId}")
    public ResponseEntity<List<Map<String, Object>>> getDealerInventory(@PathVariable Integer dealerId) {
        List<Inventory> inventory = inventoryService.getDealerInventory(dealerId);
        List<Map<String, Object>> response = inventory.stream()
                .map(this::convertInventoryToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Kiểm tra tồn kho hãng
    @GetMapping("/factory/check")
    public ResponseEntity<Boolean> checkFactoryInventory(
            @RequestParam Integer vehicleId,
            @RequestParam Integer quantity) {
        boolean available = inventoryService.checkFactoryInventory(vehicleId, quantity);
        return ResponseEntity.ok(available);
    }


    private Map<String, Object> convertInventoryToMap(Inventory inventory) {
        Map<String, Object> result = new HashMap<>();

        // Basic inventory info
        result.put("id", inventory.getId());
        result.put("availableQuantity", inventory.getAvailableQuantity());
        result.put("reservedQuantity", inventory.getReservedQuantity());
        result.put("inventoryType", inventory.getInventoryType());
        result.put("lastUpdated", inventory.getLastUpdated());

        if (inventory.getVehicle() != null) {
            Map<String, Object> vehicleMap = new HashMap<>();
            vehicleMap.put("id", inventory.getVehicle().getId());
            vehicleMap.put("modelName", inventory.getVehicle().getModelName());
            vehicleMap.put("brand", inventory.getVehicle().getBrand());
            vehicleMap.put("yearOfManufacture", inventory.getVehicle().getYearOfManufacture());
            vehicleMap.put("status", inventory.getVehicle().getStatus());
            vehicleMap.put("listedPrice", inventory.getVehicle().getListedPrice());
            vehicleMap.put("batteryCapacity", inventory.getVehicle().getBatteryCapacity());
            result.put("vehicle", vehicleMap);
        }

        if (inventory.getDealer() != null) {
            Map<String, Object> dealerMap = new HashMap<>();
            dealerMap.put("dealerId", inventory.getDealer().getDealerId());
            dealerMap.put("name", inventory.getDealer().getName());
            dealerMap.put("region", inventory.getDealer().getRegion());
            dealerMap.put("status", inventory.getDealer().getStatus());
            result.put("dealer", dealerMap);
        }

        return result;
    }
}