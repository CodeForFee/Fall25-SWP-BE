package com.example.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

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
            UserDTO.getUsername(),
            this.passwordEncoder.encode(UserDTO.getPassword()),
            UserDTO.getEmail(),
            UserDTO.getFullName(),
            UserDTO.getPhoneNumber(),
            UserDTO.getRole(),
            UserDTO.getStatus(),
            UserDTO.getDealerId()
        );

        userRepository.save(User);


        return UserDTO;
    }
 
}
