package com.example.demo.controller;

import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.CustomerResponseDTO;
import com.example.demo.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for customer management and CRM")
@SecurityRequirement(name = "bearer-jwt")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Lấy tất cả customers")
    public ResponseEntity<List<CustomerResponseDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy customer theo ID")
    public ResponseEntity<CustomerResponseDTO> getCustomerById(@PathVariable Integer id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo customer mới")
    public ResponseEntity<CustomerResponseDTO> createCustomer(@RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.ok(customerService.createCustomer(customerDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật customer")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(@PathVariable Integer id, @RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customerDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa customer")
    public ResponseEntity<String> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok("Customer deleted successfully");
    }

    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Lấy customers theo dealer")
    public ResponseEntity<List<CustomerResponseDTO>> getCustomersByDealer(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(customerService.getCustomersByDealer(dealerId));
    }
}