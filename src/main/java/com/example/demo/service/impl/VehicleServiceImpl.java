package com.example.demo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.dto.VehicleRequest;
import com.example.demo.dto.VehicleResponse;
import com.example.demo.dto.VehicleTypeResponse;
import com.example.demo.entity.Vehicle;
import com.example.demo.entity.VehicleType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.VehicleRepository;
import com.example.demo.repository.VehicleTypeRepository;
import com.example.demo.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository repo;
    private final VehicleTypeRepository vtRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VehicleResponse create(VehicleRequest req) {
        Vehicle v = Vehicle.builder()
                .modelName(req.getModelName())
                .brand(req.getBrand())
                .yearOfManufacture(req.getYearOfManufacture())
                .status(req.getStatus() == null ? "ACTIVE" : req.getStatus())
                .batteryCapacity(req.getBatteryCapacity())
                .listedPrice(req.getListedPrice())
                .specifications(req.getSpecifications())
                .versionJson(req.getVersionJson())
                .availableColorsJson(req.getAvailableColorsJson())
                .build();

        if (req.getVehicleTypeId() != null) {
            VehicleType vt = vtRepo.findById(req.getVehicleTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("VehicleType not found"));
            v.setVehicleType(vt);
        }
        v = repo.save(v);
        return toDto(v);
    }

    @Override
    public VehicleResponse update(Integer id, VehicleRequest req) {
        Vehicle v = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        v.setModelName(req.getModelName());
        v.setBrand(req.getBrand());
        v.setYearOfManufacture(req.getYearOfManufacture());
        v.setBatteryCapacity(req.getBatteryCapacity());
        v.setListedPrice(req.getListedPrice());
        v.setStatus(req.getStatus());
        v.setSpecifications(req.getSpecifications());
        v.setVersionJson(req.getVersionJson());
        v.setAvailableColorsJson(req.getAvailableColorsJson());
        if (req.getVehicleTypeId() != null) {
            VehicleType vt = vtRepo.findById(req.getVehicleTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("VehicleType not found"));
            v.setVehicleType(vt);
        } else {
            v.setVehicleType(null);
        }
        return toDto(repo.save(v));
    }

    @Override
    public VehicleResponse getById(Integer id) {
        Vehicle v = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return toDto(v);
    }

    @Override
    public List<VehicleResponse> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Vehicle not found");
        repo.deleteById(id);
    }

    @Override
    public VehicleResponse updatePriceAndConfig(Integer id, Double price, String specificationsJson) {
        Vehicle v = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (price != null) v.setListedPrice(price == null ? null : new java.math.BigDecimal(price));
        if (specificationsJson != null) {
            try {
                Map<String,Object> specs = objectMapper.readValue(specificationsJson, Map.class);
                v.setSpecifications(specs);
            } catch (Exception e) {
                throw new RuntimeException("Invalid specifications JSON");
            }
        }
        return toDto(repo.save(v));
    }

    @Override
    public VehicleResponse updateVersionsAndColors(Integer id, String versionJson, String colorsJson) {
        Vehicle v = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (versionJson != null) v.setVersionJson(versionJson);
        if (colorsJson != null) v.setAvailableColorsJson(colorsJson);
        return toDto(repo.save(v));
    }

    private VehicleResponse toDto(Vehicle v) {
        VehicleResponse r = new VehicleResponse();
        r.setId(v.getId());
        r.setModelName(v.getModelName());
        r.setBrand(v.getBrand());
        r.setYearOfManufacture(v.getYearOfManufacture());
        if (v.getVehicleType() != null) {
            VehicleTypeResponse vt = new VehicleTypeResponse();
            vt.setId(v.getVehicleType().getId());
            vt.setTypeName(v.getVehicleType().getTypeName());
            vt.setDescription(v.getVehicleType().getDescription());
            vt.setStatus(v.getVehicleType().getStatus());
            r.setVehicleType(vt);
        }
        r.setSpecifications(v.getSpecifications());
        r.setStatus(v.getStatus());
        r.setBatteryCapacity(v.getBatteryCapacity());
        r.setListedPrice(v.getListedPrice());
        r.setVersionJson(v.getVersionJson());
        r.setAvailableColorsJson(v.getAvailableColorsJson());
        return r;
    }
}
