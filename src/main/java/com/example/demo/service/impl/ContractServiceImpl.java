package com.example.demo.service.impl;

import com.example.demo.dto.ContractDTO;
import com.example.demo.dto.ContractResponseDTO;
import com.example.demo.entity.Contract;
import com.example.demo.entity.Order;
import com.example.demo.repository.ContractRepository;
import com.example.demo.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;

    @Override
    public List<ContractResponseDTO> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContractResponseDTO> getContractsByDealer(Integer dealerId) {
        return contractRepository.findByDealerId(dealerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContractResponseDTO> searchContractsByCustomerName(String customerName) {
        return contractRepository.findByCustomerNameContaining(customerName).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Order getOrderByContractId(Integer contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));

        if (contract.getOrder() == null) {
            throw new RuntimeException("Hợp đồng này không có order liên kết");
        }

        return contract.getOrder();
    }

    @Override
    public ContractResponseDTO createContract(ContractDTO contractDTO) {
        try {
            log.debug("Creating contract");

            Contract contract = new Contract();
            contract.setDocumentImage(contractDTO.getDocumentImage());
            contract.setCustomerId(contractDTO.getCustomerId());
            contract.setOrderId(contractDTO.getOrderId());
            contract.setDealerId(contractDTO.getDealerId());

            Contract savedContract = contractRepository.save(contract);
            log.debug("Contract created successfully");
            return convertToResponseDTO(savedContract);

        } catch (Exception e) {
            log.error("Error creating contract: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi server khi tạo hợp đồng: " + e.getMessage());
        }
    }

    private ContractResponseDTO convertToResponseDTO(Contract contract) {
        ContractResponseDTO dto = new ContractResponseDTO();
        dto.setId(contract.getId());
        dto.setDocumentImage(contract.getDocumentImage());
        dto.setCustomerId(contract.getCustomerId());
        dto.setOrderId(contract.getOrderId());
        dto.setDealerId(contract.getDealerId());
        if (contract.getCustomer() != null) {
            dto.setCustomerName(contract.getCustomer().getFullName());
        }
        if (contract.getDealer() != null) {
            dto.setDealerName(contract.getDealer().getName());
        }

        return dto;
    }
}