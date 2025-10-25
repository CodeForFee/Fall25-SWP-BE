package com.example.demo.dto;

import com.example.demo.entity.User;
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
    private User.Role role;
    private User.UserStatus status;
    private Integer dealerId;
}


