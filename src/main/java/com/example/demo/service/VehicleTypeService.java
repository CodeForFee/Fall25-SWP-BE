package com.example.demo.service;

import com.example.demo.dto.VehicleTypeDTO;

import com.example.demo.dto.VehicleTypeResponseDTO;

import java.util.List;

public interface VehicleTypeService {
    VehicleTypeResponseDTO create(VehicleTypeDTO req);
    VehicleTypeResponseDTO update(Integer id, VehicleTypeDTO req);
    VehicleTypeResponseDTO getById(Integer id);
    List<VehicleTypeResponseDTO> getAll();
    void delete(Integer id);
}
