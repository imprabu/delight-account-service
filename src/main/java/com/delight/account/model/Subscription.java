package com.delight.account.model;

import com.delight.account.listener.SubscriptionCacheInvalidationListener;
import com.delight.account.util.OffsetDateTimeStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
@EntityListeners({SubscriptionCacheInvalidationListener.class})
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "plan_code", nullable = false, length = 100)
    private String planCode;

    @Column(name = "plan_version", nullable = false, length = 10)
    private String planVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SubscriptionStatus status;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_subscription_id", length = 255)
    private String providerSubscriptionId;

    @Column(name = "billing_cycle_start_date")
    private LocalDate billingCycleStartDate;

    @Column(name = "billing_cycle_end_date")
    private LocalDate billingCycleEndDate;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Convert(converter = OffsetDateTimeStringConverter.class)
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
    
    @jakarta.persistence.OneToMany(mappedBy = "subscription", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<com.delight.account.model.SubscriptionFeature> subscriptionFeatures = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

