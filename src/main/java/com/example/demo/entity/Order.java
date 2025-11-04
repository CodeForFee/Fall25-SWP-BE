package com.example.demo.entity;

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

    @Column(name = "quote_id", nullable = false)
    private Integer quoteId;

    @Column(name = "customer_id", nullable = false)
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

    // 🔥 THÊM PAYMENT STATUS
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "is_payment_processed")
    private Boolean isPaymentProcessed = false;

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", referencedColumnName = "id", insertable = false, updatable = false)
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

    public enum OrderStatus {
        PENDING, APPROVED, COMPLETED, CANCELLED
    }

    public enum PaymentMethod {
        CASH, TRANSFER, INSTALLMENT, CARD, VNPAY
    }

    public enum OrderApprovalStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, INSUFFICIENT_INVENTORY
    }

    // 🔥 THÊM PAYMENT STATUS ENUM
    public enum PaymentStatus {
        UNPAID, PARTIALLY_PAID, PAID, FAILED, REFUNDED
    }

    // Helper methods
    public boolean canBeApproved() {
        return this.approvalStatus == OrderApprovalStatus.PENDING_APPROVAL &&
                this.status == OrderStatus.PENDING;
    }

    public boolean isApproved() {
        return this.approvalStatus == OrderApprovalStatus.APPROVED &&
                this.status == OrderStatus.APPROVED;
    }

    public boolean hasInventoryIssues() {
        return this.approvalStatus == OrderApprovalStatus.INSUFFICIENT_INVENTORY;
    }

    // 🔥 THÊM METHOD KIỂM TRA CÓ THỂ THANH TOÁN
    public boolean canProcessPayment() {
        return this.isApproved() &&
                this.paymentStatus != PaymentStatus.PAID &&
                this.remainingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isFullyPaid() {
        return this.paymentStatus == PaymentStatus.PAID;
    }

    // 🔥 METHOD CẬP NHẬT SAU KHI THANH TOÁN THÀNH CÔNG
    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAmount = this.totalAmount;
        this.remainingAmount = BigDecimal.ZERO;
        this.isPaymentProcessed = true;
        this.lastPaymentDate = LocalDateTime.now();
        this.status = OrderStatus.COMPLETED;
    }

    public void addPayment(BigDecimal amount) {
        this.paidAmount = this.paidAmount.add(amount);
        this.remainingAmount = this.totalAmount.subtract(this.paidAmount);

        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.paymentStatus = PaymentStatus.PAID;
        } else {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }

        this.lastPaymentDate = LocalDateTime.now();
    }

    // Static factory methods
    public static Order createFromQuote(Quote quote, Integer dealerId, Integer userId, PaymentMethod paymentMethod) {
        Order order = new Order();
        order.setQuoteId(quote.getId());
        order.setCustomerId(quote.getCustomerId());
        order.setDealerId(dealerId);
        order.setUserId(userId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(quote.getTotalAmount());
        order.setTotalDiscount(BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemainingAmount(quote.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setApprovalStatus(OrderApprovalStatus.PENDING_APPROVAL);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setNotes("Created from quote #" + quote.getId());
        return order;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", quoteId=" + quoteId +
                ", customerId=" + customerId +
                ", dealerId=" + dealerId +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", approvalStatus=" + approvalStatus +
                ", paymentStatus=" + paymentStatus +
                '}';
    }
}