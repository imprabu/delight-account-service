package com.delight.account.service;

import com.delight.account.dto.SigninRequest;

public interface SigninService {

    String signin(Long accountId, SigninRequest request);
}

