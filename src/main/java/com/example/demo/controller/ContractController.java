package com.example.demo.controller;

import com.example.demo.dto.ContractDTO;
import com.example.demo.dto.ContractResponseDTO;
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
    @Operation(summary = "Lấy tất cả hợp đồng")
    public ResponseEntity<List<ContractResponseDTO>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy hợp đồng theo ID")
    public ResponseEntity<ContractResponseDTO> getContractById(@PathVariable Integer id) {
        return ResponseEntity.ok(contractService.getContractById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo hợp đồng mới")
    public ResponseEntity<ContractResponseDTO> createContract(@RequestBody ContractDTO contractDTO) {
        return ResponseEntity.ok(contractService.createContract(contractDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hợp đồng")
    public ResponseEntity<ContractResponseDTO> updateContract(@PathVariable Integer id, @RequestBody ContractDTO contractDTO) {
        return ResponseEntity.ok(contractService.updateContract(id, contractDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hợp đồng")
    public ResponseEntity<String> deleteContract(@PathVariable Integer id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok("Hợp đồng đã được xóa thành công");
    }
}