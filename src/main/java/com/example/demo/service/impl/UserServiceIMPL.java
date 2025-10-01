package com.example.demo.service.impl;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserStatus;
import jdk.jshell.spi.ExecutionControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

import io.jsonwebtoken.Jwts;

@Service
public class UserServiceIMPL implements com.example.demo.service.UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public UserResponseDTO registerUser(UserDTO userDTO) {
        try {
            System.out.println("=== START REGISTER USER ===");

            // Check if email already exists
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new RuntimeException("Email đã tồn tại");
            }

            // Check if phone number already exists
            if (userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }

            User user = new User();
            user.setUsername(userDTO.getUsername() != null && !userDTO.getUsername().isEmpty()
                    ? userDTO.getUsername()
                    : userDTO.getEmail());

            user.setEmail(userDTO.getEmail());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            user.setFullName(userDTO.getFullName());
            user.setPhoneNumber(userDTO.getPhoneNumber());
            user.setRole(userDTO.getRole() != null ? userDTO.getRole() : Role.USER);
            user.setStatus(userDTO.getStatus() != null ? userDTO.getStatus() : UserStatus.ACTIVE);
            user.setDealerId(userDTO.getDealerId());

            User savedUser = userRepository.save(user);
            return convertToResponseDTO(savedUser);

        } catch (Exception e) {
            System.err.println("!!! ERROR IN REGISTER USER !!!");
            e.printStackTrace();
            throw new RuntimeException("Lỗi server: " + e.getMessage());
        }
    }


    private UserResponseDTO convertToResponseDTO(User user) {
        try {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setEmail(user.getEmail());
            dto.setFullName(user.getFullName());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setRole(user.getRole());
            dto.setStatus(user.getStatus());
            dto.setDealerId(user.getDealerId());
            return dto;
        } catch (Exception e) {
            System.err.println("Error converting to DTO: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String loginUser(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Email Not Found"));

        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password Do Not Match");
        }
        return "login-success";
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        return convertToResponseDTO(user);
    }

    @Override
    public UserResponseDTO createUser(UserDTO userDTO) {
        return registerUser(userDTO);
    }

    @Override
    public UserResponseDTO updateUser(Integer userId, UserDTO userDTO) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        // Cập nhật username nếu có
        if (userDTO.getUsername() != null && !userDTO.getUsername().isEmpty()) {
            existingUser.setUsername(userDTO.getUsername());
        }

        existingUser.setEmail(userDTO.getEmail());
        existingUser.setFullName(userDTO.getFullName());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());

        if (userDTO.getRole()!=null) {
            existingUser.setRole(userDTO.getRole());
        }

        if (userDTO.getPassword()!=null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        return convertToResponseDTO(updatedUser);
    }

    @Override
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        userRepository.delete(user);
    }

    @Override
    public UserResponseDTO updateUserRole(Integer userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        Role newRole = Role.valueOf(role.toUpperCase());
        user.setRole(newRole);

        User updatedUser = userRepository.save(user);
        return convertToResponseDTO(updatedUser);
    }


    @Override
    public List<UserResponseDTO> getUserByRoles(String role) {
        return userRepository.findByRole(role).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUserStatus(Integer userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        try {
            UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
            user.setStatus(newStatus);
            User updatedUser = userRepository.save(user);
            return convertToResponseDTO(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + status);
        }
    }

    @Override
    public List<UserResponseDTO> getUsersByDealer(Integer dealerId) {
        return userRepository.findByDealerId(dealerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

}