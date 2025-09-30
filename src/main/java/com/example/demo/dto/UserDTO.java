package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter 
public class UserDTO {

    private int userId;

    private String password;
    
    private String email;

    private String fullName;

    private String phoneNumber;

    private String role;

    private String status;

    private Integer dealerId;
}
