package com.delight.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for messaging service integration.
 * Loads from messaging-config.yaml and application.properties.
 */
@Component
@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {

    private ServiceConfig service = new ServiceConfig();
    private TopicsConfig topics = new TopicsConfig();

    public ServiceConfig getService() {
        return service;
    }

    public void setService(ServiceConfig service) {
        this.service = service;
    }

    public TopicsConfig getTopics() {
        return topics;
    }

    public void setTopics(TopicsConfig topics) {
        this.topics = topics;
    }

    public static class ServiceConfig {
        private String baseUrl = "http://localhost:8085";
        private String endpoint = "/api/messages/ingest";
        private String jwtToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        private int timeoutMs = 5000;
        private RetryConfig retry = new RetryConfig();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getJwtToken() {
            return jwtToken;
        }

        public void setJwtToken(String jwtToken) {
            this.jwtToken = jwtToken;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public RetryConfig getRetry() {
            return retry;
        }

        public void setRetry(RetryConfig retry) {
            this.retry = retry;
        }

        public static class RetryConfig {
            private int maxAttempts = 3;
            private long delayMs = 1000;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public long getDelayMs() {
                return delayMs;
            }

            public void setDelayMs(long delayMs) {
                this.delayMs = delayMs;
            }
        }
    }

    public static class TopicsConfig {
        private AccountActivitiesConfig accountActivities = new AccountActivitiesConfig();

        public AccountActivitiesConfig getAccountActivities() {
            return accountActivities;
        }

        public void setAccountActivities(AccountActivitiesConfig accountActivities) {
            this.accountActivities = accountActivities;
        }

        public static class AccountActivitiesConfig {
            private String topicName = "account_actvities_v1";
            private String serviceName = "delight_accounts";
            private List<PayloadTypeConfig> payloadTypes = List.of(
                new PayloadTypeConfig("account_created", true),
                new PayloadTypeConfig("account_modified", true),
                new PayloadTypeConfig("account_deleted", true)
            );

            public String getTopicName() {
                return topicName;
            }

            public void setTopicName(String topicName) {
                this.topicName = topicName;
            }

            public String getServiceName() {
                return serviceName;
            }

            public void setServiceName(String serviceName) {
                this.serviceName = serviceName;
            }

            public List<PayloadTypeConfig> getPayloadTypes() {
                return payloadTypes;
            }

            public void setPayloadTypes(List<PayloadTypeConfig> payloadTypes) {
                this.payloadTypes = payloadTypes;
            }

            public PayloadTypeConfig getPayloadType(String name) {
                return payloadTypes.stream()
                    .filter(pt -> pt.getName().equals(name))
                    .findFirst()
                    .orElse(null);
            }

            public boolean isPayloadTypeEnabled(String name) {
                PayloadTypeConfig config = getPayloadType(name);
                return config != null && config.isEnabled();
            }
        }

        public static class PayloadTypeConfig {
            private String name;
            private boolean enabled = true;

            public PayloadTypeConfig() {
            }

            public PayloadTypeConfig(String name, boolean enabled) {
                this.name = name;
                this.enabled = enabled;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }
}


