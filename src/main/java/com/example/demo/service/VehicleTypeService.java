package com.example.demo.service;

import com.example.demo.dto.VehicleTypeRequest;
import com.example.demo.dto.VehicleTypeResponse;

import java.util.List;

public interface VehicleTypeService {
    VehicleTypeResponse create(VehicleTypeRequest req);
    VehicleTypeResponse update(Integer id, VehicleTypeRequest req);
    VehicleTypeResponse getById(Integer id);
    List<VehicleTypeResponse> getAll();
    void delete(Integer id);
}
