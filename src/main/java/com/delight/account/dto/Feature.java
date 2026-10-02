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
public class Feature {
    private String code;
    private String name;
    private String type;
    private String unit;
    private String description;
    private List<String> products;
    private Integer defaultRateLimit;
}



