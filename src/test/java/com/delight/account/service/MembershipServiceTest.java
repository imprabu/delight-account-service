package com.delight.account.service;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.RoleRepository;
import com.delight.account.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private CacheKeyManager cacheKeyManager;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private com.delight.account.service.AuthorizationService authorizationService;

    @Mock
    private com.delight.account.service.SessionService sessionService;

    @InjectMocks
    private MembershipServiceImpl membershipService;

    private Account testAccount;
    private User testUser1;
    private User testUser2;
    private Membership testMembership1;
    private Membership testMembership2;
    private UpdateMemberRequest updateRequest;
    private RoleEntity orgAdminRole;
    private RoleEntity accountAdminRole;
    private RoleEntity agentRole;

    private RoleEntity createRoleEntity(String code, String name, Long id) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setAccount(testAccount);
        return role;
    }

    @BeforeEach
    void setUp() {
        Product testProduct = new Product();
        testProduct.setCode("TEST_PRODUCT");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setDomain("example.com");
        testAccount.setProduct(testProduct);

        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setEmailAddress("owner@example.com");
        testUser1.setFirstName("John");
        testUser1.setLastName("Doe");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setEmailAddress("user2@test.com");
        testUser2.setFirstName("Jane");
        testUser2.setLastName("Smith");

        // Create RoleEntity objects
        orgAdminRole = createRoleEntity("ORG_ADMIN", "Org Admin", 1L);
        accountAdminRole = createRoleEntity("ACCOUNT_ADMIN", "Account Admin", 2L);
        agentRole = createRoleEntity("AGENT", "Agent", 3L);

        testMembership1 = new Membership();
        testMembership1.setId(1L);
        testMembership1.setUser(testUser1);
        testMembership1.setAccount(testAccount);
        testMembership1.setRole(orgAdminRole);
        testMembership1.setStatus(MembershipStatus.ACTIVE);

        testMembership2 = new Membership();
        testMembership2.setId(2L);
        testMembership2.setUser(testUser2);
        testMembership2.setAccount(testAccount);
        testMembership2.setRole(accountAdminRole);
        testMembership2.setStatus(MembershipStatus.ACTIVE);

        updateRequest = new UpdateMemberRequest();
        updateRequest.setRoleId(3L); // AGENT role ID
        updateRequest.setStatus(MembershipStatus.SUSPENDED);

        // Mock role repository
        when(roleRepository.findById(1L)).thenReturn(Optional.of(orgAdminRole));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(accountAdminRole));
        when(roleRepository.findById(3L)).thenReturn(Optional.of(agentRole));
    }

    @Test
    void listMembersReturnsAllMembers() {
        List<Membership> memberships = Arrays.asList(testMembership1, testMembership2);
        
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByAccountId(1L))
                .thenReturn(memberships);

        List<MemberResponse> result = membershipService.listMembers(1L);

        assertThat(result).hasSize(2);
        
        MemberResponse member1 = result.get(0);
        assertThat(member1.getUserId()).isEqualTo(1L);
        assertThat(member1.getEmailAddress()).isEqualTo("owner@example.com");
        assertThat(member1.getFirstName()).isEqualTo("John");
        assertThat(member1.getLastName()).isEqualTo("Doe");
        assertThat(member1.getRoleCode()).isEqualTo("ORG_ADMIN");
        assertThat(member1.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        MemberResponse member2 = result.get(1);
        assertThat(member2.getUserId()).isEqualTo(2L);
        assertThat(member2.getEmailAddress()).isEqualTo("user2@test.com");
        assertThat(member2.getFirstName()).isEqualTo("Jane");
        assertThat(member2.getLastName()).isEqualTo("Smith");
        assertThat(member2.getRoleCode()).isEqualTo("ACCOUNT_ADMIN");
        assertThat(member2.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        verify(accountRepository).findById(1L);
        verify(membershipRepository).findByAccountId(1L);
    }

    @Test
    void listMembersThrowsExceptionWhenAccountNotFound() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.listMembers(1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.account.notfound");

        verify(accountRepository).findById(1L);
        verify(membershipRepository, never()).findByAccountId(anyLong());
    }

    @Test
    void listMembersReturnsEmptyListWhenNoMembers() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByAccountId(1L))
                .thenReturn(Arrays.asList());

        List<MemberResponse> result = membershipService.listMembers(1L);        

        assertThat(result).isEmpty();
        verify(accountRepository).findById(1L);
        verify(membershipRepository).findByAccountId(1L);
    }

    @Test
    void updateMemberSuccessfullyUpdatesRoleAndStatus() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        when(membershipRepository.save(any(Membership.class)))
                .thenReturn(testMembership2);

        when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);
        when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                .thenReturn(Optional.of(testMembership2));

        MemberResponse result = membershipService.updateMember(1L, 2L, updateRequest, Role.ACCOUNT_ADMIN);

        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getEmailAddress()).isEqualTo("user2@test.com");
        assertThat(result.getRoleCode()).isEqualTo("AGENT");
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);

        // Verify the membership was updated
        assertThat(testMembership2.getRole().getCode()).isEqualTo("AGENT");
        assertThat(testMembership2.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);

        verify(accountRepository).findById(1L);
        verify(membershipRepository).findByUserIdAndAccountId(2L, 1L);
        verify(membershipRepository).save(testMembership2);

        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener
        // No manual cache eviction verification needed
    }

    @Test
    void updateMemberOnlyUpdatesRole() {
        UpdateMemberRequest roleOnlyRequest = new UpdateMemberRequest();
        roleOnlyRequest.setRoleId(2L); // ACCOUNT_ADMIN role ID

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        when(membershipRepository.save(any(Membership.class)))
                .thenReturn(testMembership2);
        when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);

        MemberResponse result = membershipService.updateMember(1L, 2L, roleOnlyRequest, Role.ACCOUNT_ADMIN);

        assertThat(result.getRoleCode()).isEqualTo("ACCOUNT_ADMIN");
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE); // Should remain unchanged

        verify(membershipRepository).save(testMembership2);
        assertThat(testMembership2.getRole().getCode()).isEqualTo("ACCOUNT_ADMIN");
        assertThat(testMembership2.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void updateMemberOnlyUpdatesStatus() {
        UpdateMemberRequest statusOnlyRequest = new UpdateMemberRequest();
        statusOnlyRequest.setStatus(MembershipStatus.ACTIVE);

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        when(membershipRepository.save(any(Membership.class)))
                .thenReturn(testMembership2);

        when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);
        when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                .thenReturn(Optional.of(testMembership2));

        MemberResponse result = membershipService.updateMember(1L, 2L, statusOnlyRequest, Role.ORG_ADMIN);

        assertThat(result.getRoleCode()).isEqualTo("ACCOUNT_ADMIN"); // Should remain unchanged
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        verify(membershipRepository).save(testMembership2);
        assertThat(testMembership2.getRole().getCode()).isEqualTo("ACCOUNT_ADMIN");
        assertThat(testMembership2.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void updateMemberWithEmptyRequest() {
        UpdateMemberRequest emptyRequest = new UpdateMemberRequest();

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        when(membershipRepository.save(any(Membership.class)))
                .thenReturn(testMembership2);

        when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);
        when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                .thenReturn(Optional.of(testMembership2));

        MemberResponse result = membershipService.updateMember(1L, 2L, emptyRequest, Role.ACCOUNT_ADMIN);

        assertThat(result.getRoleCode()).isEqualTo("ACCOUNT_ADMIN"); // Should remain unchanged
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE); // Should remain unchanged

        verify(membershipRepository).save(testMembership2);
        assertThat(testMembership2.getRole().getCode()).isEqualTo("ACCOUNT_ADMIN");
        assertThat(testMembership2.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void updateMemberThrowsExceptionWhenActorRoleIsNull() {
        assertThatThrownBy(() -> membershipService.updateMember(1L, 2L, updateRequest, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.authorization");

        verify(accountRepository, never()).findById(anyLong());
        verify(membershipRepository, never()).findByUserIdAndAccountId(anyLong(), anyLong());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMemberThrowsExceptionWhenActorRoleIsUser() {
        assertThatThrownBy(() -> membershipService.updateMember(1L, 2L, updateRequest, Role.AGENT))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.authorization");

        verify(accountRepository, never()).findById(anyLong());
        verify(membershipRepository, never()).findByUserIdAndAccountId(anyLong(), anyLong());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMemberAllowsAdminRole() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        when(membershipRepository.save(any(Membership.class)))
                .thenReturn(testMembership2);

        when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);
        when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        MemberResponse result = membershipService.updateMember(1L, 2L, updateRequest, Role.ACCOUNT_ADMIN);

        assertThat(result).isNotNull();
        verify(accountRepository).findById(1L);
        verify(membershipRepository).findByUserIdAndAccountId(2L, 1L);
        verify(membershipRepository).save(testMembership2);
    }

    @Test
    void updateMemberAllowsOwnerRole() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        when(membershipRepository.save(any(Membership.class)))
                .thenReturn(testMembership2);

        when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);
        when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                .thenReturn(Optional.of(testMembership2));
        MemberResponse result = membershipService.updateMember(1L, 2L, updateRequest, Role.ORG_ADMIN);

        assertThat(result).isNotNull();
        verify(accountRepository).findById(1L);
        verify(membershipRepository).findByUserIdAndAccountId(2L, 1L);
        verify(membershipRepository).save(testMembership2);
    }

    @Test
    void updateMemberThrowsExceptionWhenAccountNotFound() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.updateMember(1L, 2L, updateRequest, Role.ACCOUNT_ADMIN))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.account.notfound");

        verify(accountRepository).findById(1L);
        verify(membershipRepository, never()).findByUserIdAndAccountId(anyLong(), anyLong());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMemberThrowsExceptionWhenMembershipNotFound() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.updateMember(1L, 2L, updateRequest, Role.ACCOUNT_ADMIN))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.user.notfound");

        verify(accountRepository).findById(1L);
        verify(membershipRepository).findByUserIdAndAccountId(2L, 1L);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMemberWithDifferentProductCodes() {
        // Create account with different product
        Product differentProduct = new Product();
        differentProduct.setCode("DIFFERENT_PRODUCT");
        testAccount.setProduct(differentProduct);

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> membershipService.updateMember(1L, 2L, updateRequest, Role.ORG_ADMIN))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.authorization");

        verify(accountRepository).findById(1L);
        verify(membershipRepository, never()).findByUserIdAndAccountId(anyLong(), anyLong());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMemberUpdatesAllRoleTypes() {
        // Test updating to each role type
        RoleEntity[] roles = {agentRole, accountAdminRole, orgAdminRole};
        Long[] roleIds = {3L, 2L, 1L};
        String[] roleCodes = {"AGENT", "ACCOUNT_ADMIN", "ORG_ADMIN"};
        
        for (int i = 0; i < roles.length; i++) {
            UpdateMemberRequest roleRequest = new UpdateMemberRequest();
            roleRequest.setRoleId(roleIds[i]);
            
            Membership testMembership = new Membership();
            testMembership.setId(2L);
            testMembership.setUser(testUser2);
            testMembership.setAccount(testAccount);
            testMembership.setRole(agentRole); // Start with AGENT role
            testMembership.setStatus(MembershipStatus.ACTIVE);

            when(accountRepository.findById(1L))
                    .thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                    .thenReturn(Optional.of(testMembership));
            when(membershipRepository.save(any(Membership.class)))
                    .thenReturn(testMembership);
            when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);

            MemberResponse result = membershipService.updateMember(1L, 2L, roleRequest, Role.ORG_ADMIN);

            assertThat(result.getRoleCode()).isEqualTo(roleCodes[i]);
            assertThat(testMembership.getRole().getCode()).isEqualTo(roleCodes[i]);
        }
    }

    @Test
    void updateMemberUpdatesAllStatusTypes() {
        // Test updating to each status type
        MembershipStatus[] statuses = {MembershipStatus.ACTIVE, MembershipStatus.SUSPENDED, MembershipStatus.REMOVED};
        
        for (MembershipStatus targetStatus : statuses) {
            UpdateMemberRequest statusRequest = new UpdateMemberRequest();
            statusRequest.setStatus(targetStatus);
            
            Membership testMembership = new Membership();
            testMembership.setId(2L);
            testMembership.setUser(testUser2);
            testMembership.setAccount(testAccount);
            testMembership.setRole(accountAdminRole);
            testMembership.setStatus(MembershipStatus.ACTIVE); // Start with ACTIVE status

            when(accountRepository.findById(1L))
                    .thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountIdWithRole(2L, 1L))
                    .thenReturn(Optional.of(testMembership));
            when(membershipRepository.save(any(Membership.class)))
                    .thenReturn(testMembership);
            when(authorizationService.hasPrivilege(anyLong(), anyLong(), eq("user.update"))).thenReturn(true);

            MemberResponse result = membershipService.updateMember(1L, 2L, statusRequest, Role.ACCOUNT_ADMIN);

            assertThat(result.getStatus()).isEqualTo(targetStatus);
            assertThat(testMembership.getStatus()).isEqualTo(targetStatus);
        }
    }
}