package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.example.demo.dto.UserDTO;
import com.example.demo.service.UserService;
@RestController
@CrossOrigin
@RequestMapping("/api/user")
@Tag(name = "User")



public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public UserDTO register(@RequestBody UserDTO userDTO) {
        
        UserDTO user = userService.registerUser(userDTO);
        return user;
    }

    @PostMapping("/login")
    public UserDTO login(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.loginUser(userDTO);
        return user;
    }    

    

}
