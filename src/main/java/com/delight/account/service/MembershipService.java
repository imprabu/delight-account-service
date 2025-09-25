package com.delight.account.service;

import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.model.Role;
import java.util.List;

public interface MembershipService {
    List<MemberResponse> listMembers(Long accountId, String productCode);
    MemberResponse updateMember(Long accountId, Long targetUserId, UpdateMemberRequest request, Role actorRole, String productCode);
}
