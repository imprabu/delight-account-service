package com.delight.account.model;

import com.delight.account.util.OffsetDateTimeStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "feature_usage",
    uniqueConstraints = @UniqueConstraint(columnNames = {
        "account_id", "subscription_id", "feature_code", "billing_cycle_start_date"
    }))
public class FeatureUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "usage_value", nullable = false)
    private Long usageValue = 0L;

    @Column(name = "billing_cycle_start_date", nullable = false)
    private LocalDate billingCycleStartDate;

    @Column(name = "billing_cycle_end_date", nullable = false)
    private LocalDate billingCycleEndDate;

    @Column(name = "last_reset_at")
    private OffsetDateTime lastResetAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Convert(converter = OffsetDateTimeStringConverter.class)
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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



