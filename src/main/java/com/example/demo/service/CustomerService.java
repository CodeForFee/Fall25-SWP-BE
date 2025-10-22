package com.example.demo.service;

import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.CustomerResponseDTO;


import java.util.List;

public interface CustomerService {
    // CRUD Operations - giống UserService
    List<CustomerResponseDTO> getAllCustomers();
    CustomerResponseDTO getCustomerById(Integer id);
    CustomerResponseDTO createCustomer(CustomerDTO customerDTO);
    CustomerResponseDTO updateCustomer(Integer id, CustomerDTO customerDTO);
    void deleteCustomer(Integer id);

    // Search Operations
    List<CustomerResponseDTO> getCustomersByDealer(Integer dealerId);
}