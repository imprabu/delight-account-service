package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturePlanConfig {
    private String version;
    private LocalDate effectiveDate;
    private List<ProductConfig> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductConfig {
        private String code;
        private String name;
        private List<PlanConfig> plans;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanConfig {
        private String code;
        private String name;
        private List<FeatureConfig> features;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureConfig {
        private String code;
        private Long limit;
        private Boolean enabled;
        private Integer rateLimitRpm;
        private List<String> channels;
    }
}

