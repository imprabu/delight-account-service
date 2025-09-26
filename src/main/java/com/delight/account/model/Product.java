package com.delight.account.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g., HRMS

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}

