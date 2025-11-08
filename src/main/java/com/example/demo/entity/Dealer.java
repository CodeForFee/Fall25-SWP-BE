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
@Table(name = "Dealers")
public class Dealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dealerId")
    private Integer dealerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "representativeName", nullable = false)
    private String representativeName;

    @Column(name = "region")
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DealerStatus status;

    // 🔥 CHỈ THÊM TRƯỜNG TỔNG NỢ
    @Column(name = "outstanding_debt", precision = 15, scale = 2)
    private BigDecimal outstandingDebt = BigDecimal.ZERO;

    @OneToMany(mappedBy = "dealer", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> users;

    @OneToMany(mappedBy = "dealer", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Order> orders;

    @OneToMany(mappedBy = "dealer", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Customer> customers;

    @OneToMany(mappedBy = "dealer", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Inventory> inventories;

    public enum DealerStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public boolean isActive() {
        return this.status == DealerStatus.ACTIVE;
    }

    public boolean canAcceptOrders() {
        return this.isActive();
    }

    public boolean hasUsers() {
        return this.users != null && !this.users.isEmpty();
    }

    public boolean hasActiveUsers() {
        return this.users != null && this.users.stream().anyMatch(User::isActive);
    }

    // Business validation
    public boolean isValidForBusiness() {
        return this.isActive() &&
                this.name != null && !this.name.trim().isEmpty() &&
                this.phone != null && !this.phone.trim().isEmpty() &&
                this.representativeName != null && !this.representativeName.trim().isEmpty();
    }

    // Static factory method
    public static Dealer createDealer(String name, String address, String phone,
                                      String representativeName, String region) {
        Dealer dealer = new Dealer();
        dealer.setName(name);
        dealer.setAddress(address);
        dealer.setPhone(phone);
        dealer.setRepresentativeName(representativeName);
        dealer.setRegion(region);
        dealer.setStatus(DealerStatus.ACTIVE);
        dealer.setOutstandingDebt(BigDecimal.ZERO);
        return dealer;
    }

    public void suspend() {
        this.status = DealerStatus.SUSPENDED;
    }

    public void activate() {
        this.status = DealerStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = DealerStatus.INACTIVE;
    }

    // Address related methods
    public boolean isInRegion(String targetRegion) {
        return this.region != null && this.region.equalsIgnoreCase(targetRegion);
    }

    public boolean hasAddress() {
        return this.address != null && !this.address.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "Dealer{" +
                "dealerId=" + dealerId +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", representativeName='" + representativeName + '\'' +
                ", region='" + region + '\'' +
                ", status=" + status +
                ", outstandingDebt=" + outstandingDebt +
                '}';
    }
}