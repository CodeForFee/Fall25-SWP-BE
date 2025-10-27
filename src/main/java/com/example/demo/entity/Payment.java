package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Column(name = "order_id", nullable = false)
    private Integer orderId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Order order;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_code", length = 100, nullable = true)
    private String transactionCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum Status {
        PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }

    public enum PaymentMethod {
        CASH, TRANSFER, INSTALLMENT, CARD
    }
}