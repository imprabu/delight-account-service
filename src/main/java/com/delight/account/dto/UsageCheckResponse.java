package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageCheckResponse {
    private Boolean allowed;
    private Long currentUsage;
    private Long limit;
    private Long remaining;
    private String message;
}



