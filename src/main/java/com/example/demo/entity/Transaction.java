package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bảng Transaction lưu chi tiết từng kỳ thanh toán trả góp
 */
@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi Transaction liên kết đến một Payment
    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber; // Kỳ thứ mấy

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount; // Số tiền cần trả cho kỳ này

    @Column(name = "due_date")
    private LocalDate dueDate; // Ngày đến hạn thanh toán

    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // Ngày thanh toán thực tế

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING hoặc PAID

    @Column(length = 50)
    private String method; // Hình thức thanh toán (VD: VNPAY, CASH,...)

    @Column(length = 255)
    private String note; // Ghi chú thêm (VD: "Kỳ 1/6")

    public enum Status {
        PENDING,
        PAID
    }
}
