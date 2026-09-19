# IAM + MessageService — Architecture, LLD, User Stories & Development Plan

| Field | Value |
|-------|-------|
| Status | Draft |
| Modules | **IAM** (`delight-account-service`), **MessageService** (evolve `delight-telemetry-poc` → `delight-message-service`) |
| Related | Platform plan, `HLD-STP-WTP-IAM-CHANGES.md`, MQTT UV500 POC |
| Out of scope here | Customer Front App (UI only), PCB adapter, full alerts product |

---

## 1. Goals and non-goals

### Goals

1. Cloud-deployable IAM and MessageService using **managed** DB / MQTT / queues where possible.
2. **Managed PostgreSQL for all services** (one engine family; separate databases per service).
3. Clear service boundaries: IAM = identity & authz; MessageService = IoT ingest, registry, timeseries.
4. Every gateway payload authenticated and authorized before storage.
5. UV500 never talks to cloud; TRB142 (or HTTPS gateway path) is the only edge publisher.
6. Front App (later) consumes IAM for login and MessageService for data — never owns authz.

### Non-goals

- Department / Group hierarchy.
- Self-hosting production Postgres / Mosquitto / Kafka.
- Keeping TiDB/MySQL as the long-term IAM database.
- Shared single database across services (still **one DB instance family**, **separate DBs/schemas**).
- PCB submission, full alert escalation (separate later).
- Modbus writes over the internet.

---

## 2. System architecture

### 2.1 Logical view

```text
┌─────────────────────┐     Channel JWT      ┌──────────────────────────┐
│  Customer Front App │◄────────────────────►│  IAM                     │
│  (later module)     │     user JWT         │  delight-account-service │
└─────────┬───────────┘                      │  Managed PostgreSQL      │
          │ user JWT                         │  (db: delight_accounts)  │
          ▼                                  │  Redis, Kafka            │
┌─────────────────────┐                      └────────────▲─────────────┘
│  MessageService     │───────────────────────────────────┘ Channel JWT
│  registry + ingest  │
│  + timeseries query │
└─────────┬───────────┘
          │
          ▼
 Managed PostgreSQL + Timescale extension
 (db: delight_telemetry)     + managed MQTT / HTTPS
```

**Database standard (locked):** Managed PostgreSQL for every service. Each service owns its own database (no cross-DB joins). MessageService enables the **TimescaleDB extension** on its Postgres for hypertables and continuous aggregates.

```text
UV500 --RS-232 Modbus RO--> TRB142 --MQTT mTLS or HTTPS--> MessageService
```

### 2.2 Module responsibilities

| Concern | IAM | MessageService |
|---------|-----|----------------|
| Org / account / user / role | Owns | Consumes via Channel APIs |
| Site for **access control** | Owns (`sites` + membership scope) | References `accountId` + `siteCode` (sync from IAM events) |
| Gateway / analyser / Modbus profile | No | Owns |
| Device X.509 / IoT thing | No (audit correlation only) | Owns issue/revoke + cloud IoT adapter |
| Measurements / raw telemetry | No | Owns |
| Session / privilege / audit of humans | Owns | Enforces using IAM session + privileges |

**Site ownership rule (important):**

- IAM `sites` = authorization identity (`code` stable; used in JWT/siteScope and MQTT `{siteId}`).
- MessageService keeps a **local site projection** (`account_id`, `code`, optional `iam_site_id`) plus gateways/analysers. After IAM Phase 1, create site in IAM first (or MessageService calls IAM / consumes `site.created.v1`). Until then, POC may create sites only in MessageService and treat `code` as provisional.

### 2.3 Cloud / managed topology (target)

| Layer | Dev | Production (preferred) |
|-------|-----|------------------------|
| IAM runtime | Local JAR | Container on E2E / AWS / Azure |
| IAM DB | Local Postgres (`delight_accounts`) | **Managed PostgreSQL** (RDS / Azure Flexible Server / E2E managed PG / Neon / etc.) |
| MessageService runtime | Local JAR | Same as above |
| MessageService DB | Podman Timescale (Postgres + extension) | **Same managed PostgreSQL provider**, separate DB `delight_telemetry` + Timescale extension (or Timescale Cloud) |
| MQTT | Podman Mosquitto | AWS IoT Core / Azure IoT Hub / HTTPS if no MQTT |
| Queue | Local Kafka (IAM) | Managed Kafka / SQS / Service Bus |
| Secrets | env files | Secret manager |
| Object raw archive | JSONB only | + S3 / Blob / S3-compatible |

**Postgres layout**

| Service | Database name | Extensions | Notes |
|---------|---------------|------------|-------|
| IAM | `delight_accounts` | none required | Relational IAM only |
| MessageService | `delight_telemetry` | `timescaledb` | Registry tables + hypertables |
| Future services | own DB each | as needed | Still managed Postgres |

Prefer **one managed Postgres provider/account** with multiple databases (cost + ops simplicity). Do **not** put IAM tables and measurements in the same database.

Apps stay portable: JDBC URLs only; MessageService uses ports for `MqttBrokerAdapter`, `ObjectStore`, `TimeseriesStore`.

### 2.3.1 IAM migration off TiDB/MySQL → PostgreSQL

Today `delight-account-service` uses TiDB via MySQL protocol + Liquibase. Target is managed PostgreSQL.

| Work | Detail |
|------|--------|
| Driver | `mysql-connector-j` → `org.postgresql:postgresql` |
| JDBC URL | `jdbc:postgresql://…/delight_accounts` |
| Liquibase | Dialect/changelog review: `AUTO_RANDOM`, MySQL types, backticks → Postgres |
| JPA | Dialect `PostgreSQLDialect`; validate IDENTITY/sequence strategy |
| Local compose | Replace TiDB with `postgres:16` (or Timescale image unused for IAM) |
| Dual-run | Optional: feature branch migration; dump/transform pilot data if any |

This is an **IAM Phase 0/1 infrastructure story** (see IAM-PG1 below) — required before production cloud deploy.

### 2.4 Trust boundaries

1. **Human** → IAM (user JWT / `hrsession`).
2. **Service** → IAM (`/channel/**` + Channel JWT; later ServiceIdentity).
3. **Gateway** → MessageService / IoT broker (device cert or HTTPS device token) — **never** a user JWT.
4. **Front App** → MessageService with user JWT; MessageService validates via IAM session.

---

## 3. IAM — Low-level design

### 3.1 Package / layer map (existing + new)

```text
com.delight.account
  filter/          TenantFilter, JwtAuthenticationFilter, ChannelAuthFilter
  controller/      accounts/*, channel/*
  service/         Session, Membership, Authorization, (new) Site, ServiceIdentity, Audit
  model/           Organization, Account, User, Membership, Role, Privilege, Session
  repository/
  messaging/       Kafka publishers
```

### 3.2 Domain model (to-be)

```text
Organization
  └── Account
        ├── Site                          [NEW]
        ├── Membership
        │     ├── Role → Privileges
        │     └── MembershipSiteScope     [NEW] ALL_SITES | SELECTED + membership_sites
        ├── ServiceIdentity               [NEW]
        ├── Session / UserCredential
        └── SecurityAuditEvent            [NEW] append-only
```

#### Tables (new / extended)

**`sites`**

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT / UUID | PK |
| account_id | BIGINT | FK |
| code | VARCHAR | Stable external id = MQTT `{siteId}` |
| name | VARCHAR | |
| status | ENUM | ACTIVE / DEACTIVATED |
| timezone | VARCHAR | |
| metadata | JSON | optional |
| created_at / updated_at | timestamptz | |

Unique `(account_id, code)`.

**`membership_site_scopes`**

| Column | Type |
|--------|------|
| membership_id | FK |
| scope_type | ALL_SITES \| SELECTED |

**`membership_sites`**

| Column | Type |
|--------|------|
| membership_id | FK |
| site_id | FK |

**`service_identities`**

| Column | Type | Notes |
|--------|------|-------|
| id | PK | |
| account_id | nullable | null = platform-wide |
| client_id | unique | |
| name | | |
| type | BACKEND_SERVICE \| PROVISIONING \| INTEGRATION | |
| status | ACTIVE \| REVOKED | |
| credential_fingerprint | | never raw secret |
| external_ref | optional | e.g. IoT thing name reference |

**`security_audit_events`** — append-only; no UPDATE/DELETE API.

### 3.3 AuthZ algorithm (pilot — Option A)

```text
1. Resolve subject userId + accountId from session / channel context
2. Casbin: enforce(privilege) on subject
3. If resource has siteId:
     load membership site scope
     allow if ALL_SITES OR siteId ∈ selected sites
4. Else: account-wide privilege only
```

Channel `POST /channel/accounts/authorize/check` today always returns true for `/channel/**` — **must be fixed** so MessageService can trust it.

### 3.4 IAM APIs (target)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/accounts/signup` | public | Create account |
| POST | `/accounts/signin` | public | Session + JWT |
| GET/POST | `/accounts/session` | user | Validate session |
| POST | `/channel/accounts/session` | Channel JWT | S2S session validate |
| POST | `/channel/accounts/authorize/check` | Channel JWT | privilege + optional siteId |
| CRUD | `/accounts/sites` | user + privilege | Site lifecycle |
| PUT | `/accounts/members/{id}/site-scope` | admin | Scope membership |
| CRUD | `/accounts/service-identities` | admin | Machine principals |
| GET | `/accounts/audit` | audit.read | Security audit query |

### 3.5 Events (versioned)

| Event | Consumers |
|-------|-----------|
| `site.created.v1` / `updated.v1` / `deactivated.v1` | MessageService site projection |
| `membership.changed.v1` (+ siteScope) | MessageService / Front App cache |
| `service_identity.*.v1` | MessageService provisioning callers |
| existing `account_*`, `user_*`, `user_role_*` | keep |

### 3.6 Privilege catalog (monitoring)

| Code | Default ORG_ADMIN | ACCOUNT_ADMIN | SITE_OPERATOR |
|------|-------------------|---------------|---------------|
| site.create/read/update/deactivate | Y | Y | read only |
| device.read / provision / credential.rotate | Y | Y | read |
| measurement.read / export | Y | Y | read |
| alert.* | Y | Y | ack + read |
| pcb.* | Y | approve+read | read ledger |
| audit.read | Y | Y | N |

### 3.7 IAM key sequences

**Sign-in**

```text
Client → POST /accounts/signin
  → validate credentials → create Session → JWT + hrsession cookie
  → (audit SIGN_IN_SUCCESS)
```

**Authorize for MessageService**

```text
MessageService → POST /channel/accounts/session { jwtToken }
  ← SessionResponse (accountId, privileges, siteScope)
MessageService → POST /channel/accounts/authorize/check
  { userId, accountId, privilegeCode, siteId? }
  ← { authorized, siteScope }
```

---

## 4. MessageService — Low-level design

### 4.1 Package map (from POC → target)

```text
com.delight.telemetry (rename com.delight.message)
  config/          IAM, MQTT, PKI, Broker, Cloud adapters
  iam/             IamClient (Channel JWT)
  security/        IamSessionFilter (human APIs)
  registry/        Site projection, Gateway, Analyser, Map, Credentials
  pki/             Client cert issue (local CA) / cloud IoT adapter
  mqtt/            Subscriber, ACL/policy sync, PemSsl
  ingest/          TelemetryIngestService, Uv500Decoder, TimeseriesStore, HttpsIngest
  web/             RegistryController, MeasurementQueryController, HttpsIngestController
```

### 4.2 Domain model

```text
Account (ref only: account_id from IAM)
  └── SiteProjection (code aligns with IAM site.code)
        ├── Gateway (TRB142) — MQTT identity
        │     ├── GatewayCredential (fingerprint only)
        │     └── GatewayAnalyserMap → Analyser + read_profile
        └── Analyser (UV500) — no MQTT identity
```

#### Core tables (POC already)

- `sites`, `gateways`, `gateway_credentials`, `analysers`, `gateway_analyser_maps`
- `telemetry_raw` (hypertable), `measurements` (hypertable), `ingest_dedup`, `ingest_events`
- Continuous aggregates `measurements_1min`, `measurements_1hour`

#### Extensions for production

| Addition | Purpose |
|----------|---------|
| `iam_site_id` on sites | Link to IAM site PK |
| `transport` on gateways | MQTT \| HTTPS |
| `iot_thing_name` / `iot_policy_name` | Cloud IoT metadata |
| object_storage_key on telemetry_raw | Optional S3/Blob pointer |
| `device_tokens` | HTTPS ingest short-lived tokens (hashed) |

### 4.3 Ingest pipeline (MQTT and HTTPS)

```text
Transport adapter
  → normalize to IngestEnvelope { topicOrPath, principalCn, clientId, payload, certFingerprint? }
  → TelemetryIngestService.ingest(...)
       checks (same for both transports):
         topic/path shape
         principal == gatewayId
         gateway ACTIVE + credential
         site/account match
         IAM account active
         payload identity fields match path (if present)
         map exists + profileVersion
         messageId idempotency
       → write telemetry_raw
       → decode UV500 profile → measurements
       → ingest_events outcome
```

**Reject / quarantine reasons (stable codes):**  
`UNAUTHENTICATED`, `UNAUTHORIZED_TOPIC`, `UNKNOWN_GATEWAY`, `REVOKED`, `UNMAPPED`, `PAYLOAD_IDENTITY_MISMATCH`, `DUPLICATE`, `OK`

### 4.4 MessageService APIs

#### Human / Front App (user JWT → IAM session)

| Method | Path | Privilege (when IAM ready) |
|--------|------|----------------------------|
| POST/GET | `/v1/sites` | site.create / site.read |
| POST | `/v1/sites/{id}/analysers` | device.provision |
| POST/GET | `/v1/sites/{id}/gateways` | device.provision / device.read |
| POST | `/v1/gateways/{id}/credentials` | device.credential.rotate |
| POST | `/v1/gateways/{id}/map` | device.provision |
| POST | `/v1/gateways/{id}/revoke` | device.credential.rotate |
| GET | `/v1/measurements` | measurement.read |
| GET | `/v1/measurements/rollup` | measurement.read |
| GET | `/v1/ingest-events` | device.read or audit |

#### Device HTTPS fallback

| Method | Path | Auth |
|--------|------|------|
| POST | `/v1/ingest/https` | mTLS client cert **or** `Authorization: Device <token>` |
| Body | same telemetry JSON as MQTT | Identity from cert CN / token binding, not body |

### 4.5 UV500 read profile (bound on map)

- Port A RS-232 Modbus **read-only**
- Registers: status `66`, error `48`, 16 floats from `16`
- Profile JSON versioned (`uv500-v1`); stored on `gateway_analyser_maps.read_profile`

### 4.6 MessageService sequences

**Register gateway**

```text
Admin (user JWT) → MessageService POST /v1/sites/{site}/gateways
  → IAM session validate + (later) authorize device.provision + site scope
  → insert gateway PENDING
Admin → POST /v1/gateways/{id}/credentials
  → issue cert (local CA or AWS IoT CreateKeysAndCertificate)
  → store fingerprint only; return PEM once
  → gateway ACTIVE; update broker policy / IoT policy
Admin → POST /v1/gateways/{id}/map { analyserId, profileVersion }
```

**MQTT message**

```text
TRB142 mTLS → Managed MQTT / Mosquitto
  → MessageService subscriber (ingest cert)
  → TelemetryIngestService
  → Timescale write
```

---

## 5. Cross-service contracts

### 5.1 Tenant / site IDs

| Field | Source of truth | Used in |
|-------|-----------------|---------|
| `tenantId` / `accountId` | IAM `accounts.id` | MQTT path, all MessageService rows |
| `siteId` / `siteCode` | IAM `sites.code` (target) | MQTT path, MessageService sites.code |
| `gatewayId` | MessageService `gateways.gateway_code` | MQTT path, cert CN |

### 5.2 Telemetry payload (minimum)

```json
{
  "messageId": "uuid",
  "observedAt": "2026-09-19T12:00:00Z",
  "sequence": 1,
  "profileVersion": "uv500-v1",
  "connectivity": { "rs232": "UP", "lte": "UP" },
  "statusRegister": 0,
  "errorStatus": 0,
  "measurements": { "COD": 12.5, "BOD": 3.2 }
}
```

Optional identity fields (`tenantId`, `siteId`, `gatewayId`) must match path if present.

### 5.3 Failure modes

| Failure | Behavior |
|---------|----------|
| IAM down | Human APIs fail closed; ingest may use short TTL account-active cache then fail closed |
| Broker down | Gateway buffers (device-side); HTTPS fallback if configured |
| Duplicate messageId | Idempotent accept (DUPLICATE) |
| Unmapped gateway | Store raw + QUARANTINED UNMAPPED; no measurements |

---

## 6. User stories

Format: **ID — As a … I want … so that …**  
Acceptance criteria are testable.

### 6.1 IAM — Epic A: Hardening & baseline

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| IAM-A1 | As a platform engineer, I want `/test/**` mutation removed so accounts cannot be changed anonymously | No public test update controller; security config has no `/test/**` permitAll; regression test 401/404 |
| IAM-A2 | As a security owner, I want production session cookies `Secure` + explicit `SameSite` | Config flag; prod Secure=true; documented local override |
| IAM-A3 | As a developer, I want secrets out of tracked env samples | Sample files use placeholders only; README points to secret manager |
| IAM-A4 | As a product owner, I want RAG UI removed from account frontend | Bot/knowledge/intents/testing routes gone; build green |
| IAM-PG1 | As a platform engineer, I migrate IAM to managed PostgreSQL for all environments | Local compose Postgres; Liquibase + JPA green; channel/session tests pass; prod uses managed PG `delight_accounts` |

### 6.2 IAM — Epic B: Sites & site-scoped access

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| IAM-B1 | As an account admin, I can create a site with stable `code` | POST creates site; unique per account; event `site.created.v1` |
| IAM-B2 | As an account admin, I can deactivate a site | Status DEACTIVATED; cannot assign new scopes to it; event published |
| IAM-B3 | As an account admin, I can set a member to ALL_SITES or SELECTED sites | PUT site-scope; persisted; membership event includes scope |
| IAM-B4 | As a site operator, I can only access my sites | authorize/check with siteId returns false for out-of-scope site |
| IAM-B5 | As MessageService, I receive site events to sync projection | Consumer upserts local site by accountId+code |

### 6.3 IAM — Epic C: Privileges & authorize API

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| IAM-C1 | As a developer, I have monitoring privilege codes seeded | Liquibase seeds site/device/measurement/alert/pcb/audit codes |
| IAM-C2 | As MessageService, authorize/check actually evaluates Casbin | Fix channel always-true bug; unit+integration tests for allow/deny |
| IAM-C3 | As MessageService, authorize/check accepts optional siteId | Response includes authorized + siteScope |
| IAM-C4 | As session client, session payload includes siteScope | SessionResponse has scope type + site ids (or fetch flag) |

### 6.4 IAM — Epic D: Service identities & audit

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| IAM-D1 | As a platform admin, I can create a service identity for MessageService | client_id + credential issued once; fingerprint stored |
| IAM-D2 | As MessageService, I can authenticate to IAM with service credential (Phase 2) | Replaces shared Channel HMAC over time; dual-run period documented |
| IAM-D3 | As a compliance officer, I can query append-only security audit | Sign-in, role change, site-scope change written; no update API |
| IAM-D4 | As an admin, failed sign-ins lock after N attempts | Configurable threshold; unlock policy; audited |

### 6.5 IAM — Epic E: Invitation & password hygiene (Phase 2)

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| IAM-E1 | Invitation expires and can be resent | Expiry enforced; resend audited |
| IAM-E2 | User can reset password via token | Rate-limited; token single-use |
| IAM-E3 | Admins can enable MFA (TOTP) | Optional; step-up for sensitive actions |

### 6.6 MessageService — Epic F: Registry & credentials (POC → production)

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| MSG-F1 | As an admin, I register a site projection under my account | IAM session required; site scoped to accountId |
| MSG-F2 | As an admin, I register UV500 analyser (no MQTT creds) | Analyser row; no cert issued |
| MSG-F3 | As an admin, I register TRB142 gateway | gateway_code generated; status PENDING |
| MSG-F4 | As an admin, I issue gateway credential once | Cert+key returned once; only fingerprint stored; gateway ACTIVE; ACL/policy updated |
| MSG-F5 | As an admin, I bind gateway→analyser with uv500-v1 profile | Unique active map; profile JSON stored |
| MSG-F6 | As an admin, I revoke a gateway | Status REVOKED; ACL disabled; ingest rejects REVOKED |
| MSG-F7 | After IAM sites exist, MessageService refuses unknown site codes | Sync or IAM lookup before create gateway |

### 6.7 MessageService — Epic G: Ingest security & decode

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| MSG-G1 | Valid mTLS MQTT publish stores measurements on mapped analyser | Rows in measurements; gateway_id lineage set |
| MSG-G2 | Missing/wrong client identity is rejected | UNAUTHENTICATED ingest_event; no measurements |
| MSG-G3 | Payload gatewayId ≠ topic is rejected | PAYLOAD_IDENTITY_MISMATCH |
| MSG-G4 | Unmapped gateway quarantines raw only | UNMAPPED; telemetry_raw present; measurements empty |
| MSG-G5 | Duplicate messageId is idempotent | Second publish DUPLICATE; no double measurements |
| MSG-G6 | UV500 floats decode via bound profile | COD/BOD/… names+units from profile |
| MSG-G7 | HTTPS ingest accepts same payload with device auth | Same pipeline outcomes as MQTT |

### 6.8 MessageService — Epic H: Query & ops

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| MSG-H1 | Authenticated user queries measurements for own account | Filtered by accountId; 401 without JWT |
| MSG-H2 | User with SELECTED site scope cannot read other sites | When IAM siteScope wired |
| MSG-H3 | Rollup endpoints use continuous aggregates | 1min/1hour queries return |
| MSG-H4 | Ingest events visible to admins for debugging | Filter by time/outcome |
| MSG-H5 | Telemetry device cert cannot call query APIs | Only human IAM session |

### 6.9 MessageService — Epic I: Cloud / managed adapters

| ID | Story | Acceptance criteria |
|----|-------|---------------------|
| MSG-I1 | Config switches MessageService to managed Postgres JDBC URL (+ Timescale) | No schema change beyond config; health check green |
| IAM-PG1 | As a platform engineer, I migrate IAM from TiDB/MySQL to managed PostgreSQL | App boots on Postgres; Liquibase migrates clean; signup/signin/session/channel tests green |
| MSG-I2 | AWS IoT Core adapter issues thing+cert (optional path) | Fingerprint stored; policy = single topic publish |
| MSG-I3 | Dev still runs on Podman Mosquitto | Feature flag / profile `local` |
| MSG-I4 | Raw payload optionally archived to object storage | Key stored; downloadable for forensics |

---

## 7. Detailed development plan

### Principles

- Ship vertical slices with tests; keep IAM and MessageService deployable independently.
- Fix IAM authorize bug before MessageService depends on privilege checks in production.
- Local Podman remains the default for MessageService until cloud choice is locked.
- Front App starts after MSG-H1 + IAM-B4 basics.

### Phase overview

| Phase | Duration (indicative) | Focus | Exit criteria |
|-------|----------------------|-------|---------------|
| **P0** | 1–2 weeks | IAM hardening + MessageService POC hardening | Secure baseline; POC demo stable |
| **P1** | 3–4 weeks | IAM sites/privileges + MessageService registry/ingest productionize | Pilot one site end-to-end with real authz |
| **P2** | 3–4 weeks | HTTPS ingest + managed DB; service identities; audit | Cloud-ready MessageService + IAM audit |
| **P3** | 2–3 weeks | Cloud IoT adapter + Front App MVP hooks | Managed MQTT or HTTPS in chosen cloud; APIs ready for UI |

---

### Phase 0 — Stabilize (week 1–2)

#### IAM P0 tasks

| Task | Stories | Owner hint | Done when |
|------|---------|------------|-----------|
| Remove TestAccountUpdate + `/test/**` permitAll | IAM-A1 | IAM | PR merged; test proves blocked |
| Cookie Secure/SameSite env-dependent | IAM-A2 | IAM | Staging config verified |
| Scrub secrets from tracked env samples | IAM-A3 | IAM | Diff reviewed |
| Remove RAG screens/routes | IAM-A4 | IAM | Frontend build green |
| Document Channel JWT integration for MessageService | — | IAM | Link from MessageService README |

#### MessageService P0 tasks

| Task | Stories | Done when |
|------|---------|-----------|
| Freeze POC APIs in OpenAPI stub | F/G/H | `/v1` listed in README |
| Add integration test: register → issue → map → sim publish | G1 | Test green with Testcontainers or compose |
| Document negative tests (G2–G5) as automated where possible | G2–G5 | At least unit coverage (existing) + one e2e script |
| Rename package plan (`com.delight.message`) — optional defer | — | Decision recorded |

**P0 exit:** Local demo works; IAM no longer has public test mutation; MessageService README matches security model.

---

### Phase 1 — Pilot IAM + MessageService coupling (week 3–6)

#### IAM P1 tasks

| Sprint slice | Tasks | Stories |
|--------------|-------|---------|
| 1.1 Sites schema | Liquibase `sites`; JPA; CRUD APIs; Kafka `site.*.v1` | B1, B2 |
| 1.2 Site scope | membership_site_scopes + membership_sites; PUT API; session payload | B3, C4 |
| 1.3 Privileges | Seed monitoring privileges; map to roles | C1 |
| 1.4 Authorize fix | Remove channel always-true; siteId optional; integration tests | C2, C3, B4 |

#### MessageService P1 tasks

| Sprint slice | Tasks | Stories |
|--------------|-------|---------|
| 1.1 IAM privilege gate | After session validate, call authorize/check for registry mutations | F1–F6, C2 |
| 1.2 Site sync | Consumer for `site.*.v1` OR create-via-IAM then project | F7, B5 |
| 1.3 Align site codes | MQTT `{siteId}` = IAM site.code; migration if POC codes differ | F7 |
| 1.4 Query site filter | Optional siteId on measurements already; enforce scope when present in session | H1, H2 |

**P1 exit:** Operator limited to one site cannot read/write another; gateway registration requires `device.provision`; pilot MQTT still works.

---

### Phase 2 — Hardening & cloud-ready data path (week 7–10)

#### IAM P2 tasks

| Tasks | Stories |
|-------|---------|
| `security_audit_events` + writers for sign-in / role / site-scope | D3 |
| Lockout on failed sign-in | D4 |
| ServiceIdentity entity + issue/rotate (Channel HMAC still supported) | D1, D2 |
| Invitation expiry/resend | E1 |
| Password reset (basic) | E2 |

#### MessageService P2 tasks

| Tasks | Stories |
|-------|---------|
| `POST /v1/ingest/https` + device token or mTLS | G7 |
| Managed Postgres + Timescale connection profile (`prod`) | I1 |
| Optional raw archive to S3-compatible store | I4 |
| Ingest metrics (accept/reject counters) for ops | H4 |
| Fail-closed IAM cache TTL documented | — |

#### IAM P2 / P0–P1 infra (Postgres)

| Tasks | Stories |
|-------|---------|
| Migrate Liquibase + JPA to PostgreSQL; local compose Postgres | IAM-PG1 |
| Staging/prod managed Postgres `delight_accounts` | IAM-PG1 |
| CI integration tests against Postgres (Testcontainers) | IAM-PG1 |

**P2 exit:** HTTPS path demoed; **both** IAM and MessageService run on managed PostgreSQL; IAM audit queryable.

---

### Phase 3 — Cloud IoT + API freeze for Front App (week 11–13)

| Tasks | Stories |
|-------|---------|
| Decide E2E vs AWS vs Azure (credits + MQTT) | I2, platform |
| AWS IoT Core **or** Azure IoT Hub adapter **or** HTTPS-only on E2E | I2, I3 |
| Cert rotation: issue new → validate → revoke old (minimal) | F4, F6 |
| Freeze OpenAPI for Front App (registry + measurements) | H1–H5 |
| Load test: ingest rate for 1 site / 1-min polling | — |

**P3 exit:** Written ADR for cloud choice; MessageService ingest on managed path; Front App can start against frozen APIs.

---

## 8. Test strategy

| Layer | IAM | MessageService |
|-------|-----|----------------|
| Unit | AuthZ site scope, privilege parse | Topic parse, decoder, ingest reject matrix |
| Integration | Liquibase + authorize/check + session | Testcontainers Timescale; MQTT optional |
| Contract | OpenAPI for channel authorize | OpenAPI `/v1` |
| E2E | Signup → site → scope → deny | Register → cert → sim → query |
| Security | No public mutation; audit immutability | Device cert ≠ query; revoke enforced |

---

## 9. Definition of Done (per story)

- Code + migrations in the owning repo
- Unit and/or integration test for acceptance criteria
- No secrets committed
- README/OpenAPI updated if API changed
- Events versioned if schema changes
- Cross-service impact noted (IAM event → MessageService consumer)

---

## 10. Risks and decisions

| Risk / decision | Mitigation |
|-----------------|------------|
| Dual site stores (IAM + MessageService) | Single `code`; IAM is authz SoT; MessageService projects |
| Channel authorize always-true today | P1 blocker for real authz |
| E2E may lack managed MQTT | HTTPS ingest or AWS IoT; ADR |
| IAM today on TiDB/MySQL | Migrate to managed PostgreSQL (IAM-PG1); treat TiDB as legacy local-only until cutover |
| One Postgres host, many DBs | Preferred; never share one DB across services |
| Shared Channel HMAC | Dual-run with ServiceIdentity; deprecate |

---

## 11. Suggested backlog order (first 15 tickets)

1. IAM-A1 Remove test mutation surface  
2. IAM-A2 Cookie hardening  
3. IAM-PG1 Migrate IAM to PostgreSQL (local + CI)  
4. IAM-C2 Fix authorize/check always-true  
5. MSG — OpenAPI + e2e register/publish script  
6. IAM-B1 Sites CRUD + Liquibase (on Postgres)  
7. IAM-C1 Seed monitoring privileges  
8. IAM-B3 Membership site scope  
9. IAM-C3 authorize/check + siteId  
10. MSG-F7 / B5 Site sync from IAM  
11. MSG privilege gates on registry APIs  
12. IAM-D3 Security audit log  
13. MSG-G7 HTTPS ingest  
14. MSG-I1 + IAM-PG1 prod: managed Postgres for both services  
15. Cloud ADR (E2E/AWS/Azure) + MSG-I2 or HTTPS-only  

---

## 12. References

- [`docs/HLD-STP-WTP-IAM-CHANGES.md`](../../delight-account-service/docs/HLD-STP-WTP-IAM-CHANGES.md) (IAM HLD)
- [`IAM_SERVICE_INTEGRATION_GUIDE.md`](../../delight-account-service/IAM_SERVICE_INTEGRATION_GUIDE.md)
- [`README.md`](../README.md) (MessageService POC)
- Platform Cursor rules: `stp-wtp-*.mdc`
