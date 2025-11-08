package com.example.demo.service;

import com.example.demo.dto.ContractDTO;
import com.example.demo.dto.ContractResponseDTO;
import com.example.demo.entity.Order;

import java.util.List;

public interface ContractService {
    List<ContractResponseDTO> getAllContracts();
    List<ContractResponseDTO> getContractsByDealer(Integer dealerId);
    List<ContractResponseDTO> searchContractsByCustomerName(String customerName);
    Order getOrderByContractId(Integer contractId);
    ContractResponseDTO createContract(ContractDTO contractDTO);
}