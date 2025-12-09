package com.delight.account.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    ORG_ADMIN("Org Admin", "Account owner with full privileges"),
    ACCOUNT_ADMIN("Account Admin", "Account administrator with management privileges"),
    AGENT("Agent", "Agent with basic privileges");

    private String displayName;
    private String description;

    private Role(String displayName, String description){
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Returns the database code for this role (uses enum name: "ORG_ADMIN", "ACCOUNT_ADMIN", "AGENT")
     */
    public String getCode() {
        return this.name();
    }
    
    /**
     * Returns the display name for this role (e.g., "Org Admin", "Account Admin", "Agent")
     */
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription(){
        return description;
    }
    
    @JsonValue
    public String toJson() {
        // API represents AGENT as USER in responses
        return this == AGENT ? "USER" : this.name();
    }
}
