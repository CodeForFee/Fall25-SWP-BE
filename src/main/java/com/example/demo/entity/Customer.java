package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "Customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "citizen_id", unique = true)
    private String citizenId;

    @Column(name = "dealer_id", nullable = false)
    private Integer dealerId;

    @Column(name = "is_vip", nullable = false)
    private Boolean isVip = false;

    @Column(name = "total_spent", precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "total_debt", precision = 15, scale = 2)
    private BigDecimal totalDebt = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", referencedColumnName = "dealerId", insertable = false, updatable = false)
    @JsonIgnore
    private Dealer dealer;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Quote> quotes;

    public void updateVipStatus() {
        BigDecimal vipThreshold = new BigDecimal("5000000000");
        this.isVip = this.totalSpent.compareTo(vipThreshold) >= 0;
    }

    public void addToTotalSpent(BigDecimal amount) {
        if (amount != null) {
            this.totalSpent = this.totalSpent.add(amount);
            updateVipStatus();
        }
    }

    public void addDebt(BigDecimal debtAmount) {
        if (debtAmount != null && debtAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDebt = this.totalDebt.add(debtAmount);
        }
    }

    public void reduceDebt(BigDecimal paymentAmount) {
        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentAmount.compareTo(this.totalDebt) > 0) {
                this.totalDebt = BigDecimal.ZERO;
            } else {
                this.totalDebt = this.totalDebt.subtract(paymentAmount);
            }
        }
    }

    public boolean hasDebt() {
        return this.totalDebt.compareTo(BigDecimal.ZERO) > 0;
    }
}