package com.delight.account.model;

import com.delight.account.listener.CacheInvalidationEntityListener;
import com.delight.account.listener.CasbinPolicyReloadListener;
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
@Table(name = "memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "account_id"}))
@EntityListeners({CacheInvalidationEntityListener.class, CasbinPolicyReloadListener.class, com.delight.account.listener.MembershipEventEntityListener.class})
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "site_scope_mode", nullable = false, length = 32)
    private SiteScopeMode siteScopeMode = SiteScopeMode.SELECTED;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Convert(converter = OffsetDateTimeStringConverter.class)
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (siteScopeMode == null) {
            siteScopeMode = SiteScopeMode.SELECTED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

