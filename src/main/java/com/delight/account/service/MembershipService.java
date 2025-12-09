package com.delight.account.service;

import com.delight.account.dto.InviteMemberRequest;
import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.model.Role;
import java.util.List;

public interface MembershipService {
    List<MemberResponse> listMembers(Long accountId);
    MemberResponse updateMember(Long accountId, Long targetUserId, UpdateMemberRequest request, Role actorRole);
    MemberResponse inviteMember(Long accountId, InviteMemberRequest request);
    void removeMember(Long accountId, Long userId);
}
