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
public class UsageDecrementRequest {
    @NotBlank(message = "Feature code is required")
    private String featureCode;
    
    @NotNull(message = "Decrement value is required")
    private Long decrement;
}



