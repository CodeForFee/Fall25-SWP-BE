package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.dto.UserDTO;

@Service
public interface UserService {

    String registerUser(UserDTO userDTO);

}
