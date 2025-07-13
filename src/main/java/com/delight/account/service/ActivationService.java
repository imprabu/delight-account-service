package com.delight.account.service;

import com.delight.account.dto.ActivationRequest;

public interface ActivationService {
    void activateUser(Long accountId, ActivationRequest request, String country);
}
