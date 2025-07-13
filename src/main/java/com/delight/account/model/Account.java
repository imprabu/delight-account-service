package com.delight.account.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"domain", "status"}))
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_id")
    private PlanType planId;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "type")
    private String industryType;

    private String email;

    @Column(name = "company_name")
    private String companyName;
}
