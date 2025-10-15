package com.example.demo.service;

import com.example.demo.dto.DealerDTO;
import com.example.demo.dto.DealerResponseDTO;
import java.util.List;

public interface DealerService {
    List<DealerResponseDTO> getAllDealers();
    DealerResponseDTO getDealerById(Integer dealerId);
    DealerResponseDTO createDealer(DealerDTO dealerDTO);
    DealerResponseDTO updateDealer(Integer dealerId, DealerDTO dealerDTO);
    void deleteDealer(Integer dealerId);
}