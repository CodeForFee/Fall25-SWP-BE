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
    public DealerResponseDTO createDealer(DealerDTO dealerDTO) {
        try {
            System.out.println("=== START CREATE DEALER ===");

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
            System.err.println("!!! ERROR IN CREATE DEALER !!!");
            e.printStackTrace();
            throw new RuntimeException("Lỗi server: " + e.getMessage());
        }
    }

    private DealerResponseDTO convertToResponseDTO(Dealer dealer) {
        try {
            DealerResponseDTO dto = new DealerResponseDTO();
            dto.setDealerId(dealer.getDealerId());
            dto.setName(dealer.getName());
            dto.setAddress(dealer.getAddress());
            dto.setPhone(dealer.getPhone());
            dto.setRepresentativeName(dealer.getRepresentativeName());
            dto.setRegion(dealer.getRegion());
            dto.setStatus(dealer.getStatus());
            return dto;
        } catch (Exception e) {
            System.err.println("Error converting to DTO: " + e.getMessage());
            throw e;
        }
    }

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

    @Override
    public DealerResponseDTO updateDealerStatus(Integer dealerId, DealerStatus status) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer không tồn tại với ID: " + dealerId));

        try {
            dealer.setStatus(status);
            Dealer updatedDealer = dealerRepository.save(dealer);
            return convertToResponseDTO(updatedDealer);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @Override
    public List<DealerResponseDTO> getDealersByStatus(DealerStatus status) {
        return dealerRepository.findByStatus(status).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DealerResponseDTO> getDealersByRegion(String region) {
        return dealerRepository.findByRegion(region).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DealerResponseDTO> searchDealersByName(String name) {
        return dealerRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DealerResponseDTO> searchDealersByRepresentative(String representativeName) {
        return dealerRepository.findByRepresentativeNameContaining(representativeName).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
}