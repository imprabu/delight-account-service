package com.delight.account.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "primary_domain")
    private String primaryDomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;
}

