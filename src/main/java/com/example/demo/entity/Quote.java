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
@Table(name = "Quote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuoteStatus status;

    // ✅ Giá trị mặc định là DRAFT khi tạo mới quote
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private QuoteApprovalStatus approvalStatus = QuoteApprovalStatus.DRAFT;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    // ✅ Các field tính toán tiền
    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_total", precision = 15, scale = 2)
    private BigDecimal finalTotal;


    // ========================= RELATIONSHIPS ========================= //

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Customer customer;

    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<QuoteDetail> quoteDetails;

    @OneToOne(mappedBy = "quote", fetch = FetchType.LAZY)
    @JsonIgnore
    private Order order;


    // ========================= ENUMS ========================= //

    public enum QuoteStatus {
        DRAFT,
        SENT,
        ACCEPTED,
        REJECTED,
        EXPIRED
    }

    public enum QuoteApprovalStatus {
        DRAFT,
        PENDING_DEALER_MANAGER_APPROVAL,
        PENDING_EVM_APPROVAL,
        APPROVED,
        REJECTED,
        INSUFFICIENT_INVENTORY
    }


    // ========================= LOGIC (BUSINESS RULES) ========================= //

    public boolean canBeSubmittedToDealerManager() {
        return this.approvalStatus == QuoteApprovalStatus.DRAFT
                && this.status == QuoteStatus.DRAFT;
    }

    public boolean canBeApprovedByDealerManager() {
        return this.approvalStatus == QuoteApprovalStatus.PENDING_DEALER_MANAGER_APPROVAL
                && this.status == QuoteStatus.DRAFT;
    }

    public boolean canBeApprovedByEVM() {
        return this.approvalStatus == QuoteApprovalStatus.PENDING_EVM_APPROVAL;
    }

    public boolean canCreateOrder() {
        return this.approvalStatus == QuoteApprovalStatus.APPROVED
                && this.status == QuoteStatus.ACCEPTED;
    }
    public boolean canBeSubmittedForApproval() {
        return this.approvalStatus == QuoteApprovalStatus.DRAFT &&
                this.status == QuoteStatus.DRAFT;
    }
}
