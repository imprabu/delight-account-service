package com.delight.account.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * PlantPulse system roles: Account Admin, Admin, User.
 */
public enum Role {
    ACCOUNT_ADMIN("Account Admin", "Account owner with full privileges across all sites"),
    ADMIN("Admin", "Site administrator with management privileges on assigned sites"),
    USER("User", "Operator with read access on assigned sites");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return this.name();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }

    /**
     * Resolve role from DB/API code, including legacy aliases from delight-account-service.
     */
    public static Role fromCode(String code) {
        if (code == null || code.isBlank()) {
            return USER;
        }
        String c = code.trim().toUpperCase();
        return switch (c) {
            case "ACCOUNT_ADMIN", "ORG_ADMIN", "OWNER" -> ACCOUNT_ADMIN;
            case "ADMIN" -> ADMIN;
            case "USER", "AGENT", "MEMBER" -> USER;
            default -> {
                try {
                    yield Role.valueOf(c);
                } catch (IllegalArgumentException e) {
                    yield USER;
                }
            }
        };
    }
}
