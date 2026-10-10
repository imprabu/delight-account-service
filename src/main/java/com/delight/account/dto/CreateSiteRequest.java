package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Create a site under the current account")
public class CreateSiteRequest {
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "code must be alphanumeric with _ or -")
    @Schema(example = "PLANT-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank
    @Size(max = 255)
    @Schema(example = "Main STP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 64)
    @Schema(example = "Asia/Kolkata")
    private String timezone;
}
