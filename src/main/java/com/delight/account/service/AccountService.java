package com.delight.account.service;

import com.delight.account.dto.AccountInfoResponse;

public interface AccountService {

    AccountInfoResponse getAccountInfo(Long accountId, Long userId);
}
