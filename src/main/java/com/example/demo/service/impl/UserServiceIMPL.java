package com.example.demo.service.impl;

import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceIMPL implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Lazy
    private final JwtService jwtService;


    @Override
    public UserResponseDTO registerUser(UserDTO userDTO) {
        try {
            log.info("=== START REGISTER USER ===");

            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new RuntimeException("Email đã tồn tại");
            }

            if (userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }

            User user = new User();
            user.setUsername(
                    (userDTO.getUsername() != null && !userDTO.getUsername().isEmpty())
                            ? userDTO.getUsername()
                            : userDTO.getEmail()
            );
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
            log.error("!!! ERROR IN REGISTER USER !!!", e);
            throw new RuntimeException("Lỗi server: " + e.getMessage());
        }
    }


    private UserResponseDTO convertToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setDealerId(user.getDealerId());
        return dto;
    }


    @Override
    public String loginUser(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        // Nếu Role là Enum
        String roleName = (user.getRole() != null) ? user.getRole().name() : "USER";
        return jwtService.generateToken(loginDTO.getEmail(), roleName);
    }


    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
    }


    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }


    @Override
    public UserResponseDTO getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return convertToResponseDTO(user);
    }


    @Override
    public UserResponseDTO createUser(UserDTO userDTO) {
        return registerUser(userDTO);
    }


    @Override
    public UserResponseDTO updateUser(Integer userId, UserDTO userDTO) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (userDTO.getUsername() != null && !userDTO.getUsername().isEmpty()) {
            existingUser.setUsername(userDTO.getUsername());
        }
        if (userDTO.getEmail() != null) {
            existingUser.setEmail(userDTO.getEmail());
        }
        if (userDTO.getFullName() != null) {
            existingUser.setFullName(userDTO.getFullName());
        }
        if (userDTO.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        }
        if (userDTO.getRole() != null) {
            existingUser.setRole(userDTO.getRole());
        }
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        return convertToResponseDTO(updatedUser);
    }


    @Override
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        userRepository.delete(user);
    }

    //
    @Override
    public UserResponseDTO updateUserRole(Integer userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Role newRole;
        try {
            newRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ: " + role);
        }

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

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


    @PostConstruct
    public void initTestData() {
        try {
            // Kiểm tra nếu chưa có user admin
            if (!userRepository.existsByEmail("admin@example.com")) {
                UserDTO adminDTO = new UserDTO();
                adminDTO.setUsername("admin");
                adminDTO.setPassword("123456");
                adminDTO.setEmail("admin@example.com");
                adminDTO.setFullName("Admin User");
                adminDTO.setPhoneNumber("0912345678");
                adminDTO.setRole(Role.ADMIN);
                adminDTO.setStatus(UserStatus.ACTIVE);

                registerUser(adminDTO);
                log.info("=== TEST ADMIN USER CREATED ===");
                log.info("Email: admin@example.com");
                log.info("Password: 123456");
            }

            // Kiểm tra nếu chưa có user thường
            if (!userRepository.existsByEmail("user@example.com")) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUsername("user");
                userDTO.setPassword("123456");
                userDTO.setEmail("user@example.com");
                userDTO.setFullName("Regular User");
                userDTO.setPhoneNumber("0923456789");
                userDTO.setRole(Role.USER);
                userDTO.setStatus(UserStatus.ACTIVE);

                registerUser(userDTO);
                log.info("=== TEST USER CREATED ===");
            }
        } catch (Exception e) {
            log.info("Test users already exist or error: " + e.getMessage());
        }
    }

}
