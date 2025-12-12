package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionInfo {
    private String planCode;
    private String planName;
    private String status;
    private LocalDate billingCycleEndDate;
    private String productCode;
    private LocalDate billingCycleStartDate;
    private LocalDate nextBillingDate;
}



