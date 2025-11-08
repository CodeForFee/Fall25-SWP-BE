package com.example.demo.controller;

import com.example.demo.dto.ContractDTO;
import com.example.demo.dto.ContractResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Contract Management", description = "APIs for contract management")
@SecurityRequirement(name = "bearer-jwt")
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @Operation(summary = "Lấy tất cả hợp đồng (Admin)")
    public ResponseEntity<List<ContractResponseDTO>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Lấy hợp đồng theo dealer")
    public ResponseEntity<List<ContractResponseDTO>> getContractsByDealer(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(contractService.getContractsByDealer(dealerId));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm hợp đồng theo tên khách hàng")
    public ResponseEntity<List<ContractResponseDTO>> searchContractsByCustomerName(
            @RequestParam String customerName) {
        return ResponseEntity.ok(contractService.searchContractsByCustomerName(customerName));
    }

    @GetMapping("/{contractId}/order")
    @Operation(summary = "Lấy thông tin order của hợp đồng")
    public ResponseEntity<Order> getOrderByContractId(@PathVariable Integer contractId) {
        return ResponseEntity.ok(contractService.getOrderByContractId(contractId));
    }

    @PostMapping
    @Operation(summary = "Tạo hợp đồng mới")
    public ResponseEntity<ContractResponseDTO> createContract(@RequestBody ContractDTO contractDTO) {
        return ResponseEntity.ok(contractService.createContract(contractDTO));
    }
}