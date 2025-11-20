package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerDTO {


    private String fullName;
    private String phone;
    private String email;
    private String citizenId;
    private Integer dealerId;

    // ====== CÁC FIELD ĐƯỢC THÊM ======

    // ID của khách hàng
    private Integer id;

    // Khách hàng VIP hay không
    private Boolean isVip;

    // Tổng số tiền khách đã mua (để hiển thị)
    private BigDecimal totalSpent;

    // Tổng nợ của khách
    private BigDecimal totalDebt;
}
