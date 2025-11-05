package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private Integer orderId;
    private Integer paymentPercentage;
    private String paymentNotes;
    private String paymentMethod;
    private BigDecimal amount;

    public boolean isValid() {
        if (paymentPercentage == null) {
            return false;
        }

        return paymentPercentage == 30 || paymentPercentage == 50 ||
                paymentPercentage == 70 || paymentPercentage == 100;
    }
}