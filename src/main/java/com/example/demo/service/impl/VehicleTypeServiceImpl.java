package com.example.demo.service.impl;

import com.example.demo.dto.VehicleTypeDTO;
import com.example.demo.dto.VehicleTypeResponseDTO;
import com.example.demo.entity.VehicleType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.VehicleTypeRepository;
import com.example.demo.service.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository repo;

    @Override
    public VehicleTypeResponseDTO create(VehicleTypeDTO req) {
        VehicleType vt = VehicleType.builder()
                .typeName(req.getTypeName())
                .description(req.getDescription())
                .status(req.getStatus())
                .build();
        vt = repo.save(vt);
        return toDto(vt);
    }

    @Override
    public VehicleTypeResponseDTO update(Integer id, VehicleTypeDTO req) {
        VehicleType vt = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("VehicleType not found"));
        vt.setTypeName(req.getTypeName());
        vt.setDescription(req.getDescription());
        vt.setStatus(req.getStatus());
        return toDto(repo.save(vt));
    }

    @Override
    public VehicleTypeResponseDTO getById(Integer id) {
        VehicleType vt = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("VehicleType not found"));
        return toDto(vt);
    }

    @Override
    public List<VehicleTypeResponseDTO> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("VehicleType not found");
        repo.deleteById(id);
    }

    private VehicleTypeResponseDTO toDto(VehicleType vt) {
        VehicleTypeResponseDTO r = new VehicleTypeResponseDTO();
        r.setId(vt.getId());
        r.setTypeName(vt.getTypeName());
        r.setDescription(vt.getDescription());
        r.setStatus(vt.getStatus());
        return r;
    }
}
