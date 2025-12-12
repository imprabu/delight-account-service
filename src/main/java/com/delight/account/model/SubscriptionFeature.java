package com.delight.account.model;

import com.delight.account.util.OffsetDateTimeStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_features",
    uniqueConstraints = @UniqueConstraint(columnNames = {"subscription_id", "feature_code"}))
public class SubscriptionFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "limit_value")
    private Long limitValue;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "rate_limit_rpm")
    private Integer rateLimitRpm;

    @Column(name = "channel_list", columnDefinition = "TEXT")
    private String channelList;

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



