package com.example.demo.service;

import com.example.demo.dto.DealerDTO;
import com.example.demo.dto.DealerResponseDTO;
import com.example.demo.entity.DealerStatus;

import java.util.List;

public interface DealerService {
    DealerResponseDTO createDealer(DealerDTO dealerDTO);
    List<DealerResponseDTO> getAllDealers();
    DealerResponseDTO getDealerById(Integer dealerId);
    DealerResponseDTO updateDealer(Integer dealerId, DealerDTO dealerDTO);
    void deleteDealer(Integer dealerId);

    DealerResponseDTO updateDealerStatus(Integer dealerId, DealerStatus status);
    List<DealerResponseDTO> getDealersByStatus(DealerStatus status);
    List<DealerResponseDTO> getDealersByRegion(String region);
    List<DealerResponseDTO> searchDealersByName(String name);
    List<DealerResponseDTO> searchDealersByRepresentative(String representativeName);
}