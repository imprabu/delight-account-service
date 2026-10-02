package com.delight.account.integration;

import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.OrganizationRepository;
import com.delight.account.repository.ProductRepository;
import com.delight.account.service.KafkaEventPublisher;
import com.delight.account.service.job.JobQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for Account event publishing flow.
 * Tests the complete flow from entity listener to worker execution.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountEventPublishingIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductRepository productRepository;

    @org.springframework.boot.test.mock.mockito.SpyBean
    private JobQueueService jobQueueService;

    @MockBean
    private KafkaEventPublisher kafkaEventPublisher;

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
        testProduct.setCode("CHAT");
        testProduct.setName("CHAT Product");
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void accountCreated_TriggersEventPublishing() {
        // Given

        Account account = new Account();
        account.setDomain("newaccount");
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
        account.setCompanyName("New Company");
        account.setOrganization(testOrganization);
        account.setProduct(testProduct);

        // When
        Account savedAccount = accountRepository.save(account);
        accountRepository.flush(); // Ensure entity listener is triggered

        // Then - verify job was enqueued (since JobRunr is disabled in tests, 
        // we verify the enqueueJob was called, not the actual processing)
        verify(jobQueueService, atLeastOnce()).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            argThat(metadata -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = (Map<String, Object>) metadata;
                return "account_created".equals(meta.get("payloadType"));
            })
        );
    }

    @Test
    void accountUpdated_TriggersEventPublishingWithChangeSet() {
        // Given

        // Create account first
        Account account = new Account();
        account.setDomain("updateaccount");
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
        account.setCompanyName("Update Company");
        account.setOrganization(testOrganization);
        account.setProduct(testProduct);
        Account savedAccount = accountRepository.save(account);
        accountRepository.flush();

        // Clear previous invocations
        reset(jobQueueService);

        // When - update account
        savedAccount.setDomain("updateddomain");
        savedAccount.setCompanyName("Updated Company");
        accountRepository.save(savedAccount);
        accountRepository.flush();

        // Then - verify job was enqueued with changeset
        verify(jobQueueService, atLeastOnce()).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            argThat(metadata -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = (Map<String, Object>) metadata;
                if (!"account_modified".equals(meta.get("payloadType"))) {
                    return false;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> changeSet = (Map<String, Object>) meta.get("changeSet");
                return changeSet != null;
            })
        );
    }

    @Test
    void accountDeleted_TriggersEventPublishing() {
        // Given

        // Create account first
        Account account = new Account();
        account.setDomain("deleteaccount");
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
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

        // Then - verify job was enqueued with accountInfo
        verify(jobQueueService, atLeastOnce()).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            argThat(metadata -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = (Map<String, Object>) metadata;
                if (!"account_deleted".equals(meta.get("payloadType"))) {
                    return false;
                }
                // DELETE should contain accountInfo
                @SuppressWarnings("unchecked")
                Map<String, Object> accountInfo = (Map<String, Object>) meta.get("accountInfo");
                return accountInfo != null && accountInfo.containsKey("id");
            })
        );
    }

    @Test
    void multipleAccountUpdates_TriggersMultipleEvents() {
        // Given

        Account account = new Account();
        account.setDomain("multitest");
        account.setStatus(AccountStatus.ACTIVE);
        account.setIndustryType("Technology");
        account.setCompanyName("Multi Company");
        account.setOrganization(testOrganization);
        account.setProduct(testProduct);
        Account savedAccount = accountRepository.save(account);
        accountRepository.flush();

        reset(jobQueueService);

        // When - multiple updates
        savedAccount.setDomain("update1");
        accountRepository.save(savedAccount);
        accountRepository.flush();

        accountRepository.save(savedAccount);
        accountRepository.flush();

        savedAccount.setCompanyName("Updated Name");
        accountRepository.save(savedAccount);
        accountRepository.flush();

        // Then - verify multiple jobs were enqueued
        verify(jobQueueService, atLeast(3)).enqueueJob(
            any(com.delight.account.domain.BatchJobType.class),
            eq(savedAccount.getId()),
            eq(savedAccount.getId()),
            argThat(metadata -> 
                "account_modified".equals(((Map<String, Object>) metadata).get("payloadType"))
            )
        );
    }
}

