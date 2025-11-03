package com.example.demo.dto;

import lombok.Data;

@Data
public class VNPayPaymentResponseDTO {
    private String code;
    private String message;
    private String paymentUrl;
    private String transactionId;
}
