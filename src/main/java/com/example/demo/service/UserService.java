package com.example.demo.service;

import com.example.demo.dto.UserResponseDTO;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;

import java.util.List;

@Service
public interface UserService {

    UserResponseDTO registerUser(UserDTO userDTO);

    String loginUser(LoginDTO loginDTO);

    List<UserResponseDTO> getAllUsers();
    UserResponseDTO getUserById(Integer userId);
    UserResponseDTO createUser(UserDTO userDTO);
    UserResponseDTO updateUser(Integer userId,UserDTO userDTO);
    void deleteUser(Integer userId);

    UserResponseDTO updateUserRole(Integer userId, String roleId);
    List<UserResponseDTO> getUserByRoles(String role);
    UserResponseDTO updateUserStatus(Integer userId, String status);
    List<UserResponseDTO> getUsersByDealer(Integer dealerId);
}
