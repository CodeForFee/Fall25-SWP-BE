package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
@Tag(name = "User Management", description = "APIs for user management and authentication")
public class UserController {

    @Autowired
    private UserService userService;


    @PostMapping("/register")
    @Operation(summary = "Đăng ký user mới")
    public UserResponseDTO register(@RequestBody UserDTO userDTO) {
        return userService.registerUser(userDTO);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        String token = userService.loginUser(loginDTO);
        return ResponseEntity.ok(Map.of("token",token));
    }


    @GetMapping
    @Operation(summary = "Lấy tất cả users")
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy user theo ID")
    public UserResponseDTO getUserById(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @Operation(summary = "Tạo user mới")
    public UserResponseDTO createUser(@RequestBody UserDTO userDTO) {
        return userService.createUser(userDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật user")
    public UserResponseDTO updateUser(@PathVariable Integer id, @RequestBody UserDTO userDTO) {
        return userService.updateUser(id, userDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa user")
    public String deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return "User deleted successfully";
    }

    // ROLE
    @PutMapping("/{id}/role")
    @Operation(summary = "Cập nhật role cho user")
    public UserResponseDTO updateUserRole(@PathVariable Integer id, @RequestParam String role) {
        return userService.updateUserRole(id, role);
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Lấy users theo role")
    public List<UserResponseDTO> getUsersByRole(@PathVariable String role) {
        return userService.getUserByRoles(role);
    }

    // STATUS
    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái user")
    public UserResponseDTO updateUserStatus(@PathVariable Integer id, @RequestParam String status) {
        return userService.updateUserStatus(id, status);
    }

    // DEALER
    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Lấy users theo dealer")
    public List<UserResponseDTO> getUsersByDealer(@PathVariable Integer dealerId) {
        return userService.getUsersByDealer(dealerId);
    }
}
