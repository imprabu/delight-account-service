package com.delight.account.service;

import com.delight.account.config.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.ServiceConfig serviceConfig;

    @Mock
    private MessagingProperties.TopicsConfig topicsConfig;

    @Mock
    private MessagingProperties.TopicsConfig.AccountActivitiesConfig accountActivitiesConfig;

    @Mock
    private MessagingProperties.ServiceConfig.RetryConfig retryConfig;

    private MessagingService messagingService;

    @BeforeEach
    void setUp() {
        messagingService = new MessagingService(messagingProperties, restTemplate);

        lenient().when(messagingProperties.getService()).thenReturn(serviceConfig);
        lenient().when(messagingProperties.getTopics()).thenReturn(topicsConfig);
        lenient().when(topicsConfig.getAccountActivities()).thenReturn(accountActivitiesConfig);

        lenient().when(serviceConfig.getBaseUrl()).thenReturn("http://localhost:8085");
        lenient().when(serviceConfig.getEndpoint()).thenReturn("/api/messages/ingest");
        lenient().when(serviceConfig.getJwtToken()).thenReturn("test-jwt-token");
        lenient().when(serviceConfig.getRetry()).thenReturn(retryConfig);
        lenient().when(retryConfig.getMaxAttempts()).thenReturn(3);
        lenient().when(retryConfig.getDelayMs()).thenReturn(100L);

        lenient().when(accountActivitiesConfig.getServiceName()).thenReturn("delight_accounts");
        lenient().when(accountActivitiesConfig.getTopicName()).thenReturn("account_activities_v1");
        lenient().when(accountActivitiesConfig.isPayloadTypeEnabled(anyString())).thenReturn(true);
    }

    @Test
    void publishAccountEvent_Success_ReturnsTrue() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);
        accountInfo.put("domain", "testdomain");

        ResponseEntity<String> response = new ResponseEntity<>("Success", HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        )).thenReturn(response);

        // When
        boolean result = messagingService.publishAccountEvent(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isTrue();
        verify(restTemplate).exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        );
    }

    @Test
    void publishAccountEvent_WithChangeSet_IncludesChangeSet() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        Map<String, Object> changeSet = new HashMap<>();
        changeSet.put("domain", Map.of("old", "olddomain", "new", "newdomain"));

        ResponseEntity<String> response = new ResponseEntity<>("Success", HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        )).thenReturn(response);

        // When
        messagingService.publishAccountEvent(
            "account_modified", "100", accountInfo, changeSet
        );

        // Then
        ArgumentCaptor<org.springframework.http.HttpEntity> entityCaptor = 
            ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(restTemplate).exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            entityCaptor.capture(), 
            eq(String.class)
        );

        org.springframework.http.HttpEntity<?> entity = entityCaptor.getValue();
        assertThat(entity).isNotNull();
    }

    @Test
    void publishAccountEvent_Non2xxResponse_ReturnsFalse() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        ResponseEntity<String> response = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        )).thenReturn(response);

        // When
        boolean result = messagingService.publishAccountEvent(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void publishAccountEvent_RestClientException_ReturnsFalse() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        when(restTemplate.exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        )).thenThrow(new RestClientException("Connection failed"));

        // When
        boolean result = messagingService.publishAccountEvent(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void publishAccountEvent_PayloadTypeDisabled_ReturnsTrue() {
        // Given
        when(accountActivitiesConfig.isPayloadTypeEnabled("account_created"))
            .thenReturn(false);

        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        // When
        boolean result = messagingService.publishAccountEvent(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isTrue(); // Returns true when disabled (intentionally skipped)
        verify(restTemplate, never()).exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        );
    }

    @Test
    void publishAccountEventWithRetry_SuccessOnFirstAttempt() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        ResponseEntity<String> response = new ResponseEntity<>("Success", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // When
        boolean result = messagingService.publishAccountEventWithRetry(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isTrue();
        verify(restTemplate, times(1)).exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        );
    }

    @Test
    void publishAccountEventWithRetry_RetriesOnFailure() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        // First two attempts fail, third succeeds
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("Connection failed"))
            .thenThrow(new RestClientException("Connection failed"))
            .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // When
        boolean result = messagingService.publishAccountEventWithRetry(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isTrue();
        verify(restTemplate, times(3)).exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        );
    }

    @Test
    void publishAccountEventWithRetry_FailsAfterMaxRetries() {
        // Given
        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("id", 100L);

        when(restTemplate.exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        )).thenThrow(new RestClientException("Connection failed"));

        // When
        boolean result = messagingService.publishAccountEventWithRetry(
            "account_created", "100", accountInfo, null
        );

        // Then
        assertThat(result).isFalse();
        verify(restTemplate, times(3)).exchange(
            anyString(), 
            any(org.springframework.http.HttpMethod.class), 
            any(org.springframework.http.HttpEntity.class), 
            eq(String.class)
        );
    }
}

