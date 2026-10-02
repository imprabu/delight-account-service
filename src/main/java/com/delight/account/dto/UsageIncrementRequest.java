package com.delight.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageIncrementRequest {
    @NotBlank(message = "Feature code is required")
    private String featureCode;
    
    @NotNull(message = "Increment value is required")
    private Long increment;
}



