package com.delight.account.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"domain", "status"}))
@EntityListeners(com.delight.account.listener.AccountEventEntityListener.class)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan")
    private PlanType plan;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "type")
    private String industryType;

    private String email;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "updated_at", columnDefinition = "datetime(6)")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
