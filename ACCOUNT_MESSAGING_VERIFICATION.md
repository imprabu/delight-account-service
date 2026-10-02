# Account Messaging to /ingest Endpoint - Verification Summary

## Overview
This document summarizes the verification of account create and modify operations sending messages to the `/api/messages/ingest` endpoint.

## Configuration Verification

### 1. Messaging Service Endpoint
**Location**: `src/main/resources/messaging-config.yaml`

```yaml
messaging:
  service:
    base-url: ${MESSAGING_SERVICE_URL:http://localhost:8085}
    endpoint: /api/messages/ingest  ✅ CORRECT
    jwt-token: ${MESSAGING_JWT_TOKEN:...}
    timeout-ms: ${MESSAGING_TIMEOUT_MS:5000}
    retry:
      max-attempts: ${MESSAGING_RETRY_MAX_ATTEMPTS:3}
      delay-ms: ${MESSAGING_RETRY_DELAY_MS:1000}
```

**Full URL**: `http://localhost:8085/api/messages/ingest` (or from `MESSAGING_SERVICE_URL` env var)

### 2. Batch Job Configuration
**Location**: `src/main/resources/application.properties`

```properties
# Account Event Publish Job Queue Configuration
app.jobrunr.account-event-publish.enabled=${JOBRUNR_ACCOUNT_EVENT_PUBLISH_ENABLED:true}  ✅ ENABLED
app.jobrunr.account-event-publish.worker-count=5
app.jobrunr.account-event-publish.poll-interval-seconds=15
app.jobrunr.account-event-publish.delete-succeeded-jobs-after-hours=24
app.jobrunr.account-event-publish.permanently-delete-jobs-after-hours=72
app.jobrunr.account-event-publish.max-retries=3
app.jobrunr.account-event-publish.retry-delay-seconds=60
```

### 3. Payload Types Configuration
**Location**: `src/main/resources/messaging-config.yaml`

```yaml
payload-types:
  - name: account_created
    enabled: ${MESSAGING_ACCOUNT_CREATED_ENABLED:true}  ✅ ENABLED
  - name: account_modified
    enabled: ${MESSAGING_ACCOUNT_MODIFIED_ENABLED:true}  ✅ ENABLED
  - name: account_deleted
    enabled: ${MESSAGING_ACCOUNT_DELETED_ENABLED:true}
```

## Flow Architecture

### Account Creation Flow
1. **Trigger**: Account created via `/account/signup` endpoint
2. **Entity Listener**: `AccountEventEntityListener.onAccountCreated()` is triggered
3. **Job Enqueue**: Job of type `ACCOUNT_EVENT_PUBLISH` is enqueued with metadata:
   ```java
   metadata.put("payloadType", "account_created");
   ```
4. **Worker Processing**: `AccountEventPublisherWorker.processJob()` processes the job:
   - Fetches account from database
   - Builds account info payload
   - Calls `MessagingService.publishAccountEventWithRetry()`
5. **Message Publishing**: `MessagingService` sends POST request to `/api/messages/ingest`

### Account Modification Flow
1. **Trigger**: Account updated (via any update operation)
2. **Pre-Update**: `AccountEventEntityListener.onPreUpdate()` captures old state
3. **Post-Update**: `AccountEventEntityListener.onAccountUpdated()` is triggered
4. **Job Enqueue**: Job of type `ACCOUNT_EVENT_PUBLISH` is enqueued with metadata:
   ```java
   metadata.put("payloadType", "account_modified");
   metadata.put("changeSet", changeSet);  // Contains old/new values for changed fields
   ```
5. **Worker Processing**: `AccountEventPublisherWorker.processJob()` processes the job:
   - Fetches account from database
   - Builds account info payload
   - Uses changeset from metadata
   - Calls `MessagingService.publishAccountEventWithRetry()`
6. **Message Publishing**: `MessagingService` sends POST request to `/api/messages/ingest`

## Key Components

### 1. AccountEventEntityListener
**Location**: `src/main/java/com/delight/account/listener/AccountEventEntityListener.java`
- JPA Entity Listener that triggers on account create/update/delete
- Enqueues jobs to JobRunr queue
- Builds changeset for update events

### 2. AccountEventPublisherWorker
**Location**: `src/main/java/com/delight/account/service/job/worker/AccountEventPublisherWorker.java`
- JobRunr worker that processes `ACCOUNT_EVENT_PUBLISH` jobs
- Fetches account data from database
- Builds payload and calls MessagingService

### 3. MessagingService
**Location**: `src/main/java/com/delight/account/service/MessagingService.java`
- Publishes events to messaging service
- Sends POST requests to `/api/messages/ingest`
- Implements retry logic (3 attempts with 1 second delay)

## Testing

### Test Script
**Location**: `test-account-messaging.sh`

The test script performs:
1. Account creation via `/account/signup` endpoint
2. Account modification via `/test/account/{id}/update` endpoint
3. Log verification for message publishing

**Usage**:
```bash
./test-account-messaging.sh
```

### Manual Testing

#### Test Account Creation
```bash
curl -X POST http://localhost:8080/account/signup \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Test Company",
    "emailAddress": "test@example.com",
    "industryType": "Technology",
    "firstName": "Test",
    "lastName": "User"
  }'
```

#### Test Account Modification
```bash
curl -X POST http://localhost:8080/test/account/1/update \
  -H "Content-Type: application/json"
```

### Log Verification

Check logs for successful message publishing:
```bash
# View all messaging-related logs
tail -f logs/application.log | grep -E "(MESSAGING|ACCOUNT-EVENT)"

# Check for successful publication
grep "✅.*MESSAGING.*Successfully published" logs/application.log

# Check for /ingest endpoint calls
grep "MESSAGING.*/api/messages/ingest" logs/application.log
```

Expected log messages:
- `📤 [ACCOUNT-EVENT] Account created: {accountId}`
- `📤 [ACCOUNT-EVENT-WORKER] Processing account event job {jobId} for account {accountId}`
- `📤 [MESSAGING] Publishing account_created event for account {accountId} to {url}/api/messages/ingest`
- `✅ [MESSAGING] Successfully published account_created event for account {accountId}`

## Verification Checklist

- [x] Endpoint configured correctly: `/api/messages/ingest`
- [x] Batch job `ACCOUNT_EVENT_PUBLISH` is enabled
- [x] `account_created` payload type is enabled
- [x] `account_modified` payload type is enabled
- [x] Entity listener triggers on account create
- [x] Entity listener triggers on account update
- [x] Worker processes jobs correctly
- [x] MessagingService sends to correct endpoint
- [x] Retry logic is configured (3 attempts, 1s delay)

## Potential Issues

1. **Messaging Service Not Running**: If the messaging service at `http://localhost:8085` is not running, messages will fail after retries
2. **JobRunr Not Processing**: Ensure JobRunr workers are running and processing jobs
3. **Payload Type Disabled**: Check environment variables:
   - `MESSAGING_ACCOUNT_CREATED_ENABLED=true`
   - `MESSAGING_ACCOUNT_MODIFIED_ENABLED=true`
4. **Database Connection**: Worker fetches account from DB, ensure DB is accessible

## Environment Variables

Key environment variables for configuration:
- `MESSAGING_SERVICE_URL`: Base URL for messaging service (default: `http://localhost:8085`)
- `MESSAGING_JWT_TOKEN`: JWT token for authentication
- `MESSAGING_ACCOUNT_CREATED_ENABLED`: Enable/disable account_created events (default: `true`)
- `MESSAGING_ACCOUNT_MODIFIED_ENABLED`: Enable/disable account_modified events (default: `true`)
- `JOBRUNR_ACCOUNT_EVENT_PUBLISH_ENABLED`: Enable/disable batch job processing (default: `true`)







