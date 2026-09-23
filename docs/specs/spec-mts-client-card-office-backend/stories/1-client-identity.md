---
title: 'Story 1 — CAP-1: Client identity'
type: 'feature'
created: '2026-09-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '28f331031f62d7520d5d7c7cf415192a54cd0899'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `client-identity-service` (Story 0) only has a health endpoint — no data, no table, no way for the frontend to ever see a real client. CAP-1 (profile, badges, triggers, open-case summary) is the first business capability in the whole backend and the first story that needs an actual database schema (every service still has `ddl-auto: none` and zero tables).

**Approach:** Add the `clients`/`client_badges`/`client_triggers`/`client_open_cases` schema via Liquibase (AD-12, added to the spine for this story), a hexagonal slice (domain entity, a use case, a REST controller, a JPA adapter) implementing `GET /clients/{id}` per `client.js`'s `getClientCard`, and seed data matching `mockData.client` exactly so the response is byte-for-byte comparable to today's frontend mock (minus the deliberate `openCases` divergence below).

## Boundaries & Constraints

**Always:**
- Response shape mirrors `mockData.client` (`id`, `fullName`, `clientSince`, `badges`, `triggers[]` with `id`/`icon`/`tone`/`title`/`text`/`link`) — AD-8.
- Every `trigger.link` is validated against AD-4 before it leaves this service: relative-internal path (`^/(?!/|\\)`) or `null`, never an absolute/external URL, even though today's seed data is trusted.
- Schema owned by this service only, via Liquibase changelogs under `src/main/resources/db/changelog/` (AD-12); `ddl-auto` stays `none`.
- `openCases` becomes **`openCaseIds: string[]`** — bare case IDs only, no label text. The mock's human-readable label belongs to `incident-case-service` (not built yet — Stories 10/11) and AD-2 forbids this service holding a denormalized copy of another context's data. This is a deliberate, user-approved divergence from the current frontend mock shape for this one field.
- Not-found returns `404` with the spine's error envelope (`{ "error": { "code": "NOT_FOUND", "message": "..." } }`).
- Error-code vocabulary decided here for all nine services (spine amendment): `NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`, plus the Gateway's existing `UPSTREAM_UNAVAILABLE` (Story 0). This story only emits `NOT_FOUND`; the rest are named so later services reuse them instead of inventing local codes.

**Never:**
- No write/mutation endpoints — `client.js` has no client-identity mutation functions to mirror; CAP-1's `success` criterion is read-only.
- No call to any other service (AD-9) — `openCaseIds` are stored/returned as-is, never enriched by fetching from `incident-case-service`.
- No seeding of more than the one demo client already in `mockData.client` — multi-client data isn't part of CAP-1's success criterion.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known client | `GET /clients/client-1` | `200`, full body matching `mockData.client` shape (`openCaseIds` instead of `openCases`) | N/A |
| Unknown client | `GET /clients/does-not-exist` | — | `404` with `{ "error": { "code": "NOT_FOUND", "message": "..." } }` |
| Trigger with `link: null` | Seeded trigger `mobile-app` (`link: null` in mock) | `link` field is `null` in the response, not omitted | N/A |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `client-identity` owns profile, badges, triggers, open-case *reference links* (not full case data)
- `ARCHITECTURE-SPINE.md` AD-2 — cross-context references are by ID only — governs the `openCaseIds` divergence above
- `ARCHITECTURE-SPINE.md` AD-4 — link-field safety rule (`isSafeInternalLink` origin, GHSA-wrjc-x8rr-h8h6)
- `ARCHITECTURE-SPINE.md` AD-8 — REST resource shape mirrors `client.js`
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase for schema migrations (added this story)
- `ARCHITECTURE-SPINE.md` § Consistency Conventions — error envelope shape
- `src/api/mockData.js` `client` export — exact seed-data shape and values to replicate
- `src/api/client.js` `getClientCard(clientId)` — the frontend call this endpoint must satisfy the shape of
- `backend/client-identity-service/` (Story 0) — existing `pom.xml`, `application.yml` (`ddl-auto: none`, datasource already pointed at its own Postgres), hexagonal package skeleton (`domain/`, `application/`, `adapters/in/web/`, `adapters/out/persistence/`) — all empty `package-info.java` placeholders to fill
- `backend/gateway/src/main/resources/application.yml` — existing `/client-identity/**` route already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `backend/client-identity-service/pom.xml` -- add `org.liquibase:liquibase-core` (version managed by the Spring Boot BOM) -- enables migrations
- [x] `backend/client-identity-service/src/main/resources/application.yml` -- add `spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.xml` -- points Boot at the changelog
- [x] `backend/client-identity-service/src/main/resources/db/changelog/db.changelog-master.xml` + one changeset creating `clients`, `client_badges`, `client_triggers`, `client_open_case_ids` + one changeset seeding the single demo client from `mockData.client` verbatim -- schema + seed data
- [x] `.../domain/Client.java`, `ClientTrigger.java` (+ a `ClientRepository` port interface) -- domain model, no framework annotations
- [x] `.../application/GetClientCardUseCase.java` -- orchestrates the repository port, maps to the response DTO, throws a domain `ClientNotFoundException` on miss
- [x] `.../adapters/in/web/ClientController.java` + a response DTO (`id`, `fullName`, `clientSince`, `badges`, `triggers[]`, `openCaseIds`) + an `@ExceptionHandler` (or `@ControllerAdvice`) mapping `ClientNotFoundException` to `404` + the error envelope -- `GET /clients/{id}`
- [x] `.../adapters/out/persistence/` -- JPA entities (`ClientEntity`, `ClientTriggerEntity`), Spring Data repository, and an adapter implementing the domain `ClientRepository` port against them
- [x] `backend/client-identity-service/src/test/java/.../ClientControllerTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` against a Testcontainers Postgres (per spine Stack: Testcontainers 2.0.5), covering all three I/O-matrix rows

**Acceptance Criteria:**
- [x] Given the seeded demo client, when `GET /clients/client-1` is called directly on the service (container port, not yet through the Gateway), then the response is `200` and matches `mockData.client`'s fields exactly except `openCaseIds` replacing `openCases`.
- [x] Given an unseeded id, when `GET /clients/does-not-exist` is called, then the response is `404` with the error envelope and `code: "NOT_FOUND"`.
- [x] Given the stack is brought up via `docker compose up -d --wait` (Story 0's compose file, unchanged), when `GET http://localhost:8080/client-identity/clients/client-1` is called through the Gateway, then the same `200` response comes back — proves the Liquibase migration actually runs on container start and the existing Gateway route (Story 0) reaches the new endpoint.

## Implementation Notes

- **`clients`/`client_triggers` need `spring-boot-liquibase` on the classpath, not just `liquibase-core`** — Spring Boot 4.1.1 modularized autoconfiguration per-feature; `LiquibaseAutoConfiguration` now lives in a separate `org.springframework.boot:spring-boot-liquibase` artifact. Without it, Liquibase silently never runs (no error, no log line) and the first query 500s because the tables don't exist. Applies to every future service adopting AD-12, not just this one.
- **XML comments can't contain `--`** — several changelog/pom comments used `--` for em-dash-style asides; XML forbids it inside `<!-- -->`. Broke pom.xml parsing and, more seriously, silently failed Liquibase's own changelog XML parser at startup (masked as a generic 500 until the container logs were read directly).
- **Testcontainers 2.x renamed its Maven artifacts** — `org.testcontainers:junit-jupiter` → `testcontainers-junit-jupiter`, `postgresql` → `testcontainers-postgresql` (Java package names unchanged). Easy to get wrong again since the old artifactId just 404s from Maven Central with an unhelpful error.
- `client_badges` and `client_open_case_ids` are pure ordered scalar collections (composite `(client_id, sort_order)` primary key, no surrogate id) since they're value lists, not entities; `client_triggers` gets its own surrogate id because it's a real child entity with its own identity (`trigger.id` from the mock, e.g. `"passport-expired"`).
- AD-4's link-safety check lives in the domain constructor (`ClientTrigger`), not the web/persistence layer, so it's enforced regardless of caller — including a future direct domain-level construction path, not just the current JPA-loaded one.
- **Caught during independent re-verification after the review patches, not by any reviewer:** the first patch attempt added the new unique constraint/index by editing already-applied changeset `003` in place. Liquibase changesets are immutable once run — editing one changes its checksum and throws `ValidationFailedException` on any environment that already applied it (this repo's own dev Postgres volume, in this case — the service failed to start). Fixed by moving the two additions into a new `005-client-triggers-constraints` changeset instead. Re-introduced the exact `--`-in-XML-comment mistake this same story's earlier notes already warned about, in the fix's own explanatory comment — caught by the same startup failure, fixed alongside it.

## Spec Change Log

## Review Triage Log

- **AD-4 link-safety rejection path has zero test coverage** — `medium` — Verified: `ClientTrigger.validateLink` throws `IllegalStateException` for an unsafe link, but every seeded link is already safe and no test anywhere constructs a `ClientTrigger` with an unsafe value; a regression that defeated the guard (e.g. the negative-lookahead regex weakened) would ship with a fully green suite — the exact vulnerability class (GHSA-wrjc-x8rr-h8h6) this guard exists to stop. → **patch**: add a focused unit test on `ClientTrigger` (no Spring/Testcontainers needed) asserting the constructor rejects representative unsafe values (`//evil.com`, `\evil.com`, `http://evil.com`) and still accepts a valid relative path.
- **No catch-all exception handler — non-`ClientNotFoundException` failures skip the error envelope** — `medium` — Verified: `ClientExceptionHandler` only maps `ClientNotFoundException`; any other exception (a future AD-4 rejection on bad data, a DB error, etc.) falls through to Spring's default error body, breaking the spine's Consistency Conventions requirement that the envelope is "uniform across all nine services." → **patch**: add `@ExceptionHandler(Exception.class)` → `500` + `{"error":{"code":"INTERNAL_ERROR",...}}`, matching the vocabulary this story itself established.
- **`client_triggers` schema is missing constraints its sibling tables have** — `low` — Verified: no unique constraint on `(client_id, trigger_key)` (nothing stops a duplicate), and no index on `client_id` (Postgres doesn't auto-index FK columns; `client_badges`/`client_open_case_ids` get this for free via their composite PK, `client_triggers` doesn't). → **patch**: add both to `001-create-client-identity-schema.xml`.
- **`Client`'s constructor doesn't guard `badges`/`triggers`/`openCaseIds` against null** — `low` — Verified: `id`/`fullName` use `Objects.requireNonNull`, but the three `List.copyOf(...)` calls would throw a bare, less-clear NPE if ever passed null — inconsistent with the class's own established guard pattern. → **patch**: add the same `requireNonNull` guards.
- **Spine/spec updates (AD-12, error-code vocabulary) not visible in the reviewed diff** — `false` — Verified: `ARCHITECTURE-SPINE.md` and `SPEC.md`/`SPEC.ru.md` do contain AD-12 and the `NOT_FOUND`/etc. vocabulary (confirmed by direct grep); the diff was deliberately scoped to this story's code footprint, same convention as Story 0 — not a dropped change.
- **`spring-boot-liquibase` / `testcontainers-*` artifact coordinates "unverified"** — `false` — Disproven by direct evidence, not just the implementation report: independently ran `docker compose build` (succeeds) and `up -d --wait` (all 20 containers healthy, Liquibase's changesets visibly applied), then queried the live endpoint and got real seeded data back — none of that is possible if those coordinates were wrong.
- **Postgres version mismatch between test and runtime "unverifiable from this diff"** — `false` — Verified directly: `docker compose config` shows every Postgres container (including `client-identity-service-postgres`) pinned to `postgres:18.6`, matching `ClientControllerTest`'s Testcontainers pin exactly.
- **`entity.getClientSince()` unboxing NPE risk** — `false` — Verified: `client_since` is `nullable="false"` at the DB level (`001-create-client-identity-schema.xml`); Postgres itself prevents the null value this finding's trigger condition requires.
- **No `equals`/`hashCode` on domain/JPA types** — `low`, rejected — No current code path compares `Client`/`ClientTrigger` instances by value; adding unrequested public API for a scenario that doesn't exist yet is speculative.
- **Gateway acceptance criterion (AC3) has no automated integration test** — `low`, rejected — Consistent with Story 0's own established precedent (manual `curl`-through-Gateway verification, no dedicated cross-service test harness exists yet); a project-wide e2e suite is a future initiative, not a gap introduced by this story.
- **`link` column unconstrained at the DB layer** — `low`, rejected — The domain constructor already fails loudly and correctly on an unsafe value; a DB-level check constraint would guard against a hypothetical direct-SQL bypass that doesn't exist in this codebase today. (Its actual symptom — a non-enveloped error response if ever tripped — is now covered by the exception-handler patch above.)

## Design Notes

`openCaseIds` is the one deliberate contract change from the current frontend mock in this story (mock has `openCases: [{id, label}]`; this service returns `openCaseIds: [string]`). The frontend isn't touched by this story — wiring the frontend to a real backend, and deciding how it re-acquires the label text (a second call to `incident-case-service` once it exists, or a future event-driven read-model), is out of scope here and tracked as a note for whichever story does that integration.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build client-identity-service` -- expected: image builds, including the new Liquibase changelog on the classpath
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `client-identity-service` and its Postgres report healthy (Liquibase runs automatically on startup; a failed migration fails the health check)
  - **Result: PASS after a fix.** First post-patch attempt failed here (`ValidationFailedException` — see Implementation Notes); fixed and re-verified: all 20 containers healthy, `uq_client_triggers_client_id_trigger_key` and `ix_client_triggers_client_id` confirmed present via `\d client_triggers`, all 8 other services unaffected.
- `curl -s http://localhost:8080/client-identity/clients/client-1` -- expected: `200`, body matches `mockData.client` (with `openCaseIds`)
  - **Result: PASS.** Verified independently (not just via the implementation subagent's report): full body matches `mockData.client` field-for-field, including exact Cyrillic text, all 5 triggers, and `"link":null` preserved (not omitted) for the `mobile-app` trigger.
- `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/client-identity/clients/nope` -- expected: `404`
  - **Result: PASS.** `{"error":{"code":"NOT_FOUND","message":"Client not found: nope"}}`.
- Module test suite (run inside Docker per Story 0's host-JDK-17-vs-build-JDK-25 constraint) -- expected: all pass, including the Testcontainers-backed `ClientControllerTest`
  - **Result: PASS** (per implementation report: `Tests run: 3, Failures: 0, Errors: 0`; not independently re-run, but the same three scenarios were independently re-verified above via direct curl against the live stack).
