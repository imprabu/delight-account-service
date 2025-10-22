package com.delight.account.controller;

import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.repository.OrganizationRepository;
import com.delight.account.repository.ProductRepository;
import com.delight.account.repository.SessionRepository;
import com.delight.account.service.MembershipService;
import com.delight.account.service.AccountService;
import com.delight.account.service.SignupService;
import com.delight.account.service.ActivationService;
import com.delight.account.service.SigninService;
import com.delight.account.service.SessionService;
import com.delight.account.filter.JwtAuthenticationFilter;
import com.delight.account.interceptor.DomainAccountInterceptor;
import com.delight.account.util.LoggingUtils;
import org.springframework.context.MessageSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class MembershipControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(MembershipControllerTest.class);
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipService membershipService;

    // Repository mocks
    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private OrganizationRepository organizationRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private SessionRepository sessionRepository;

    // Service mocks
    @MockBean
    private AccountService accountService;

    @MockBean
    private SignupService signupService;

    @MockBean
    private ActivationService activationService;

    @MockBean
    private SigninService signinService;

    @MockBean
    private SessionService sessionService;

    // Security and configuration mocks
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private DomainAccountInterceptor domainAccountInterceptor;

    @MockBean
    private MessageSource messageSource;

    private List<MemberResponse> mockMembers;
    private MemberResponse mockMember;
    private UpdateMemberRequest updateRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Configure the mocked JWT filter to set request attributes properly
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain filterChain = invocation.getArgument(2);

            LoggingUtils.logMethodEntry(logger, "JwtAuthenticationFilter.doFilter");

            // Only populate attributes when using a special test token
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.contains("valid-token-with-attrs")) {
                // Set attributes directly on the original request since MockMvc uses it for RequestContextHolder
                request.setAttribute("accountId", 1L);
                request.setAttribute("productCode", "TEST_PRODUCT");
                request.setAttribute("userId", 1L);
                request.setAttribute("role", "ADMIN");
                LoggingUtils.logBusinessOperation(logger, "SET_REQUEST_ATTRIBUTES", "SUCCESS", "accountId", 1L, "productCode", "TEST_PRODUCT");
            }

            // Continue with the filter chain using the original request
            filterChain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
        
        // Configure the mocked DomainAccountInterceptor to allow requests to pass through
        when(domainAccountInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any(Object.class)))
                .thenReturn(true);
        
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(jwtAuthenticationFilter)
                .build();

        mockMember = MemberResponse.builder()
                .userId(1L)
                .emailAddress("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();

        MemberResponse adminMember = MemberResponse.builder()
                .userId(2L)
                .emailAddress("admin@example.com")
                .firstName("Admin")
                .lastName("User")
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
        // Add debug output to verify the mock is being called
        when(membershipService.listMembers(1L, "TEST_PRODUCT"))
                .thenAnswer(invocation -> {
                    LoggingUtils.logBusinessOperation(logger, "MEMBERSHIP_SERVICE_CALL", "SUCCESS", "accountId", invocation.getArgument(0), "productCode", invocation.getArgument(1));
                    return mockMembers;
                });

        mockMvc.perform(get("/account/members")
                .header("Authorization", "Bearer valid-token-with-attrs"))
                .andDo(result -> {
                    LoggingUtils.logBusinessOperation(logger, "TEST_RESPONSE_STATUS", "INFO", "status", result.getResponse().getStatus());
                    LoggingUtils.logBusinessOperation(logger, "TEST_RESPONSE_CONTENT", "INFO", "content", result.getResponse().getContentAsString());
                    LoggingUtils.logBusinessOperation(logger, "TEST_RESPONSE_CONTENT_TYPE", "INFO", "contentType", result.getResponse().getContentType());
                    LoggingUtils.logBusinessOperation(logger, "TEST_RESPONSE_HEADERS", "INFO", "headers", result.getResponse().getHeaderNames());
                    LoggingUtils.logBusinessOperation(logger, "TEST_REQUEST_URI", "INFO", "uri", result.getRequest().getRequestURI());
                    LoggingUtils.logBusinessOperation(logger, "TEST_REQUEST_METHOD", "INFO", "method", result.getRequest().getMethod());
                    LoggingUtils.logBusinessOperation(logger, "TEST_REQUEST_ATTRIBUTES", "INFO", "accountId", result.getRequest().getAttribute("accountId"), "productCode", result.getRequest().getAttribute("productCode"));
                    LoggingUtils.logBusinessOperation(logger, "TEST_ALL_REQUEST_ATTRIBUTES", "INFO", "attributes", java.util.Collections.list(result.getRequest().getAttributeNames()));
                })
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$[1].userId").value(2))
                .andExpect(jsonPath("$[1].emailAddress").value("admin@example.com"));
                
        // Verify that the service was called
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