package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
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
    @JsonIgnore
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

    // 🔥 THÊM FIELD PAYMENT PERCENTAGE
    @Column(name = "payment_percentage")
    private Integer paymentPercentage;

    // 🔥 CÁC TRƯỜNG CHO VNPAY
    @Column(name = "vnpay_transaction_no", length = 15)
    private String vnpayTransactionNo;

    @Column(name = "vnpay_bank_code", length = 20)
    private String vnpayBankCode;

    @Column(name = "vnpay_card_type", length = 20)
    private String vnpayCardType;

    @Column(name = "vnpay_pay_date")
    private LocalDateTime vnpayPayDate;

    @Column(name = "vnpay_response_code", length = 2)
    private String vnpayResponseCode;

    @Column(name = "vnpay_txn_ref", length = 100, unique = true)
    private String vnpayTxnRef;

    @Column(name = "vnpay_secure_hash", length = 256)
    private String vnpaySecureHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }

    public enum PaymentMethod {
        CASH, TRANSFER, VNPAY
    }

    // 🔥 HELPER METHODS
    public boolean isSuccessful() {
        return status == Status.COMPLETED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public void markAsCompleted(String vnpayTransactionNo) {
        this.status = Status.COMPLETED;
        this.vnpayTransactionNo = vnpayTransactionNo;
        this.transactionCode = vnpayTransactionNo;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = Status.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public static Payment createVNPayPayment(Order order, BigDecimal amount) {
        Integer paymentPercentage = order.getPaymentPercentage();
        if (paymentPercentage == null) {
            paymentPercentage = 100;
        }

        return Payment.builder()
                .orderId(order.getId())
                .amount(amount)
                .paymentDate(java.time.LocalDate.now())
                .status(Status.PENDING)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentPercentage(paymentPercentage) // 🔥 LẤY TỪ ORDER
                .vnpayTxnRef("VNP" + System.currentTimeMillis())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }
}