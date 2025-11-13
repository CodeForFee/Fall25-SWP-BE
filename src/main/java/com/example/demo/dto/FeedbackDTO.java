package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data; 
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class FeedbackDTO {

    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, max = 100, message = "Tên phải từ 2 đến 100 ký tự")
    private String customerName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Min(value = 1, message = "Đánh giá phải ít nhất là 1")
    @Max(value = 5, message = "Đánh giá tối đa là 5")
    private int rating;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 1000, message = "Nội dung không quá 1000 ký tự")
    private String message;

    // KHÔNG CẦN VIẾT BẤT KỲ GETTER/SETTER nào!
}