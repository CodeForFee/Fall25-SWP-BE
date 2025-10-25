package com.example.demo.service;

import com.example.demo.dto.ContractDTO;
import com.example.demo.dto.ContractResponseDTO;

import java.util.List;

public interface ContractService {
    List<ContractResponseDTO> getAllContracts();
    ContractResponseDTO getContractById(Integer id);
    ContractResponseDTO createContract(ContractDTO contractDTO);
    ContractResponseDTO updateContract(Integer id, ContractDTO contractDTO);
    void deleteContract(Integer id);
}