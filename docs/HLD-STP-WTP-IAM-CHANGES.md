# HLD: delight-account-service changes for STP/WTP Monitoring SaaS

| Field | Value |
|-------|-------|
| Status | Draft |
| Service | `delight-account-service` |
| Audience | Platform / IAM / security |
| Related | `delight-platform-service-plan.md`, STP/WTP system design, Horiba interface design |
| Out of scope | Device registry, telemetry ingestion, PCB adapter, measurement store |

---

## 1. Purpose

Evolve `delight-account-service` into the **identity and tenancy authority** for the STP/WTP monitoring platform without turning it into a device or telemetry system.

Today the service supports organization → account (tenant) → user/membership/session with Casbin RBAC and Kafka events. STP/WTP requires **site-scoped access**, **non-human service identities**, stronger **security controls**, and an **immutable audit trail**, while remaining the source of truth that other services authorize against.

---

## 2. Current state (as-is)

### 2.1 What exists

| Capability | Implementation |
|------------|----------------|
| Tenancy | Account resolved from subdomain `Host` (`TenantFilter` → `TenantContext`) |
| Hierarchy | Organization → Account → User / Membership / Session / Roles |
| AuthN | JWT + DB `sessions`; cookie `hrsession`; Channel JWT for `/channel/**` |
| AuthZ | Casbin + Redis; subject `{userId}:{accountId}`; roles → privileges |
| System roles | `ORG_ADMIN`, `ACCOUNT_ADMIN`, `AGENT` |
| Events | `account_*`, `user_*`, `user_role_*` Kafka topics |
| Stack | Java 21, Spring Boot 3.4.6, TiDB/MySQL, Liquibase, Redis, JobRunr |

### 2.2 Gaps vs STP/WTP needs

1. No **Site** entity or membership site scope (authz is account-wide).
2. No **service/device identity** model (only human users + shared channel JWT).
3. Privilege catalog is chat/CRM-oriented; missing site/device/export/PCB-related codes.
4. Incomplete custom-role design (`restricted_privileges` designed, not fully shipped).
5. Weak security posture: public `/test/**` mutation endpoint; browser session cookie hardening incomplete; dual JWT exposure risk.
6. No immutable security **audit log**.
7. No password reset / MFA / lockout / admin step-up / invitation expiry-resend.
8. RAG mock UI remnants unrelated to IAM (cleanup only).

### 2.3 Explicit non-goals for this service

- Gateway/analyser inventory, Modbus profiles, AWS IoT Thing lifecycle → **device registry**.
- Storing gateway private keys or issuing device X.509 material as long-lived secrets in account DB.
- Reading measurement or PCB databases.

Account service may store **references** (e.g. site id, service principal id, certificate **metadata id/status** pointers owned by registry) only where needed for authorization and audit correlation.

---

## 3. Target state (to-be)

### 3.1 Domain hierarchy

```text
Organization
  └── Account (tenant)                    ← existing; maps to MQTT {tenantId}
        ├── Site*                         ← NEW; maps to MQTT {siteId}
        ├── Membership (user ↔ account)
        │     └── MembershipSiteScope*    ← NEW: ALL_SITES | explicit site set
        ├── Role / Privilege              ← extend catalog
        ├── ServiceIdentity*              ← NEW: backend / provisioning principals
        ├── Session / UserCredential      ← harden
        └── SecurityAuditEvent*           ← NEW: append-only audit
```

\* New or substantially extended for this HLD.

Gateways and analysers remain in the **device registry**; account service answers: “who may act on which tenant/sites with which privileges?”

### 3.2 Logical architecture

```text
Browser / SPA
  → Account Web/API (session cookie or Bearer)
       → TenantFilter + JwtAuthenticationFilter
       → Casbin (account + site-aware enforcement)
       → Domain services (membership, site, service-identity, audit)

Internal services (ingestion, registry, alerts, PCB adapter)
  → /channel or future service-credential endpoints
       → ChannelAuthFilter / ServiceIdentityAuth
       → AuthorizationService (privilege + site scope check)
       → versioned Kafka events for cache invalidation
```

### 3.3 Design principles

1. **Account service owns IAM data only**; other services enforce locally using tokens + cached scope from events/APIs.
2. **Site is first-class for access control**, even if device details live elsewhere.
3. **Humans ≠ machines**: gateways and backend workers are `ServiceIdentity` (or registry-owned device principals with IAM-linked metadata), never `User` rows.
4. **Least privilege**: telemetry credentials must not imply management privileges.
5. **Audit everything privileged**: sign-in, role/site change, invite, export, cert-rotation request, PCB submission ack (when initiated via console with IAM actor).

---

## 4. Capability workstreams

### WS-0 — Immediate hardening (Phase 0)

| Change | Detail |
|--------|--------|
| Remove test surface | Delete `TestAccountUpdateController`; remove `/test/**` `permitAll` |
| Session cookies | Production: `Secure=true`, explicit `SameSite`; env-dependent for local |
| JWT delivery | Prefer HttpOnly cookie as browser session; do not also return JWT to JS when cookie is the chosen model |
| Secrets | No real credentials in tracked `envVariables_*`; inject at deploy |
| Frontend cleanup | Remove RAG screens (`BotSettings`, `KnowledgeBase`, `IntentsFlows`, `Testing`, `FlowBuilder`) and related routes/settings |

### WS-1 — Sites and site-scoped membership (Phase 1 / pilot)

**Entities**

- `sites`: `id`, `account_id`, `code` (stable external id), `name`, `status`, `timezone`, `metadata` (JSON), timestamps.
- `membership_site_scopes`: `membership_id`, `scope_type` (`ALL_SITES` | `SELECTED`), optional rows in `membership_sites` (`membership_id`, `site_id`).

**Rules**

- `ORG_ADMIN` / `ACCOUNT_ADMIN` default to `ALL_SITES` unless restricted.
- Operator roles (evolved from `AGENT` or new `SITE_OPERATOR`) may be limited to explicit sites.
- Authorization check becomes: privilege **and** (ALL_SITES **or** site ∈ membership sites).
- Sign-in / session validate / channel authorize responses include compact `siteScope`: `{ type, siteIds[] }`.

**Casbin extension options (choose one in implementation)**

| Option | Approach | Trade-off |
|--------|----------|-----------|
| A (recommended for pilot) | Casbin keeps privilege checks; site scope enforced in `AuthorizationService` after Casbin | Simpler; clear separation |
| B | Encode site in Casbin `g3` / resource path | More policy complexity; harder cache invalidation |

**APIs (illustrative)**

- `POST/GET/PATCH /accounts/sites`
- `PUT /accounts/members/{id}/site-scope`
- Extend `POST /channel/accounts/authorize/check` with optional `siteId` / `resourceSiteId`

**Events**

- `site.created.v1`, `site.updated.v1`, `site.deactivated.v1`
- Extend membership events with site-scope payload (or `membership.site_scope.changed.v1`)

### WS-2 — Service identities and internal auth evolution (Phase 1–2)

**Entity `service_identities`**

- `id`, `account_id` (nullable for platform-wide), `client_id`, `name`, `type` (`BACKEND_SERVICE` | `PROVISIONING` | `INTEGRATION`)
- `status`, `key_version` / credential fingerprint (never raw secret)
- Optional link fields for correlation: `external_ref` (e.g. AWS IoT thing name **reference only**)

**Rules**

- Issue short-lived tokens (JWT or opaque) signed by account service or internal issuer.
- Channel HMAC shared keys are **deprecated over time**; migrate callers to service credentials.
- Device X.509 remains owned by device registry / AWS IoT; account service may record that a **service identity** was used to request rotation (audit), not the private key.

**APIs**

- Admin: create/rotate/revoke service identity credentials
- Internal: token introspection / authorize check for service principals

### WS-3 — Privilege catalog for monitoring product (Phase 1)

Add privilege codes (seed via Liquibase), for example:

| Category | Codes (examples) |
|----------|------------------|
| Site | `site.create`, `site.read`, `site.update`, `site.deactivate` |
| Device (authz only) | `device.read`, `device.provision`, `device.credential.rotate` |
| Measurement | `measurement.read`, `measurement.export` |
| Alert | `alert.read`, `alert.acknowledge`, `alert.configure` |
| PCB | `pcb.submit`, `pcb.approve`, `pcb.read_ledger` |
| Audit | `audit.read` |

Map default grants:

- `ORG_ADMIN` — all
- `ACCOUNT_ADMIN` — manage sites/users/devices; PCB approve; no org delete
- Operator role — read measurements/alerts; acknowledge; no provision/rotate/PCB approve unless granted

Frontend Security UI should eventually show monitoring privileges instead of chat-only labels when product is STP/WTP.

### WS-4 — Security controls (Phase 1–2)

| Control | Behavior |
|---------|----------|
| Invitation | Expiry + resend; audit invite/accept |
| Password reset | Tokenized reset flow; rate-limited |
| Lockout | Threshold after failed sign-in; unlock policy |
| MFA | Optional TOTP (or equivalent) for admins first |
| Step-up | Re-auth required for role change, site scope change, service-credential rotate, PCB approve |

### WS-5 — Immutable security audit (Phase 1)

**Entity `security_audit_events`** (append-only)

- `id`, `account_id`, `actor_type` (`USER` | `SERVICE` | `SYSTEM`), `actor_id`
- `action`, `resource_type`, `resource_id`, `site_id` (nullable)
- `outcome`, `ip`, `user_agent`, `correlation_id`, `payload` (redacted JSON)
- `created_at` (no update path)

Emit for: sign-in success/failure, logout-all, invite, role change, site scope change, service credential rotate, data export, and privileged actions proxied through account APIs.

Retention and export for compliance are operational concerns; schema must support export without mutation.

---

## 5. Authorization contract for consumers

Downstream services (API gateway, device registry, measurement, alerts, PCB) must:

1. Validate session or service token with account service (or trusted JWT verification).
2. Enforce **privilege + site scope** on every site-scoped resource.
3. Subscribe to membership/site events to invalidate local auth caches.
4. Never treat MQTT device identity as a human session.

**Authorize check request (extended)**

```json
{
  "accountId": "...",
  "userId": "...",
  "privilege": "measurement.read",
  "siteId": "..."
}
```

**Response**

```json
{
  "allowed": true,
  "siteScope": { "type": "SELECTED", "siteIds": ["..."] }
}
```

JWT/session claims (compact): `accountId`, `userId`, `role`, `orgId`, `sessionId`, `siteScopeType`, optional hashed/short site list or “fetch scope” flag for large sets.

---

## 6. Data ownership and events

| Producer | Event | Consumers |
|----------|-------|-----------|
| Account | `site.*.v1` | Device registry, API gateway cache, alerts |
| Account | `membership.*.v1` (+ site scope) | Auth caches, notifications |
| Account | `service_identity.*.v1` | Internal callers, audit projections |
| Device registry | `gateway.*`, `device.credential.rotation.*` | Account audit correlation only (optional consumer) |

Account service **does not** publish telemetry or PCB submission domain events.

---

## 7. Migration and delivery phases

### Phase 0 — Harden (must ship first)

- Security cleanup (test controller, cookie/JWT policy, secrets hygiene)
- RAG UI removal
- Verify builds: `frontend` `npm run build`, `./gradlew build`
- Preserve existing user edits to `README.md`, `compose.yaml`, `envVariables_docker_local`

### Phase 1 — Pilot IAM (one site)

- Introduce `sites` + default site per pilot account
- Membership `ALL_SITES` for admins; optional SELECTED for operators
- Extend authorize check + session payload with site scope
- Seed monitoring privileges; map to system roles
- Append-only audit for sign-in and membership/site changes
- Invitation expiry/resend + basic lockout

### Phase 2 — Operational IAM

- Service identities replacing shared channel keys for new callers
- MFA + admin step-up
- Password reset
- Custom roles completion (`restricted_privileges` + APIs) if needed for plant orgs
- Full privilege UX for monitoring product

### Phase 3 — Scale / compliance

- Efficient large site-set scope (groups or “site sets”)
- Audit export/evidence packs
- Formal OIDC/internal issuer if multi-issuer federation required

---

## 8. Non-functional requirements

| Area | Requirement |
|------|-------------|
| Consistency | Membership/site scope changes invalidate Casbin/Redis and session-derived caches promptly |
| Performance | Authorize check p99 suitable for gateway path (cache-friendly); avoid N+1 site joins |
| Security | No private keys in DB or events; audit payloads redacted |
| Compatibility | Existing chat product accounts continue to work; new privileges additive |
| Tenancy | Subdomain account resolution remains; MQTT `tenantId` aligns with account external id/code |

---

## 9. Open decisions

1. **Stable external ids**: use numeric DB ids vs UUID/`code` strings for MQTT `{tenantId}` / `{siteId}` — recommend opaque stable `code` published on create.
2. **Casbin vs service-layer site checks** — recommend Option A for pilot.
3. **Product packaging**: single account service for chat + monitoring vs product-flagged privilege sets — recommend product-aware privilege seeding, shared IAM core.
4. **Where “request cert rotation” is initiated**: console → device registry (primary) with IAM privilege check; account service only audits if called.
5. **LLMNoteAssistant / CRM screens**: retain vs strip when repositioning UI for STP/WTP (separate product decision).

---

## 10. Success criteria (Phase 1)

- [ ] Public `/test/**` mutation path gone; production cookies Secure + SameSite set
- [ ] Pilot account can create one site and assign operator to that site only
- [ ] `authorize/check` denies cross-site access for SELECTED memberships
- [ ] Session/authorize responses expose site scope consumed by API gateway
- [ ] Security audit rows exist for sign-in and role/site changes
- [ ] No gateway private keys or Modbus write capabilities introduced in this service
- [ ] Builds green; pre-existing local user file edits preserved

---

## 11. References

- Platform plan: `~/Documents/Codex/2026-07-16/am/outputs/delight-platform-service-plan.md`
- Existing IAM design: `ROLE_PRIVILEGE_SYSTEM_DESIGN.md`, `IAM_SERVICE_INTEGRATION_GUIDE.md`, `KAFKA_EVENT_TYPES.md`
- Cursor context: `.cursor/rules/stp-wtp-*.mdc`, `delight-account-service/.cursor/rules/account-service-stp-wtp.mdc`
