package com.example.demo.service;

import com.example.demo.dto.VehicleDTO;
import com.example.demo.dto.VehicleResponseDTO;

import java.util.List;

public interface VehicleService {
    VehicleResponseDTO create(VehicleDTO req);
    VehicleResponseDTO update(Integer id, VehicleDTO req);
    VehicleResponseDTO getById(Integer id);
    List<VehicleResponseDTO> getAll();
    void delete(Integer id);

    // Feature-specific helper endpoints
    VehicleResponseDTO updatePriceAndConfig(Integer id, Double price, String specificationsJson);
    VehicleResponseDTO updateVersionsAndColors(Integer id, String versionJson, String colorsJson);
}
