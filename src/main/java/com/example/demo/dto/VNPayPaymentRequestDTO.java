package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VNPayPaymentRequestDTO {
    private Integer orderId;
    private BigDecimal amount;
    private String orderInfo;
    private String bankCode = "";
    private String language = "vn";
}

