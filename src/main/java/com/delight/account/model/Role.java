package com.delight.account.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    OWNER,
    ADMIN,
    MEMBER;

    @JsonValue
    public String toJson() {
        // API represents MEMBER as USER in responses
        return this == MEMBER ? "USER" : this.name();
    }
}
