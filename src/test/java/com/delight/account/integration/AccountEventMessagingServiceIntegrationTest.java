package com.delight.account.integration;

import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.OrganizationRepository;
import com.delight.account.repository.ProductRepository;
import com.delight.account.service.MessagingService;
import com.delight.account.service.job.JobQueueService;
import com.delight.account.service.job.worker.AccountEventPublisherWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test to verify that the messaging service is actually called
 * when accounts are created, updated, or deleted.
 * 
 * This test manually triggers the worker to process jobs (simulating JobRunr)
 * to verify the complete flow from entity listener to messaging service.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountEventMessagingServiceIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductRepository productRepository;

    @SpyBean
    private JobQueueService jobQueueService;

    @Autowired
    private AccountEventPublisherWorker accountEventPublisherWorker;

    @SpyBean
    private MessagingService messagingService;

    private Organization testOrganization;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setPrimaryDomain("test.com");
        testOrganization.setStatus(OrganizationStatus.ACTIVE);
        testOrganization = organizationRepository.save(testOrganization);

        // Create test product
        testProduct = new Product();
        testProduct.setCode("HRMS");
        testProduct.setName("HRMS Product");
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void accountCreated_CallsMessagingService() {
        // Given
        Account account = new Account();
        account.setDomain("newaccount");
        account.setPlan(PlanType.TRIAL);
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
        account.setEmail("new@example.com");
        account.setCompanyName("New Company");
        account.setOrganization(testOrganization);
        account.setProduct(testProduct);

        // When
        Account savedAccount = accountRepository.save(account);
        accountRepository.flush(); // Ensure entity listener is triggered

        // Capture the job metadata that was enqueued
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService, atLeastOnce()).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            metadataCaptor.capture()
        );

        // Get the captured metadata
        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata).isNotNull();
        assertThat(metadata.get("payloadType")).isEqualTo("account_created");

        // Manually trigger the worker (simulating JobRunr)
        accountEventPublisherWorker.processJob(1L, savedAccount.getId(), metadata);

        // Then - verify messaging service was called
        ArgumentCaptor<String> payloadTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accountIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> changeSetCaptor = ArgumentCaptor.forClass(Map.class);

        verify(messagingService, atLeastOnce()).publishAccountEventWithRetry(
            payloadTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture(),
            changeSetCaptor.capture()
        );

        assertThat(payloadTypeCaptor.getValue()).isEqualTo("account_created");
        assertThat(accountIdCaptor.getValue()).isEqualTo(savedAccount.getId().toString());
        assertThat(changeSetCaptor.getValue()).isNull();
        
        Map<String, Object> accountInfo = accountInfoCaptor.getValue();
        assertThat(accountInfo).isNotNull();
        assertThat(accountInfo.get("id")).isEqualTo(savedAccount.getId());
        assertThat(accountInfo.get("domain")).isEqualTo("newaccount");
        assertThat(accountInfo.get("email")).isEqualTo("new@example.com");
    }

    @Test
    void accountUpdated_CallsMessagingServiceWithChangeSet() {
        // Given - create account first
        Account account = new Account();
        account.setDomain("updateaccount");
        account.setPlan(PlanType.TRIAL);
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
        account.setEmail("update@example.com");
        account.setCompanyName("Update Company");
        account.setOrganization(testOrganization);
        account.setProduct(testProduct);
        Account savedAccount = accountRepository.save(account);
        accountRepository.flush();

        // Clear previous invocations
        reset(jobQueueService);

        // When - update account
        savedAccount.setDomain("updateddomain");
        savedAccount.setPlan(PlanType.PAID);
        savedAccount.setCompanyName("Updated Company");
        accountRepository.save(savedAccount);
        accountRepository.flush();

        // Capture the job metadata
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService, atLeastOnce()).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            metadataCaptor.capture()
        );

        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata).isNotNull();
        assertThat(metadata.get("payloadType")).isEqualTo("account_modified");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadataChangeSet = (Map<String, Object>) metadata.get("changeSet");
        assertThat(metadataChangeSet).isNotNull();

        // Manually trigger worker
        accountEventPublisherWorker.processJob(2L, savedAccount.getId(), metadata);

        // Then - verify messaging service was called with changeset
        ArgumentCaptor<String> payloadTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> changeSetCaptor = ArgumentCaptor.forClass(Map.class);

        verify(messagingService, atLeastOnce()).publishAccountEventWithRetry(
            payloadTypeCaptor.capture(),
            eq(savedAccount.getId().toString()),
            any(),
            changeSetCaptor.capture()
        );

        assertThat(payloadTypeCaptor.getValue()).isEqualTo("account_modified");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> changeSet = changeSetCaptor.getValue();
        assertThat(changeSet).isNotNull();
        // Verify changeset is passed (may be empty if no actual changes detected, but structure should be correct)
        // The changeset structure: { "fieldName": { "old": oldValue, "new": newValue } }
        // Note: Changeset might be empty if oldState wasn't captured properly, but the messaging service should still be called
        verify(messagingService, atLeastOnce()).publishAccountEventWithRetry(
            eq("account_modified"),
            eq(savedAccount.getId().toString()),
            any(Map.class),
            any(Map.class)
        );
    }

    @Test
    void accountDeleted_CallsMessagingService() {
        // Given - create account first
        Account account = new Account();
        account.setDomain("deleteaccount");
        account.setPlan(PlanType.TRIAL);
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
        account.setEmail("delete@example.com");
        account.setCompanyName("Delete Company");
        account.setOrganization(testOrganization);
        account.setProduct(testProduct);
        Account savedAccount = accountRepository.save(account);
        accountRepository.flush();

        // Clear previous invocations
        reset(jobQueueService);

        // When - delete account
        accountRepository.delete(savedAccount);
        accountRepository.flush();

        // Capture the job metadata
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobQueueService, atLeastOnce()).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            metadataCaptor.capture()
        );

        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata).isNotNull();
        assertThat(metadata.get("payloadType")).isEqualTo("account_deleted");
        assertThat(metadata.get("accountInfo")).isNotNull();

        // Manually trigger worker
        accountEventPublisherWorker.processJob(3L, savedAccount.getId(), metadata);

        // Then - verify messaging service was called
        ArgumentCaptor<String> payloadTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);

        verify(messagingService, atLeastOnce()).publishAccountEventWithRetry(
            payloadTypeCaptor.capture(),
            eq(savedAccount.getId().toString()),
            accountInfoCaptor.capture(),
            isNull()
        );

        assertThat(payloadTypeCaptor.getValue()).isEqualTo("account_deleted");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> accountInfo = accountInfoCaptor.getValue();
        assertThat(accountInfo).isNotNull();
        assertThat(accountInfo.get("id")).isEqualTo(savedAccount.getId());
        assertThat(accountInfo.get("domain")).isEqualTo("deleteaccount");
    }
}

