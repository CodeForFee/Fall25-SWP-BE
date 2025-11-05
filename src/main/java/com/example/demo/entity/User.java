package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "fullName", nullable = false)
    private String fullName;

    @Column(name = "phoneNumber", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "dealerId",nullable = true)
    private Integer dealerId;

    // Liên kết One-to-Many với Promotion
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private List<Promotion> promotions;

    // Liên kết Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealerId", referencedColumnName = "dealerId", insertable = false, updatable = false)
    private Dealer dealer;

    // 🔥 THÊM: Liên kết One-to-Many với Order
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Order> orders;

    // 🔥 THÊM: Liên kết One-to-Many với Quote
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Quote> quotes;

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING
    }

    public enum Role {
        ADMIN,
        DEALER_MANAGER,
        DEALER_STAFF,
        EVM_MANAGER,
    }

    // 🔥 THÊM HELPER METHODS
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isDealerUser() {
        return this.role == Role.DEALER_MANAGER || this.role == Role.DEALER_STAFF;
    }

    public boolean isEVM() {
        return this.role == Role.EVM_MANAGER;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean canApproveQuotes() {
        return this.isEVM() || this.isAdmin();
    }

    public boolean canCreateOrders() {
        return this.isDealerUser() || this.isAdmin();
    }

    public boolean canManageInventory() {
        return this.isAdmin() || this.isEVM();
    }

    // Business validation
    public boolean isValidForOrderCreation() {
        return this.isActive() && this.canCreateOrders();
    }

    // Static factory method
    public static User createDealerManager(String username, String email, String password, String fullName,
                                           String phoneNumber, Integer dealerId) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setRole(Role.DEALER_MANAGER);
        user.setStatus(UserStatus.ACTIVE);
        user.setDealerId(dealerId);
        return user;
    }

    public static User createEVM(String username, String email, String password, String fullName, String phoneNumber) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setRole(Role.EVM_MANAGER);
        user.setStatus(UserStatus.ACTIVE);
        user.setDealerId(null);
        return user;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", dealerId=" + dealerId +
                '}';
    }
}