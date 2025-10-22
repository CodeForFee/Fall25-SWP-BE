package com.example.demo.service.impl;

import com.example.demo.dto.ContractDTO;
import com.example.demo.dto.ContractResponseDTO;
import com.example.demo.entity.Contract;
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
    public ContractResponseDTO getContractById(Integer id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + id));
        return convertToResponseDTO(contract);
    }

    @Override
    public ContractResponseDTO createContract(ContractDTO contractDTO) {
        try {
            log.info("=== START CREATE CONTRACT ===");

            if (contractRepository.existsByContractNumber(contractDTO.getContractNumber())) {
                throw new RuntimeException("Số hợp đồng đã tồn tại");
            }

            Contract contract = new Contract();
            contract.setOrderId(contractDTO.getOrderId());
            contract.setVin(contractDTO.getVin());
            contract.setContractNumber(contractDTO.getContractNumber());
            contract.setSignedDate(contractDTO.getSignedDate());
            contract.setCustomerSignature(contractDTO.getCustomerSignature());
            contract.setDealerRepresentative(contractDTO.getDealerRepresentative());

            Contract savedContract = contractRepository.save(contract);
            log.info("=== CONTRACT CREATED SUCCESSFULLY ===");
            return convertToResponseDTO(savedContract);

        } catch (Exception e) {
            log.error("!!! ERROR IN CREATE CONTRACT !!!", e);
            throw new RuntimeException("Lỗi server khi tạo hợp đồng: " + e.getMessage());
        }
    }

    @Override
    public ContractResponseDTO updateContract(Integer id, ContractDTO contractDTO) {
        Contract existingContract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + id));


        if (!existingContract.getContractNumber().equals(contractDTO.getContractNumber()) &&
                contractRepository.existsByContractNumber(contractDTO.getContractNumber())) {
            throw new RuntimeException("Số hợp đồng đã tồn tại");
        }


        existingContract.setOrderId(contractDTO.getOrderId());
        existingContract.setVin(contractDTO.getVin());
        existingContract.setContractNumber(contractDTO.getContractNumber());
        existingContract.setSignedDate(contractDTO.getSignedDate());
        existingContract.setCustomerSignature(contractDTO.getCustomerSignature());
        existingContract.setDealerRepresentative(contractDTO.getDealerRepresentative());

        Contract updatedContract = contractRepository.save(existingContract);
        log.info("=== CONTRACT UPDATED SUCCESSFULLY ===");
        return convertToResponseDTO(updatedContract);
    }

    @Override
    public void deleteContract(Integer id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + id));
        contractRepository.delete(contract);
        log.info("=== CONTRACT DELETED SUCCESSFULLY ===");
    }

    private ContractResponseDTO convertToResponseDTO(Contract contract) {
        ContractResponseDTO dto = new ContractResponseDTO();
        dto.setId(contract.getId());
        dto.setOrderId(contract.getOrderId());
        dto.setVin(contract.getVin());
        dto.setContractNumber(contract.getContractNumber());
        dto.setSignedDate(contract.getSignedDate());
        dto.setCustomerSignature(contract.getCustomerSignature());
        dto.setDealerRepresentative(contract.getDealerRepresentative());
        return dto;
    }
}