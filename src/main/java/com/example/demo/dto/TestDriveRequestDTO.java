package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestDriveRequestDTO {

    private String CustomerName;
    private String CustomerEmail;
    private String PhoneNumber;
    private Integer dealerId; 
    private String Time;
}