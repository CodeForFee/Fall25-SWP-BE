package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.UserService;
@RestController
@CrossOrigin
@RequestMapping("api/user")



public class UserController {
    @Autowired
    private UserService userService;

@PostMapping("/register")
public String register(@RequestBody UserDTO userDTO) {
    
    String id = userService.registerUser(userDTO);
    return id;

}
}