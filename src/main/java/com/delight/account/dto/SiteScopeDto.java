package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Site access scope for the session user")
public class SiteScopeDto {
    @Schema(description = "ALL_SITES or SELECTED", example = "ALL_SITES")
    private String mode;

    @Builder.Default
    @Schema(description = "Site UUIDs when mode is SELECTED")
    private List<String> siteIds = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Site codes when mode is SELECTED")
    private List<String> siteCodes = new ArrayList<>();
}
