package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentResponseDTO {
    private Integer id;
    private Integer orderId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String transactionCode;
    private String vnpayTransactionNo;
    private String vnpayResponseCode;
    private String vnpayTxnRef;
}
