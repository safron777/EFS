---
title: 'Story 3 — CAP-3: Cards and balances'
type: 'feature'
created: '2026-09-27'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '80797ddba6c490a5fd78052e85d887f57484daf3'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `card-account-service` (Story 0) only has a health endpoint. CAP-3 needs the frontend's `getCards(clientId, productId)` call served with the client's card list, balances, and status.

**Approach:** Add the `cards` schema via Liquibase (AD-12) and a hexagonal slice implementing `GET /clients/{clientId}/products/{productId}/cards`, seeded from `mockData.cards` — but with two deliberate wire-format corrections the mock itself doesn't make: money as integer kopecks (AD-3) and dates as ISO-8601 (Consistency Conventions, established in Story 1), not the mock's raw ruble integer and `dd.MM.yyyy` strings.

## Boundaries & Constraints

**Always:**
- Response is a JSON array mirroring `mockData.cards`' fields (`id`, `product`, `masked`, `subtitle`, `isMain`, `status`, `balance`, `issuedAt`, `account`, `closedAt`) at `GET /clients/{clientId}/products/{productId}/cards` — AD-8.
- `balance` is a signed integer in kopecks (AD-3) — the mock's `12430` (rendered by the frontend as `12 430 ₽` via `toLocaleString`) becomes seed value `1243000`. This is a deliberate divergence from the mock's raw number, not a copy of it.
- `issuedAt`/`closedAt` are ISO-8601 dates (`"2025-01-12"`), not the mock's `"12.01.2025"` — same Consistency Conventions rule Story 1 already established for dates; locale formatting stays a frontend concern.
- `account`/`issuedAt`/`closedAt` are nullable and present only where the mock has them (active card: `account`+`issuedAt`, no `closedAt`; closed card: `closedAt`, no `account`/`issuedAt`) — reproduce this per-row presence exactly, not a uniform shape with nulls padded in everywhere.
- Cards reference their client by `clientId` only (AD-2) — no copy of client profile fields.
- This service gets its own `@RestControllerAdvice extends ResponseEntityExceptionHandler` from the start (not a bare `@ExceptionHandler(Exception.class)`) — Stories 1 and 2 both shipped that bare form and both had to be patched after independent verification caught it silently converting Spring's own `405`/`415` into a misleading `500`. Copy the now-fixed shape those two services already have, not their original one.

**Never:**
- No write/mutation endpoints — CAP-3's `success` criterion is read-only; "switching" the active card is a frontend-only concern (`useCardScopedData`'s `activeCardId` state), not a backend action.
- No cross-service call to `client-identity-service` to validate `clientId` (AD-9).
- No `NOT_FOUND` case for an unknown `clientId`/`productId` — this is a collection endpoint; an unmatched combination returns `200` with an empty array, standard REST collection semantics, not a 404 (unlike Story 1's single-resource `/clients/{id}` lookup).
- No documents endpoint — that's CAP-4, a separate story on this same service.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known client, known product | `GET /clients/client-1/products/debit-mts-dengi/cards` | `200`, array of 2 cards matching `mockData.cards` (with the kopecks/ISO-date corrections above) | N/A |
| Unknown client or product | `GET /clients/nope/products/debit-mts-dengi/cards` | `200`, `[]` | N/A |
| Non-GET method | `POST /clients/client-1/products/debit-mts-dengi/cards` | — | `405` with the spine's error envelope, not a hang or a `500` |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `card-account` owns cards, balances, linked account
- `ARCHITECTURE-SPINE.md` AD-2 — cross-context references by ID only
- `ARCHITECTURE-SPINE.md` AD-3 — money as integer minor units, never a formatted string
- `ARCHITECTURE-SPINE.md` AD-8 — REST resource shape mirrors `client.js`
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase for schema migrations
- `ARCHITECTURE-SPINE.md` § Consistency Conventions — error envelope + code vocabulary, ISO-8601 dates
- `src/api/mockData.js` `cards` export — exact fixture shape and values (before the kopecks/ISO-date corrections)
- `src/api/client.js` `getCards(clientId, productId)` — `/clients/{clientId}/products/{productId}/cards`
- `src/hooks/useCardScopedData.js` — confirms "switching" cards is frontend-only state (`activeCardId`), not a backend concept; backend only ever returns the full list
- `src/pages/BalancePage.jsx` (`mainCard.balance.toLocaleString("ru-RU")`) and `src/components/shared/CardSwitcher.jsx` — confirms the mock's `balance` is raw rubles (not kopecks) and which fields (`status`) drive UI state, grounding the AD-3 correction above
- `backend/client-identity-service/` (Story 1, post-fix) and `backend/offers-service/` (Story 2, post-fix) — both now have the corrected `ResponseEntityExceptionHandler`-based advice; copy that shape directly, do not re-derive the buggy bare form
- `backend/gateway/src/main/resources/application.yml` — existing `/card-account/**` route already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `.../domain/Card.java` -- framework-free entity (`id`, `product`, `masked`, `subtitle`, `isMain`, `status`, `balanceMinorUnits`, `issuedAt`, `account`, `closedAt`), a `CardRepository` port (`List<Card> findByClientIdAndProductId(String clientId, String productId)`)
- [x] `.../application/GetCardsUseCase.java` -- orchestrates the port, no not-found branching (empty list is a valid result)
- [x] `.../adapters/in/web/CardController.java` + response DTO array + `CardExceptionHandler extends ResponseEntityExceptionHandler` (the corrected shape, not Story 1/2's original) -- `GET /clients/{clientId}/products/{productId}/cards`
- [x] `.../adapters/out/persistence/` -- JPA entity, Spring Data repository (`findByClientIdAndProductId`), adapter implementing the domain port
- [x] `backend/card-account-service/src/main/resources/db/changelog/` -- `cards` table (nullable `account`/`issued_at`/`closed_at`) + one changeset seeding both mock cards for `client-1`/`debit-mts-dengi` with the kopecks/ISO-date corrections
- [x] `backend/card-account-service/src/test/java/.../CardControllerIntegrationTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` against a Testcontainers Postgres, covering all three I/O-matrix rows (including the `405` case, learned from Stories 1/2)

**Acceptance Criteria:**
- [x] Given the seeded data, when `GET /clients/client-1/products/debit-mts-dengi/cards` is called directly on the service, then the response is `200` with both cards, `balance` in kopecks (`1243000`/`0`) and dates in ISO-8601 (`"2025-01-12"`/`"2025-10-20"`), matching per-row field presence from the mock exactly.
- [x] Given an unknown `clientId`, when `GET /clients/nope/products/debit-mts-dengi/cards` is called, then the response is `200` with `[]`.
- [x] Given a non-GET method, when `POST /clients/client-1/products/debit-mts-dengi/cards` is called, then the response is `405` with the error envelope, not `500`.
- [x] Given the stack is brought up via `docker compose up -d --wait` (unchanged compose file), when the same request is made through the Gateway, then the same `200` response comes back.

## Implementation Notes

- `CardResponse` is annotated `@JsonInclude(Include.NON_NULL)` at the class level so `account`/`issuedAt`/`closedAt` are *omitted* (not emitted as `"key":null`) on the row that lacks them -- the opposite choice from Story 1's `ClientCardResponse.TriggerResponse.link`, which deliberately keeps `"link":null`. The two differ because the mock itself differs: every trigger object always has a `link` key (sometimes `null`), but `mockData.cards`' row objects simply omit the key entirely when absent. Verified on the wire: `card-4019`'s JSON has no `closedAt` key; `card-7274`'s has neither `account` nor `issuedAt`.
- `clientId`/`productId` live only on the JPA `CardEntity` and the `findByClientIdAndProductId` query, never on the domain `Card` or the response -- AD-2 plus the story's explicit field list for `Card` (which omits them) made this the natural boundary; the persistence adapter is the only place that knows the scoping columns exist.
- Added a repository method `findByClientIdAndProductIdOrderByIsMainDescIdAsc` (rather than the unordered `findByClientIdAndProductId` the port signature names) so the two-row response is deterministically ordered main-card-first, matching `mockData.cards`' own order; nothing in the story mandates ordering explicitly, but an unordered `SELECT` over Postgres gives no such guarantee and the I/O matrix's "array of 2 cards matching mockData.cards" reads most naturally as order included.
- **Sandbox-specific, not a project defect:** this host's JDK is 17 and the stack targets Java 25 (Story 0's documented host-JDK-vs-build-JDK split), so both `docker compose build` and the Testcontainers-backed `mvn test` had to run inside an `eclipse-temurin:25` container rather than directly on the host, exactly as Story 0/1's README section describes. Running Testcontainers this way (mounting the host's `/var/run/docker.sock` into that container -- "Docker-outside-of-Docker") needed two extra flags this sandbox's Docker Desktop-on-macOS networking required: `TESTCONTAINERS_RYUK_DISABLED=true` (the resource-reaper's own port wasn't reachable back from the JVM container) and `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` (without it, Testcontainers guessed the bridge-gateway IP for the Postgres container's mapped port, which Docker Desktop's macOS VM doesn't route back to). Neither flag is committed anywhere in this repo; they were only needed to execute `mvn test` inside this particular sandbox session, not by the code itself.
- **Caught while running the above, fixed before it ever reached a committed changeset:** the first draft of `001-create-card-account-schema.xml`/`002-seed-cards-client-1.xml` used `--` inside XML `<!-- -->` comments for em-dash-style asides (documentation style already used elsewhere in this repo's Java Javadoc). XML forbids `--` inside comments; Liquibase's SAX parser rejected the changelog outright (`"databaseChangeLog" expected as root element`, `The string "--" is not permitted within comments`) before a single changeset could run. This is the exact trap Story 1's Implementation Notes already names ("XML comments can't contain `--`") -- reproduced here despite reading that note first, then fixed by rewording the affected comments (commas/full stops in place of `--`) rather than suppressing or working around the parser.

## Spec Change Log

## Review Triage Log

- **`CardExceptionHandler.handleExceptionInternal` converts `HttpStatusCode`→`HttpStatus` unnecessarily and can crash** — `medium` — Verified: `HttpStatus.valueOf(statusCode.value())` is called only to reach `.is4xxClientError()`, which `HttpStatusCode` already exposes directly; `HttpStatus.valueOf(int)` throws `IllegalArgumentException` for any non-standard code, which would turn an in-flight error response into a fresh unhandled `500`. Also: a non-4xx/5xx status falls through to `"INTERNAL_ERROR"` with no explicit branch, and `exception.getMessage()` has no null fallback (unlike the sibling `handleUnexpected` method), risking `"message": null` against the envelope's documented `string` contract. **The identical pattern already exists in `client-identity-service` and `offers-service` (Stories 1/2) — this story's spec told the implementer to copy that shape, so it copied the latent issue too.** → **patch**: use `statusCode.is4xxClientError()`/`.is5xxServerError()` directly, add an explicit fallback branch, and default the message when null — applied here and retroactively to both sibling services, since 7 more stories are set to copy this exact shape next.
- **Field-presence test asserts on raw JSON via string-splitting** — `low` — Verified: `perRowFieldPresenceMatchesMockExactlyNotPaddedWithNulls` checks for absent keys with `body.split("\\},\\{")` + substring checks — correct today, but silently breaks under pretty-printing, key reordering, or a string value containing `},{`. → **patch**: replace with a `JsonNode`-based "key must not exist" assertion.
- **Verification section's `mvn` command path is inconsistent with its siblings** — `low` — Verified: every other command in the list is repo-root-relative (`docker compose -f backend/docker-compose.yml ...`), but the test command reads `mvn -f card-account-service/pom.xml test` (missing the `backend/` prefix) — copy-pasting from repo root would fail with "no such file". → **patch**: fix the path.
- **`Card.java`'s Javadoc has a garbled sentence** — `low` — Verified: "AD-2 already rules out copying more of the client profile onto this object than its id would have been" doesn't parse. → **patch**: reword.
- **This review's diff omitted `card-account-service/pom.xml`** — acknowledged, no code action — Verified: a gap in how I scoped the diff for this review round (I only diffed `backend/pom.xml`, not the service's own `pom.xml`, which did change), not a defect in the change itself — the real `docker compose build`/`up`/test run I did independently already exercises the actual `pom.xml` on disk, so the implementation was verified regardless of this review's blind spot.
- **This review's diff omitted the `.ru.md` translation** — `false` — Same established convention as Stories 0–2: the Russian duplicate is a translation, not code under review.
- **Seed changeset has no `<rollback>` element** — `low`, rejected — Liquibase rollback support has no current use in this demo (no rollback workflow exists or was requested anywhere in the project; CI/CD is explicitly out of scope per Story 0).
- **No dedicated unit tests for `GetCardsUseCase`/`Card`/`CardResponse`** — `low`, rejected — `GetCardsUseCase` has no branching logic to unit-test (pure pass-through, same conclusion Story 2 reached for its own use case); the per-row nullability mapping this would otherwise cover is already asserted end-to-end by `perRowFieldPresenceMatchesMockExactlyNotPaddedWithNulls`.
- **No authorization check against `clientId` enumeration** — rejected, out of scope by intent — The entire backend's real auth model (OAuth2/OIDC via corporate SSO) is already Deferred in the architecture spine; introducing an ad hoc authorization check on one endpoint would contradict that already-settled deferral, not fill a gap this story created.

## Design Notes

The kopecks and ISO-date corrections mean this endpoint's numbers/dates will not look like the current frontend mock's raw values side-by-side — that's intentional (AD-3, and the dates convention from Story 1), not a mismatch to fix. Wiring the frontend to consume the corrected wire format is out of scope here, same as Story 1's `openCaseIds` divergence.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build card-account-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `card-account-service` and its Postgres report healthy
  - **Result: PASS.** Full 20-container stack (gateway + 9 services + 9 postgres + kafka) all reported healthy, including `card-account-service` and `card-account-service-postgres`; the other 8 services unaffected.
- `curl -s http://localhost:8080/card-account/clients/client-1/products/debit-mts-dengi/cards` -- expected: `200`, 2 cards with kopecks/ISO-date values
  - **Result: PASS.** `[{"id":"card-4019","product":"Zero","masked":"•••• 4019","subtitle":"8_MC World Zero RUR","isMain":true,"status":"active","balance":1243000,"issuedAt":"2025-01-12","account":"****6272"},{"id":"card-7274","product":"ZERO","masked":"•••• 7274","subtitle":"8_MC World Zero RUR · Виртуальная","isMain":false,"status":"closed","balance":0,"closedAt":"2025-10-20"}]` -- HTTP 200; kopecks (`1243000`/`0`) and ISO dates confirmed; `card-4019` has no `closedAt` key and `card-7274` has neither `account` nor `issuedAt`, exactly mirroring `mockData.cards`' per-row shape.
- `curl -s http://localhost:8080/card-account/clients/nope/products/debit-mts-dengi/cards` -- expected: `200`, `[]`
  - **Result: PASS.** `[]`, HTTP 200.
- `curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/card-account/clients/client-1/products/debit-mts-dengi/cards` -- expected: `405`
  - **Result: PASS.** `405` with body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`, not `500`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `mvn -f backend/card-account-service/pom.xml test` (run inside an `eclipse-temurin:25` container per Story 0's host-JDK constraint, see Implementation Notes): `Tests run: 4, Failures: 0, Errors: 0` -- `CardControllerIntegrationTest` against a real Testcontainers Postgres, exercising the Liquibase-applied schema/seed and all three I/O-matrix rows plus the per-row field-presence assertion. Stack torn down afterward with `docker compose down`.
