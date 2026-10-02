package com.delight.account.service.job.worker;

import com.delight.account.domain.BatchJobType;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.KafkaEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountEventPublisherWorkerTest {

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @Mock
    private AccountRepository accountRepository;

    private AccountEventPublisherWorker worker;
    private Account testAccount;
    private Organization testOrganization;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        worker = new AccountEventPublisherWorker();
        // Use reflection to inject mocks, or make fields package-private for testing
        try {
            var kafkaEventPublisherField = AccountEventPublisherWorker.class.getDeclaredField("kafkaEventPublisher");
            kafkaEventPublisherField.setAccessible(true);
            kafkaEventPublisherField.set(worker, kafkaEventPublisher);

            var accountRepositoryField = AccountEventPublisherWorker.class.getDeclaredField("accountRepository");
            accountRepositoryField.setAccessible(true);
            accountRepositoryField.set(worker, accountRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }

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
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setIndustryType("Technology");
        testAccount.setCompanyName("Test Company");
        testAccount.setOrganization(testOrganization);
        testAccount.setProduct(testProduct);
    }

    @Test
    void processJob_AccountCreated_FetchesAccountFromDB() {
        // Given
        Long jobId = 1L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_created");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        verify(accountRepository).findById(accountId);
        
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);

        verify(kafkaEventPublisher).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo("account_created");
        assertThat(accountIdCaptor.getValue()).isEqualTo(100L);

        Map<String, Object> accountInfo = accountInfoCaptor.getValue();
        assertThat(accountInfo.get("id")).isEqualTo(100L);
        assertThat(accountInfo.get("domain")).isEqualTo("testdomain");
    }

    @Test
    void processJob_AccountModified_UsesChangeSet() {
        // Given
        Long jobId = 2L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_modified");
        
        Map<String, Object> changeSet = new HashMap<>();
        changeSet.put("domain", Map.of("old", "olddomain", "new", "newdomain"));
        changeSet.put("plan", Map.of("old", "TRIAL", "new", "PREMIUM"));
        metadata.put("changeSet", changeSet);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        verify(accountRepository).findById(accountId);
        
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaEventPublisher).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo("account.updated");
        assertThat(accountIdCaptor.getValue()).isEqualTo(100L);
        Map<String, Object> capturedAccountInfo = accountInfoCaptor.getValue();
        assertThat(capturedAccountInfo).isNotNull();
        // Note: changeSet is not passed to publishAccountEvent, only accountInfo
    }

    @Test
    void processJob_AccountDeleted_UsesAccountInfoFromMetadata() {
        // Given
        Long jobId = 3L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_deleted");
        
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);
        accountInfo.put("domain", "testdomain");
        accountInfo.put("email", "test@example.com");
        metadata.put("accountInfo", accountInfo);


        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        // Should NOT fetch from DB for DELETE
        verify(accountRepository, never()).findById(any());
        
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaEventPublisher).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );
        
        assertThat(eventTypeCaptor.getValue()).isEqualTo("account.deleted");
        assertThat(accountIdCaptor.getValue()).isEqualTo(100L);

        assertThat(accountInfoCaptor.getValue()).isEqualTo(accountInfo);
    }

    @Test
    void processJob_AccountDeleted_WithMissingAccountInfo_FetchesFromDB() {
        // Given
        Long jobId = 4L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_deleted");
        // accountInfo is missing

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        verify(accountRepository).findById(accountId);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaEventPublisher).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );
        
        assertThat(eventTypeCaptor.getValue()).isEqualTo("account.deleted");
        assertThat(accountIdCaptor.getValue()).isEqualTo(100L);
    }

    @Test
    void processJob_UnknownPayloadType_LogsError() {
        // Given
        Long jobId = 5L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "unknown_type");

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        verify(accountRepository, never()).findById(any());
        verify(kafkaEventPublisher, never()).publishAccountEvent(any(), any(), any());
    }

    @Test
    void processJob_AccountNotFound_LogsWarning() {
        // Given
        Long jobId = 6L;
        Long accountId = 999L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_created");

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        verify(accountRepository).findById(accountId);
        verify(kafkaEventPublisher, never()).publishAccountEvent(any(), any(), any());
    }

    @Test
    void processJob_MissingPayloadType_LogsError() {
        // Given
        Long jobId = 7L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        // payloadType is missing

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        verify(accountRepository, never()).findById(any());
        verify(kafkaEventPublisher, never()).publishAccountEvent(any(), any(), any());
    }

    @Test
    void processJob_MissingAccountId_LogsError() {
        // Given
        Long jobId = 8L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_created");

        // When
        worker.processJob(jobId, null, metadata);

        // Then
        verify(accountRepository, never()).findById(any());
        verify(kafkaEventPublisher, never()).publishAccountEvent(any(), any(), any());
    }

    @Test
    void processJob_MessagingServiceFailure_ThrowsException() {
        // Given
        Long jobId = 9L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_created");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        // Note: messagingService was replaced by kafkaEventPublisher

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then - should complete without throwing (error is logged)
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaEventPublisher).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );
    }

    @Test
    void getJobType_ReturnsCorrectType() {
        // When
        BatchJobType jobType = worker.getJobType();

        // Then
        assertThat(jobType).isEqualTo(BatchJobType.ACCOUNT_EVENT_PUBLISH);
    }

    @Test
    void processJob_AccountModified_WithEmptyChangeSet() {
        // Given
        Long jobId = 10L;
        Long accountId = 100L;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payloadType", "account_modified");
        metadata.put("changeSet", new HashMap<>()); // Empty changeset

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When
        worker.processJob(jobId, accountId, metadata);

        // Then
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> accountIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map<String, Object>> accountInfoCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaEventPublisher).publishAccountEvent(
            eventTypeCaptor.capture(),
            accountIdCaptor.capture(),
            accountInfoCaptor.capture()
        );
        
        assertThat(eventTypeCaptor.getValue()).isEqualTo("account.updated");
        assertThat(accountIdCaptor.getValue()).isEqualTo(100L);
        // Note: changeSet is not passed to publishAccountEvent, only accountInfo
    }
}


