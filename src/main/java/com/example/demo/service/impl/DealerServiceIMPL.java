
package com.example.demo.service.impl;

import com.example.demo.dto.DealerDTO;
import com.example.demo.dto.DealerResponseDTO;
import com.example.demo.entity.Dealer;
import com.example.demo.entity.DealerStatus;
import com.example.demo.repository.DealerRepository;
import com.example.demo.service.DealerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealerServiceIMPL implements DealerService {

    private final DealerRepository dealerRepository;

    @Override
    public List<DealerResponseDTO> getAllDealers() {
        return dealerRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DealerResponseDTO getDealerById(Integer dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer Not Found"));
        return convertToResponseDTO(dealer);
    }

    @Override
    public DealerResponseDTO createDealer(DealerDTO dealerDTO) {
        try {
            if (dealerRepository.existsByName(dealerDTO.getName())) {
                throw new RuntimeException("Tên đại lý đã tồn tại");
            }
            if (dealerRepository.existsByPhone(dealerDTO.getPhone())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }

            Dealer dealer = new Dealer();
            dealer.setName(dealerDTO.getName());
            dealer.setAddress(dealerDTO.getAddress());
            dealer.setPhone(dealerDTO.getPhone());
            dealer.setRepresentativeName(dealerDTO.getRepresentativeName());
            dealer.setRegion(dealerDTO.getRegion());
            dealer.setStatus(dealerDTO.getStatus() != null ? dealerDTO.getStatus() : DealerStatus.ACTIVE);

            Dealer savedDealer = dealerRepository.save(dealer);
            return convertToResponseDTO(savedDealer);

        } catch (Exception e) {
            log.error("!!! ERROR IN CREATE DEALER !!!", e);
            throw new RuntimeException("Lỗi server: " + e.getMessage());
        }
    }

    @Override
    public DealerResponseDTO updateDealer(Integer dealerId, DealerDTO dealerDTO) {
        Dealer existingDealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer Not Found"));

        // Kiểm tra trùng tên
        if (dealerDTO.getName() != null && !dealerDTO.getName().equals(existingDealer.getName())) {
            if (dealerRepository.existsByName(dealerDTO.getName())) {
                throw new RuntimeException("Tên đại lý đã tồn tại");
            }
            existingDealer.setName(dealerDTO.getName());
        }

        // Kiểm tra trùng số điện thoại
        if (dealerDTO.getPhone() != null && !dealerDTO.getPhone().equals(existingDealer.getPhone())) {
            if (dealerRepository.existsByPhone(dealerDTO.getPhone())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
            existingDealer.setPhone(dealerDTO.getPhone());
        }

        if (dealerDTO.getAddress() != null) {
            existingDealer.setAddress(dealerDTO.getAddress());
        }
        if (dealerDTO.getRepresentativeName() != null) {
            existingDealer.setRepresentativeName(dealerDTO.getRepresentativeName());
        }
        if (dealerDTO.getRegion() != null) {
            existingDealer.setRegion(dealerDTO.getRegion());
        }
        if (dealerDTO.getStatus() != null) {
            existingDealer.setStatus(dealerDTO.getStatus());
        }

        Dealer updatedDealer = dealerRepository.save(existingDealer);
        return convertToResponseDTO(updatedDealer);
    }

    @Override
    public void deleteDealer(Integer dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer Not Found"));
        dealerRepository.delete(dealer);
    }

    private DealerResponseDTO convertToResponseDTO(Dealer dealer) {
        DealerResponseDTO dto = new DealerResponseDTO();
        dto.setDealerId(dealer.getDealerId());
        dto.setName(dealer.getName());
        dto.setAddress(dealer.getAddress());
        dto.setPhone(dealer.getPhone());
        dto.setRepresentativeName(dealer.getRepresentativeName());
        dto.setRegion(dealer.getRegion());
        dto.setStatus(dealer.getStatus());
        return dto;
    }
}