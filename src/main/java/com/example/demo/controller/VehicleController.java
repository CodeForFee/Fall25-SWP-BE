package com.example.demo.controller;

import com.example.demo.dto.VehicleRequest;
import com.example.demo.dto.VehicleResponse;
import com.example.demo.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService service;

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(@PathVariable Integer id, @RequestBody VehicleRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Update price and specifications
    @PatchMapping("/{id}/price-config")
    public ResponseEntity<VehicleResponse> updatePriceAndConfig(
            @PathVariable Integer id,
            @RequestParam(required = false) Double price,
            @RequestBody(required = false) String specificationsJson
    ) {
        return ResponseEntity.ok(service.updatePriceAndConfig(id, price, specificationsJson));
    }

    // Update versions and colors (client sends JSON body with 'versionJson' and/or 'colorsJson' plain text)
    @PatchMapping("/{id}/versions-colors")
    public ResponseEntity<VehicleResponse> updateVersionsAndColors(
            @PathVariable Integer id,
            @RequestBody String bodyJson
    ) {
        // for simplicity, client sends raw JSON like {"versionJson": "...", "colorsJson":"[...]"}
        // controller forwards strings to service
        // parse minimal fields
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = m.readValue(bodyJson, java.util.Map.class);
            String versionJson = map.get("versionJson") == null ? null : map.get("versionJson").toString();
            String colorsJson = map.get("colorsJson") == null ? null : map.get("colorsJson").toString();
            return ResponseEntity.ok(service.updateVersionsAndColors(id, versionJson, colorsJson));
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON body");
        }
    }
}
