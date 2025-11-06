package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Order_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "quote_id", nullable = false, unique = true)
    private Integer quoteId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "dealer_id", nullable = false)
    private Integer dealerId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_discount", precision = 15, scale = 2)
    private BigDecimal totalDiscount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "remaining_amount", precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private OrderApprovalStatus approvalStatus;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "is_payment_processed")
    private Boolean isPaymentProcessed = false;

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    @Column(name = "payment_percentage")
    private Integer paymentPercentage;

    @Column(name = "payment_notes", columnDefinition = "TEXT")
    private String paymentNotes;

    // 🔹 Thêm hạn trả góp (mặc định 12 tháng)
    @Column(name = "installment_months")
    private Integer installmentMonths = 12;

    // ✅ Sửa thành @OneToOne - Một Quote chỉ tạo một Order
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", referencedColumnName = "dealerId", insertable = false, updatable = false)
    private Dealer dealer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Contract> contracts;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Payment> payments;

    // ===== ENUMS =====
    public enum OrderStatus {
        PENDING, APPROVED, COMPLETED, CANCELLED
    }

    public enum PaymentMethod {
        CASH, TRANSFER, INSTALLMENT, CARD, VNPAY
    }

    public enum OrderApprovalStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, INSUFFICIENT_INVENTORY
    }

    public enum PaymentStatus {
        UNPAID, PARTIALLY_PAID, PAID, FAILED, REFUNDED
    }

    // ===== BUSINESS LOGIC =====
    public boolean canBeApproved() {
        return this.approvalStatus == OrderApprovalStatus.PENDING_APPROVAL &&
                this.status == OrderStatus.PENDING;
    }

    public boolean isApproved() {
        return this.approvalStatus == OrderApprovalStatus.APPROVED &&
                this.status == OrderStatus.APPROVED;
    }

    public boolean canProcessPayment() {
        return true;
    }

    public BigDecimal calculatePaymentAmountByPercentage(Integer percentage) {
        if (percentage == null || this.totalAmount == null) {
            return BigDecimal.ZERO;
        }

        switch (percentage) {
            case 30:
                return this.totalAmount.multiply(new BigDecimal("0.30"));
            case 50:
                return this.totalAmount.multiply(new BigDecimal("0.50"));
            case 70:
                return this.totalAmount.multiply(new BigDecimal("0.70"));
            case 100:
                return this.totalAmount;
            default:
                throw new RuntimeException("Invalid payment percentage: " + percentage);
        }
    }

    public BigDecimal calculateRemainingDebt(Integer paymentPercentage) {
        BigDecimal paidAmount = calculatePaymentAmountByPercentage(paymentPercentage);
        return this.totalAmount.subtract(paidAmount);
    }

    public void processPaymentWithPercentage(Integer paymentPercentage, String notes) {
        if (paymentPercentage == null || (paymentPercentage != 30 && paymentPercentage != 50 &&
                paymentPercentage != 70 && paymentPercentage != 100)) {
            throw new RuntimeException("Invalid payment percentage. Must be 30, 50, 70, or 100");
        }

        this.paymentPercentage = paymentPercentage;
        this.paymentNotes = notes;

        BigDecimal paymentAmount = calculatePaymentAmountByPercentage(paymentPercentage);

        this.paidAmount = paymentAmount;
        this.remainingAmount = this.totalAmount.subtract(paymentAmount);

        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.paymentStatus = PaymentStatus.PAID;
        } else {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }

        this.lastPaymentDate = LocalDateTime.now();
        this.isPaymentProcessed = true;
    }

    // Thêm constructor mới vào Order.java
    public Order(Integer quoteId, Integer customerId, Integer dealerId, Integer userId,
                 LocalDate orderDate, OrderStatus status, PaymentMethod paymentMethod,
                 String notes, Integer paymentPercentage) {
        this.quoteId = quoteId;
        this.customerId = customerId;
        this.dealerId = dealerId;
        this.userId = userId;
        this.orderDate = orderDate;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.paymentPercentage = paymentPercentage;
        this.approvalStatus = OrderApprovalStatus.PENDING_APPROVAL;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.paidAmount = BigDecimal.ZERO;
        this.remainingAmount = BigDecimal.ZERO;
        this.totalDiscount = BigDecimal.ZERO;
        this.installmentMonths = 12;
    }
}