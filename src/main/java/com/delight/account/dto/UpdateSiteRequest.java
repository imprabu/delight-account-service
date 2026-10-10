package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSiteRequest {
    @Size(max = 255)
    private String name;

    @Size(max = 64)
    private String timezone;
}
