package com.example.demo.entity;

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
@Table(name = "Dealers")
public class Dealer {

    @Id
    @Column(name = "dealerId")
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ✅ SỬA: AUTO → IDENTITY
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

    // Liên kết One-to-Many
    @OneToMany(mappedBy = "dealer", fetch = FetchType.LAZY)
    private List<User> users;
}