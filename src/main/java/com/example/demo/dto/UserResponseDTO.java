package com.example.demo.dto;

import com.example.demo.entity.Role;
import com.example.demo.entity.UserStatus;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class UserResponseDTO {
    private Integer userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private Integer dealerId;
}


