package com.example.demo.service;

import com.example.demo.dto.VehicleRequest;
import com.example.demo.dto.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse create(VehicleRequest req);
    VehicleResponse update(Integer id, VehicleRequest req);
    VehicleResponse getById(Integer id);
    List<VehicleResponse> getAll();
    void delete(Integer id);

    // Feature-specific helper endpoints
    VehicleResponse updatePriceAndConfig(Integer id, Double price, String specificationsJson);
    VehicleResponse updateVersionsAndColors(Integer id, String versionJson, String colorsJson);
}
