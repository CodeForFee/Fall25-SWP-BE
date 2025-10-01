package com.example.demo.service.impl;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public UserDTO registerUser(com.example.demo.dto.UserDTO UserDTO) {

        User User = new User(
            UserDTO.getUserId(),
            UserDTO.getEmail(),
            this.passwordEncoder.encode(UserDTO.getPassword()),
            UserDTO.getFullName(),
            UserDTO.getPhoneNumber(),
            UserDTO.getRole(),
            UserDTO.getStatus(),
            UserDTO.getDealerId()
        );

        userRepository.save(User);


        return UserDTO;
    }

    @Override
    public String loginUser(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
            .orElseThrow(() -> new RuntimeException("Email không tồn tại"));
        
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai mật khẩu");
        }
        
        String token = Jwts.builder()
                            .setSubject(user.getEmail())
                            .setExpiration(new Date(System.currentTimeMillis() + 24))
                            .compact();
        
        return token;
    }

 
}
