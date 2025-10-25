package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "vin", nullable = false, length = 50)
    private String vin;

    @Column(name = "contract_number", nullable = false, unique = true, length = 100)
    private String contractNumber;

    @Column(name = "signed_date")
    private LocalDate signedDate;

    @Column(name = "customer_signature", columnDefinition = "TEXT")
    private String customerSignature;

    @Column(name = "dealer_representative", length = 255)
    private String dealerRepresentative;

    // Relationships - SỬA LẠI @JoinColumn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Order order;
}