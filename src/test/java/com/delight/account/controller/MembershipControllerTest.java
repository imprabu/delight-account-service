package com.delight.account.controller;

import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import com.delight.account.service.MembershipService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MembershipController.class)
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipService membershipService;

    private List<MemberResponse> mockMembers;
    private MemberResponse mockMember;
    private UpdateMemberRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockMember = MemberResponse.builder()
                .userId(1L)
                .emailAddress("owner@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();

        MemberResponse adminMember = MemberResponse.builder()
                .userId(2L)
                .emailAddress("admin@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.ADMIN)
                .status(MembershipStatus.ACTIVE)
                .build();

        mockMembers = Arrays.asList(mockMember, adminMember);

        updateRequest = new UpdateMemberRequest();
        updateRequest.setRole(Role.ADMIN);
        updateRequest.setStatus(MembershipStatus.ACTIVE);
    }

    @Test
    void listMembersReturnsMembers() throws Exception {
        when(membershipService.listMembers(1L, "TEST_PRODUCT"))
                .thenReturn(mockMembers);

        MockHttpServletRequestBuilder request = get("/account/members")
                .header("Authorization", "Bearer valid-token")
                .requestAttr("accountId", 1L)
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].emailAddress").value("owner@example.com"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].userId").value(2))
                .andExpect(jsonPath("$[1].emailAddress").value("admin@example.com"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"))
                .andExpect(jsonPath("$[1].lastName").value("Smith"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"));

        verify(membershipService).listMembers(1L, "TEST_PRODUCT");
    }

    @Test
    void listMembersThrowsExceptionWhenAccountIdMissing() throws Exception {
        MockHttpServletRequestBuilder request = get("/account/members")
                .header("Authorization", "Bearer valid-token")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).listMembers(anyLong(), anyString());
    }

    @Test
    void listMembersThrowsExceptionWhenProductCodeMissing() throws Exception {
        MockHttpServletRequestBuilder request = get("/account/members")
                .header("Authorization", "Bearer valid-token")
                .requestAttr("accountId", 1L);

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).listMembers(anyLong(), anyString());
    }

    @Test
    void listMembersHandlesServiceException() throws Exception {
        when(membershipService.listMembers(1L, "TEST_PRODUCT"))
                .thenThrow(new ApiException("error.account.notfound"));

        MockHttpServletRequestBuilder request = get("/account/members")
                .header("Authorization", "Bearer valid-token")
                .requestAttr("accountId", 1L)
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService).listMembers(1L, "TEST_PRODUCT");
    }

    @Test
    void updateMemberSuccessfully() throws Exception {
        MemberResponse updatedMember = MemberResponse.builder()
                .userId(2L)
                .emailAddress("user@example.com")
                .firstName("Bob")
                .lastName("Johnson")
                .role(Role.ADMIN)
                .status(MembershipStatus.ACTIVE)
                .build();

        when(membershipService.updateMember(1L, 2L, updateRequest, Role.OWNER, "TEST_PRODUCT"))
                .thenReturn(updatedMember);

        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.emailAddress").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.lastName").value("Johnson"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(membershipService).updateMember(1L, 2L, updateRequest, Role.OWNER, "TEST_PRODUCT");
    }

    @Test
    void updateMemberWithOnlyRole() throws Exception {
        UpdateMemberRequest roleOnlyRequest = new UpdateMemberRequest();
        roleOnlyRequest.setRole(Role.MEMBER);

        MemberResponse updatedMember = MemberResponse.builder()
                .userId(2L)
                .emailAddress("user@example.com")
                .firstName("Bob")
                .lastName("Johnson")
                .role(Role.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();

        when(membershipService.updateMember(1L, 2L, roleOnlyRequest, Role.ADMIN, "TEST_PRODUCT"))
                .thenReturn(updatedMember);

        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleOnlyRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "ADMIN")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        verify(membershipService).updateMember(1L, 2L, roleOnlyRequest, Role.ADMIN, "TEST_PRODUCT");
    }

    @Test
    void updateMemberWithOnlyStatus() throws Exception {
        UpdateMemberRequest statusOnlyRequest = new UpdateMemberRequest();
        statusOnlyRequest.setStatus(MembershipStatus.SUSPENDED);

        MemberResponse updatedMember = MemberResponse.builder()
                .userId(2L)
                .emailAddress("user@example.com")
                .firstName("Bob")
                .lastName("Johnson")
                .role(Role.MEMBER)
                .status(MembershipStatus.SUSPENDED)
                .build();

        when(membershipService.updateMember(1L, 2L, statusOnlyRequest, Role.OWNER, "TEST_PRODUCT"))
                .thenReturn(updatedMember);

        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusOnlyRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(membershipService).updateMember(1L, 2L, statusOnlyRequest, Role.OWNER, "TEST_PRODUCT");
    }

    @Test
    void updateMemberThrowsExceptionWhenAccountIdMissing() throws Exception {
        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).updateMember(anyLong(), anyLong(), any(), any(), anyString());
    }

    @Test
    void updateMemberThrowsExceptionWhenRoleMissing() throws Exception {
        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).updateMember(anyLong(), anyLong(), any(), any(), anyString());
    }

    @Test
    void updateMemberThrowsExceptionWhenProductCodeMissing() throws Exception {
        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).updateMember(anyLong(), anyLong(), any(), any(), anyString());
    }

    @Test
    void updateMemberHandlesUserNotFound() throws Exception {
        when(membershipService.updateMember(1L, 999L, updateRequest, Role.OWNER, "TEST_PRODUCT"))
                .thenThrow(new ApiException("error.user.notfound"));

        MockHttpServletRequestBuilder request = patch("/account/members/999")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService).updateMember(1L, 999L, updateRequest, Role.OWNER, "TEST_PRODUCT");
    }

    @Test
    void updateMemberHandlesAuthorizationError() throws Exception {
        when(membershipService.updateMember(1L, 2L, updateRequest, Role.MEMBER, "TEST_PRODUCT"))
                .thenThrow(new ApiException("error.authorization"));

        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "MEMBER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService).updateMember(1L, 2L, updateRequest, Role.MEMBER, "TEST_PRODUCT");
    }

    @Test
    void updateMemberWithEmptyRequest() throws Exception {
        UpdateMemberRequest emptyRequest = new UpdateMemberRequest();

        MemberResponse unchangedMember = MemberResponse.builder()
                .userId(2L)
                .emailAddress("user@example.com")
                .firstName("Bob")
                .lastName("Johnson")
                .role(Role.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();

        when(membershipService.updateMember(1L, 2L, emptyRequest, Role.OWNER, "TEST_PRODUCT"))
                .thenReturn(unchangedMember);

        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(membershipService).updateMember(1L, 2L, emptyRequest, Role.OWNER, "TEST_PRODUCT");
    }

    @Test
    void updateMemberWithInvalidJson() throws Exception {
        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}")
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).updateMember(anyLong(), anyLong(), any(), any(), anyString());
    }

    @Test
    void updateMemberWithoutContentType() throws Exception {
        MockHttpServletRequestBuilder request = patch("/account/members/2")
                .header("Authorization", "Bearer valid-token")
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isUnsupportedMediaType());

        verify(membershipService, never()).updateMember(anyLong(), anyLong(), any(), any(), anyString());
    }

    @Test
    void updateMemberWithInvalidUserId() throws Exception {
        MockHttpServletRequestBuilder request = patch("/account/members/invalid")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .requestAttr("accountId", 1L)
                .requestAttr("role", "OWNER")
                .requestAttr("productCode", "TEST_PRODUCT");

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).updateMember(anyLong(), anyLong(), any(), any(), anyString());
    }
}