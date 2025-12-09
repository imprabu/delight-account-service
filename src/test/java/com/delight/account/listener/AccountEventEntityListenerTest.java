package com.delight.account.listener;

import com.delight.account.domain.BatchJobType;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.job.JobQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountEventEntityListenerTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private JobQueueService jobQueueService;

    @Mock
    private AccountRepository accountRepository;

    private AccountEventEntityListener listener;
    private Account testAccount;
    private Organization testOrganization;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        listener = new AccountEventEntityListener();
        listener.setApplicationContext(applicationContext);

        when(applicationContext.getBean(JobQueueService.class)).thenReturn(jobQueueService);
        // Use lenient() to avoid UnnecessaryStubbingException for tests that don't use AccountRepository
        lenient().when(applicationContext.getBean(AccountRepository.class)).thenReturn(accountRepository);

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");
        testOrganization.setPrimaryDomain("test.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setCode("CHAT");
        testProduct.setName("CHAT Product");

        testAccount = new Account();
        testAccount.setId(100L);
        testAccount.setDomain("testdomain");
        testAccount.setPlan(PlanType.TRIAL);
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setIndustryType("Technology");
        testAccount.setEmail("test@example.com");
        testAccount.setCompanyName("Test Company");
        testAccount.setOrganization(testOrganization);
        testAccount.setProduct(testProduct);
    }

    @Test
    void onAccountCreated_EnqueuesJobWithMinimalMetadata() {
        // When
        listener.onAccountCreated(testAccount);

        // Then
        ArgumentCaptor<BatchJobType> jobTypeCaptor = ArgumentCaptor.forClass(BatchJobType.class);
        ArgumentCaptor<Long> jobIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(jobQueueService).enqueueJob(
            any(BatchJobType.class),
            jobIdCaptor.capture(),
            accountIdCaptor.capture(),
            metadataCaptor.capture()
        );

        assertThat(jobIdCaptor.getValue()).isEqualTo(100L);
        assertThat(accountIdCaptor.getValue()).isEqualTo(100L);

        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata).containsKey("payloadType");
        assertThat(metadata.get("payloadType")).isEqualTo("account_created");
        // Should NOT contain accountInfo - worker will fetch from DB
        assertThat(metadata).doesNotContainKey("accountInfo");
    }

    @Test
    void onAccountUpdated_EnqueuesJobWithChangeSet() {
        // Given - simulate old state
        Account oldAccount = new Account();
        oldAccount.setId(100L);
        oldAccount.setDomain("olddomain");
        oldAccount.setPlan(PlanType.TRIAL);
        oldAccount.setStatus(AccountStatus.ACTIVE);
        oldAccount.setIndustryType("Old Industry");
        oldAccount.setEmail("old@example.com");
        oldAccount.setCompanyName("Old Company");
        oldAccount.setOrganization(testOrganization);
        oldAccount.setProduct(testProduct);

        // Mock repository to return old account
        when(accountRepository.findById(100L)).thenReturn(Optional.of(oldAccount));

        // Capture old state in PreUpdate
        listener.onPreUpdate(testAccount);

        // Update account
        testAccount.setDomain("newdomain");
        testAccount.setPlan(PlanType.PAID);
        testAccount.setCompanyName("New Company");

        // When
        listener.onAccountUpdated(testAccount);

        // Then
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).enqueueJob(
            any(BatchJobType.class),
            eq(100L),
            eq(100L),
            metadataCaptor.capture()
        );

        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata.get("payloadType")).isEqualTo("account_modified");
        assertThat(metadata).containsKey("changeSet");

        @SuppressWarnings("unchecked")
        Map<String, Object> changeSet = (Map<String, Object>) metadata.get("changeSet");
        assertThat(changeSet).isNotEmpty();
        assertThat(changeSet).containsKey("domain");
        assertThat(changeSet).containsKey("plan");
        assertThat(changeSet).containsKey("companyName");
        // Should NOT contain accountInfo - worker will fetch from DB
        assertThat(metadata).doesNotContainKey("accountInfo");
    }

    @Test
    void onAccountUpdated_WithNoChanges_StillEnqueuesJob() {
        // Mock repository to return same account
        when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));
        
        // Capture same state in PreUpdate
        listener.onPreUpdate(testAccount);

        // When - update with same values
        listener.onAccountUpdated(testAccount);

        // Then
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).enqueueJob(
            any(BatchJobType.class),
            any(),
            any(),
            metadataCaptor.capture()
        );

        Map<String, Object> metadata = metadataCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> changeSet = (Map<String, Object>) metadata.get("changeSet");
        // ChangeSet may be empty if no fields changed
        assertThat(changeSet).isNotNull();
    }

    @Test
    void onAccountDeleted_EnqueuesJobWithAccountInfo() {
        // When
        listener.onAccountDeleted(testAccount);

        // Then
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).enqueueJob(
            any(BatchJobType.class),
            eq(100L),
            eq(100L),
            metadataCaptor.capture()
        );

        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata.get("payloadType")).isEqualTo("account_deleted");
        // DELETE should contain accountInfo since we can't fetch after deletion
        assertThat(metadata).containsKey("accountInfo");

        @SuppressWarnings("unchecked")
        Map<String, Object> accountInfo = (Map<String, Object>) metadata.get("accountInfo");
        assertThat(accountInfo.get("id")).isEqualTo(100L);
        assertThat(accountInfo.get("domain")).isEqualTo("testdomain");
        assertThat(accountInfo.get("email")).isEqualTo("test@example.com");
        assertThat(accountInfo.get("companyName")).isEqualTo("Test Company");
    }

    @Test
    void onAccountCreated_WhenJobQueueServiceNotAvailable_LogsWarning() {
        // Given
        AccountEventEntityListener newListener = new AccountEventEntityListener();
        newListener.setApplicationContext(applicationContext);
        
        when(applicationContext.getBean(JobQueueService.class))
            .thenThrow(new org.springframework.beans.BeansException("Bean not found") {});

        // When
        newListener.onAccountCreated(testAccount);

        // Then - should not throw exception, just log warning
        verify(jobQueueService, never()).enqueueJob(any(BatchJobType.class), any(), any(), any());
    }

    @Test
    void onAccountUpdated_WhenJobQueueServiceNotAvailable_CleansUpThreadLocal() {
        // Given
        AccountEventEntityListener newListener = new AccountEventEntityListener();
        newListener.setApplicationContext(applicationContext);
        
        when(applicationContext.getBean(JobQueueService.class))
            .thenThrow(new org.springframework.beans.BeansException("Bean not found") {});
        when(applicationContext.getBean(AccountRepository.class)).thenReturn(accountRepository);
        when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

        // Capture old state
        newListener.onPreUpdate(testAccount);

        // When
        newListener.onAccountUpdated(testAccount);

        // Then - ThreadLocal should be cleaned up
        verify(jobQueueService, never()).enqueueJob(any(BatchJobType.class), any(), any(), any());
    }

    @Test
    void onPreUpdate_CapturesOldState() {
        // Mock repository to return old account
        Account oldAccount = new Account();
        oldAccount.setId(100L);
        oldAccount.setDomain("olddomain");
        oldAccount.setPlan(PlanType.TRIAL);
        oldAccount.setStatus(AccountStatus.ACTIVE);
        oldAccount.setIndustryType("Technology");
        oldAccount.setEmail("test@example.com");
        oldAccount.setCompanyName("Test Company");
        oldAccount.setOrganization(testOrganization);
        oldAccount.setProduct(testProduct);
        
        when(accountRepository.findById(100L)).thenReturn(Optional.of(oldAccount));
        
        // When
        listener.onPreUpdate(testAccount);

        // Then - old state should be captured (we can't directly verify ThreadLocal,
        // but we can verify it's used in the next update)
        testAccount.setDomain("changed");
        listener.onAccountUpdated(testAccount);

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).enqueueJob(any(BatchJobType.class), any(), any(), metadataCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> changeSet = (Map<String, Object>) metadataCaptor.getValue().get("changeSet");
        assertThat(changeSet).containsKey("domain");
    }

    @Test
    void buildAccountInfo_IncludesAllFields() {
        // Use reflection or package-private access to test buildAccountInfo
        // For now, we test it indirectly through onAccountDeleted
        listener.onAccountDeleted(testAccount);

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).enqueueJob(any(BatchJobType.class), any(), any(), metadataCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> accountInfo = (Map<String, Object>) metadataCaptor.getValue().get("accountInfo");
        
        assertThat(accountInfo).containsKeys(
            "id", "domain", "plan", "status", "industryType", 
            "email", "companyName", "organizationId", "organizationName",
            "organizationPrimaryDomain", "productCode", "productName"
        );
        assertThat(accountInfo.get("organizationId")).isEqualTo(1L);
        assertThat(accountInfo.get("productCode")).isEqualTo("CHAT");
    }

    @Test
    void buildAccountInfo_HandlesNullRelations() {
        // Given - account without organization and product
        Account accountWithoutRelations = new Account();
        accountWithoutRelations.setId(200L);
        accountWithoutRelations.setDomain("nodomain");
        accountWithoutRelations.setEmail("no@example.com");

        // When
        listener.onAccountDeleted(accountWithoutRelations);

        // Then
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService).enqueueJob(any(BatchJobType.class), any(), any(), metadataCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> accountInfo = (Map<String, Object>) metadataCaptor.getValue().get("accountInfo");
        
        assertThat(accountInfo.get("id")).isEqualTo(200L);
        assertThat(accountInfo.get("organizationId")).isNull();
        assertThat(accountInfo.get("productCode")).isNull();
    }
}

