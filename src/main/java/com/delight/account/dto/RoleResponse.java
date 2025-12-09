package com.delight.account.dto;

import com.delight.account.model.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role information with details")
public class RoleResponse {
    @Schema(description = "Role ID", example = "1")
    private Long id;

    @Schema(description = "Role code", example = "ORG_ADMIN")
    private String code;

    @Schema(description = "Role name", example = "Org Admin")
    private String name;

    @Schema(description = "Role description", example = "Organization Administrator with full privileges")
    private String description;

    @Schema(description = "Role type", example = "SYSTEM", allowableValues = {"SYSTEM", "CUSTOM"})
    private RoleType roleType;

    @Schema(description = "Whether this is a custom role", example = "false")
    private Boolean isCustom;

    @Schema(description = "Hierarchy level (1 = highest)", example = "1")
    private Integer hierarchyLevel;
}


