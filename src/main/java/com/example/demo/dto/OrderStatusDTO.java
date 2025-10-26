package com.example.demo.dto;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusDTO {
    private String status;
    private Long total;
}
