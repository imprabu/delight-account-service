package com.delight.account.integration;

import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.OrganizationRepository;
import com.delight.account.repository.ProductRepository;
import com.delight.account.service.KafkaEventPublisher;
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
    void accountCreated_CallsMessagingService() {
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

        // Then - verify Kafka event publisher was called
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);

        verify(kafkaEventPublisher, atLeastOnce()).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo("account_created");
        assertThat(accountIdCaptor.getValue()).isEqualTo(savedAccount.getId());
        
        Map<String, Object> accountInfo = accountInfoCaptor.getValue();
        assertThat(accountInfo).isNotNull();
        assertThat(accountInfo.get("id")).isEqualTo(savedAccount.getId());
        assertThat(accountInfo.get("domain")).isEqualTo("newaccount");
    }

    @Test
    void accountUpdated_CallsMessagingServiceWithChangeSet() {
        // Given - create account first
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

        // Then - verify Kafka event publisher was called
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);

        verify(kafkaEventPublisher, atLeastOnce()).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo("account_modified");
        assertThat(accountIdCaptor.getValue()).isEqualTo(savedAccount.getId());
        
        Map<String, Object> accountInfo = accountInfoCaptor.getValue();
        assertThat(accountInfo).isNotNull();
    }

    @Test
    void accountDeleted_CallsMessagingService() {
        // Given - create account first
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

        // Then - verify Kafka event publisher was called
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);

        verify(kafkaEventPublisher, atLeastOnce()).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo("account_deleted");
        assertThat(accountIdCaptor.getValue()).isEqualTo(savedAccount.getId());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> accountInfo = accountInfoCaptor.getValue();
        assertThat(accountInfo).isNotNull();
        assertThat(accountInfo.get("id")).isEqualTo(savedAccount.getId());
        assertThat(accountInfo.get("domain")).isEqualTo("deleteaccount");
    }
}

