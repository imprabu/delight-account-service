package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteResponse {
    private String id;
    private Long accountId;
    private String code;
    private String name;
    private String timezone;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
