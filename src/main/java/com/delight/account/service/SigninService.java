package com.delight.account.service;

import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.SigninResponse;
import com.delight.account.model.ClientType;

public interface SigninService {

    String signin(Long accountId, String domain, SigninRequest request);
    
    SigninResponse signinWithSession(Long accountId, String domain, SigninRequest request, String userAgent, String ipAddress, ClientType clientType);
}

