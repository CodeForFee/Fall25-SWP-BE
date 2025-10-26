
package com.example.demo.service;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;
import java.util.List;

public interface UserService {
    // CRUD Operations - 5 endpoints
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO getUserById(Integer userId);
    UserResponseDTO createUser(UserDTO userDTO);
    UserResponseDTO updateUser(Integer userId, UserDTO userDTO);
    void deleteUser(Integer userId);
    // Authentication
    String loginUser(LoginDTO loginDTO);
    User getUserByEmail(String email);
}