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

    // 🔥 SỬA: user_id có thể null để xử lý luồng manager
    @Column(name = "user_id", nullable = true)
    private Integer userId;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuoteStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private QuoteApprovalStatus approvalStatus = QuoteApprovalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_role", nullable = false)
    private User.Role creatorRole;

    @Column(name = "dealer_id", nullable = false)
    private Integer dealerId;

    @Column(name = "current_approver_role", length = 50)
    private String currentApproverRole;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private Customer customer;

    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
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

    // ========================= BUSINESS RULES ========================= //

    /**
     * 🔥 STAFF CÓ THỂ GỬI QUOTE CỦA CHÍNH MÌNH CHO MANAGER
     */
    public boolean canBeSubmittedToDealerManager(User currentUser) {
        return this.creatorRole == User.Role.DEALER_STAFF &&
                this.userId != null && this.userId.equals(currentUser.getUserId()) && // Staff gửi quote của chính mình
                this.approvalStatus == QuoteApprovalStatus.DRAFT &&
                this.status == QuoteStatus.DRAFT;
    }

    /**
     * 🔥 MANAGER CÙNG DEALER CÓ THỂ DUYỆT QUOTE CỦA STAFF
     */
    public boolean canBeApprovedByDealerManager(User manager) {
        return this.creatorRole == User.Role.DEALER_STAFF && // Quote được tạo bởi staff
                manager.getRole() == User.Role.DEALER_MANAGER && // Người duyệt là manager
                this.dealerId.equals(manager.getDealerId()) && // Cùng dealer
                this.approvalStatus == QuoteApprovalStatus.PENDING_DEALER_MANAGER_APPROVAL &&
                this.status == QuoteStatus.DRAFT;
    }

    /**
     * 🔥 STAFF CÓ THỂ TẠO ORDER TỪ QUOTE ĐÃ ĐƯỢC MANAGER DUYỆT
     */
    public boolean canCreateOrder(User currentUser) {
        return this.creatorRole == User.Role.DEALER_STAFF && // Quote được tạo bởi staff
                this.userId != null && this.userId.equals(currentUser.getUserId()) && // Staff tạo order từ quote của chính mình
                this.approvalStatus == QuoteApprovalStatus.APPROVED &&
                this.status == QuoteStatus.ACCEPTED;
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