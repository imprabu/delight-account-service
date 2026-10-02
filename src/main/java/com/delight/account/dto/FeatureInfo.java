package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureInfo {
    private String code;
    private String name;
    private String type;
    private Long limit;
    private Long currentUsage;
    private Boolean enabled;
    private Integer rateLimitRpm;  // For api.access feature
    private List<String> allowedChannels;  // For channels.allowed feature
}



