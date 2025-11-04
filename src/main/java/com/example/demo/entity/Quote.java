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

    // 👈 THÊM GIÁ TRỊ MẶC ĐỊNH
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

    // Liên kết Many-to-One với User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId", insertable = false, updatable = false)
    private User user;

    // Liên kết Many-to-One với Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Customer customer;

    // Liên kết One-to-Many với QuoteDetail
    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<QuoteDetail> quoteDetails;

    // Liên kết One-to-One với Order
    @OneToOne(mappedBy = "quote", fetch = FetchType.LAZY)
    @JsonIgnore
    private Order order;

    public enum QuoteStatus {
        DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED
    }

    // Enum mới cho approval flow
    public enum QuoteApprovalStatus {
        DRAFT,
        PENDING_EVM_APPROVAL,
        APPROVED,
        REJECTED,
        INSUFFICIENT_INVENTORY
    }

    // Helper methods
    public boolean canBeSubmittedForApproval() {
        return this.approvalStatus == QuoteApprovalStatus.DRAFT &&
                this.status == QuoteStatus.DRAFT;
    }

    public boolean canBeApprovedByEVM() {
        return this.approvalStatus == QuoteApprovalStatus.PENDING_EVM_APPROVAL;
    }

    public boolean canCreateOrder() {
        return this.approvalStatus == QuoteApprovalStatus.APPROVED &&
                this.status == QuoteStatus.ACCEPTED;
    }

}