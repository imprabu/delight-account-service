package com.delight.account.model;

import com.delight.account.util.OffsetDateTimeStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "privileges",
    uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
public class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String code;

    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "is_system_privilege")
    private Boolean isSystemPrivilege = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Convert(converter = OffsetDateTimeStringConverter.class)
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "privilege", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePrivilege> rolePrivileges = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}


