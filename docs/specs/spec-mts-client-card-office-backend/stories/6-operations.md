---
title: 'Story 6 — CAP-6: Operations history'
type: 'feature'
created: '2026-10-10'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'aaeaf72e5046a018f1b5d4f4499611eb1896094a'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `operations-service` (Story 0) only has a health endpoint. CAP-6 needs the frontend's `getOperations(cardId)` call served with a card's operation history, including any failed-transfer alert.

**Approach:** Add the operations schema via Liquibase (AD-12) and a hexagonal slice implementing `GET /cards/{cardId}/operations`, seeded from `mockData.operations` — scoped to `card-4019` (client-1's active card, per Story 3's seed) — with the AD-3 kopecks and ISO-8601-date corrections already established, but deliberately keeping `status` as the mock's literal Russian string, unlike every other normalized field.

## Boundaries & Constraints

**Always:**
- Response shape `{failedAlert: {title, text} | null, rows: [{id, date, op, sum, status}]}` at `GET /cards/{cardId}/operations`, mirroring `mockData.operations` — AD-8.
- `sum` is a signed integer in kopecks (AD-3) — the mock's `"−5 000 ₽"` / `"+450 ₽"` become `-500000` / `45000`, never a formatted string.
- `date` is ISO-8601. The mock's `"12.09"` has no year; `failedAlert.text`'s embedded `"12.09.2026"` pins the year for every row (all five fall within the same September) — `"2026-09-12"` etc.
- `status` stays the mock's exact literal Russian string (`"Отклонён"` / `"Проведена"` / `"В обработке"`) unchanged — `src/components/ui/Pill.jsx`'s `STATUS_TONE` map keys off this exact string, so normalizing it to an enum code would silently break the frontend's tone lookup.
- Rows ordered newest-first (`date` descending), matching mock order.
- Operations reference their card by `cardId` only (AD-2) — no cross-service call to `card-account-service` to validate it (AD-9).
- This service's `@RestControllerAdvice` is written in the already-final shape from the start (extends `ResponseEntityExceptionHandler`, overrides `handleExceptionInternal` calling `.headers(headers)`, uses `statusCode.is4xxClientError()`/`is5xxServerError()` directly, clean `MethodArgumentNotValidException` message) — the shape every other service needed three separate retroactive fixes to reach. Copy that final shape, not an intermediate one.

**Never:**
- No write/mutation endpoints — CAP-6's success criterion is read-only.
- No `NOT_FOUND` for an unknown `cardId` — this is a collection-shaped endpoint; any `cardId` without seeded history returns `200` with `{failedAlert: null, rows: []}`, the same 200-empty precedent Story 3 set for an unmatched `clientId`/`productId`.
- Only `card-4019` gets the fixture rows + alert; `card-7274` (Story 3's other seeded card) and any other `cardId` get the empty shape — operation history is inherently per-card, unlike Story 2's offer which is identical for every client. This is a deliberate scoping call, not a gap.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known card with history | `GET /cards/card-4019/operations` | `200`, `failedAlert` + 5 rows matching `mockData.operations` with kopecks/ISO corrections, `status` unchanged | N/A |
| Known card, no history | `GET /cards/card-7274/operations` | `200`, `{failedAlert: null, rows: []}` | N/A |
| Unknown card | `GET /cards/nope/operations` | `200`, `{failedAlert: null, rows: []}` | N/A |
| Non-GET method | `POST /cards/card-4019/operations` | — | `405` with the spine's error envelope, `Allow` header present |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `operations` owns operation history per card; AD-2 — reference by `cardId` only; AD-3 — money as integer kopecks; AD-8 — REST shape mirrors `client.js`; AD-9 — no synchronous inter-service calls; AD-12 — Liquibase migrations
- `src/api/mockData.js` export `operations` — exact fixture; `failedAlert.text`'s `"12.09.2026"` is what pins the year for all 5 row dates
- `src/api/client.js` `getOperations(cardId)` → `GET /cards/{cardId}/operations`
- `src/components/ui/Pill.jsx` `STATUS_TONE` — proof `status` must stay the literal Russian string
- `backend/card-account-service/` (Story 3) — sibling precedent for entity/JPA-repository layering, the `001-create-schema` + `002-seed` changeset split, and the already-fixed `CardExceptionHandler` shape (copy verbatim, adjust package)
- `backend/operations-service/` (Story 0) — existing empty `pom.xml`/`application.yml`/package-info scaffold to build into
- `backend/gateway/src/main/resources/application.yml` — existing `/operations/**` route (`StripPrefix=1`) already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `db/changelog/changes/001-create-operations-schema.xml` -- create `operations` (id, card_id, op_date, description, amount_minor_units, status) and `operation_alerts` (card_id PK, title, text) tables -- AD-12; two tables because an alert is a 0-or-1-per-card sub-aggregate, not a row field
- [x] `db/changelog/changes/002-seed-operations-card-4019.xml` -- seed the 5 `mockData.operations.rows` for `card-4019` + its one `operation_alerts` row -- nothing seeded for `card-7274`
- [x] `domain/Operation.java`, `domain/FailedTransferAlert.java`, `domain/OperationsHistory.java` -- framework-free value objects; `OperationsHistory` aggregates a nullable alert + ordered rows
- [x] `domain/OperationsRepository.java` -- port, `OperationsHistory getHistory(String cardId)`
- [x] `application/GetOperationsUseCase.java` -- orchestrates the port
- [x] `adapters/out/persistence/OperationEntity.java`, `OperationAlertEntity.java`, their Spring Data interfaces, `JpaOperationsRepository.java` -- maps both tables, assembles `OperationsHistory`, orders rows by `op_date DESC`
- [x] `adapters/in/web/OperationsController.java` + response DTOs -- `GET /cards/{cardId}/operations`
- [x] `adapters/in/web/OperationsExceptionHandler.java` + `ErrorResponse.java` -- final shape from the start (see Boundaries)
- [x] `src/test/.../OperationsControllerIntegrationTest.java` -- covers all four I/O Matrix rows against a real Testcontainers Postgres

**Acceptance Criteria:**
- Given the seed, when `GET /cards/card-4019/operations` is called directly on the service, then `200` with `failedAlert` + 5 rows, `sum` as signed kopecks, `date` as ISO-8601, `status` unchanged, newest-first.
- Given `card-7274`, when `GET /cards/card-7274/operations` is called, then `200` with `{failedAlert: null, rows: []}`.
- Given the stack raised via `docker compose up -d --wait`, when `GET http://localhost:8080/operations/cards/card-4019/operations` is called through the Gateway, then the same `200` payload arrives.
- Given a non-`GET` method, when `POST` is sent, then `405` with the spine's error envelope and an `Allow` header.

## Implementation Notes

- The `operations`/`operation_alerts` two-table split, the `OperationsHistory` aggregate shape, and the `OperationsExceptionHandler` final shape (copied verbatim from `card-issuance-service`'s `IssuanceExceptionHandler`) all landed exactly as planned in Code Map/Tasks — no deviation.
- Liquibase changelog comments were written carefully to avoid a literal `--` inside `<!-- -->` blocks this time (the bug that has bitten Stories 1, 3, 4, and 5) — no repeat.
- **Post-review patch:** `OperationJpaRepository.findByCardIdOrderByOpDateDesc` had no secondary sort key, so two operations sharing the same `op_date` would have had an unspecified relative order despite the Javadoc's "deterministic" claim. Renamed to `findByCardIdOrderByOpDateDescIdAsc` (adding `id` ascending as the tiebreaker) and updated its one call site in `JpaOperationsRepository`. Today's fixture has 5 distinct dates so this never manifested visibly, but the fix removes the latent nondeterminism rather than leaving it for a future story's seed to trip over.
- Full-stack re-verification (clean `docker compose up -d --wait`, all 20 containers) performed independently both before and after the patch: all 4 I/O Matrix rows confirmed via `curl` through the Gateway, plus the `operations`/`operation_alerts` table schema inspected directly in Postgres (`\d`) to confirm the PK/index/column shape matches the spec exactly. Module test suite re-run independently in a throwaway `eclipse-temurin:25` container (host JDK is 17, per the project's Docker-only build constraint): `Tests run: 4, Failures: 0, Errors: 0`.

## Spec Change Log

## Review Triage Log

- **Finding (Blind Hunter):** diff has no sibling `6-operations.ru.md` translation. **Verdict: false.** The Russian duplicate exists on disk (`docs/specs/spec-mts-client-card-office-backend/stories/6-operations.ru.md`, created alongside spec approval); it was simply outside the path scope the reviewer's diff was generated with (`backend/operations-service` + the English `.md` only), not a real gap.
- **Finding (Blind Hunter):** `OperationsExceptionHandler.handleExceptionInternal`'s `else if (statusCode.is4xxClientError())` branch forwards raw `exception.getMessage()` unscrubbed, unlike the `MethodArgumentNotValidException` branch above it. **Verdict: medium if real, but defer.** Byte-for-byte identical to the already-shipped `ClientExceptionHandler`/`OfferExceptionHandler`/`CardExceptionHandler`/`IssuanceExceptionHandler` in all four prior services — this story's frozen Boundaries & Constraints explicitly mandate copying that exact final shape, not inventing a stricter one. Pre-existing across the whole codebase, not caused by this story.
- **Finding (Blind Hunter):** no test exercises the generic `INTERNAL_ERROR`/500 branch or `handleUnexpected`. **Verdict: low, defer.** Same gap exists identically in every sibling service's exception-handler tests (none of their read-only slices have a natural trigger for an unmapped exception either); not introduced here.
- **Finding (Blind Hunter):** seed `op_date` values in `002-seed-operations-card-4019.xml` are manually back-derived from the year embedded in `failedAlert.text`, with no automated check keeping them in sync if either is edited later. **Verdict: low, rejected.** Unlikely a developer edits one without the other in a static, already-shipped seed file; a sync guard is more than a direct correction.
- **Finding (Blind Hunter):** `OperationJpaRepository.findByCardIdOrderByOpDateDesc` has no secondary sort key, so the Javadoc's "deterministic newest-first" claim only holds because today's 5 fixture rows have distinct dates — two same-day operations would have unspecified relative order. **Verdict: low, patch.** Trivial direct fix: add `card_id` id as a secondary key.
- **Finding (Blind Hunter):** no seeded fixture/test covers "card has rows but no alert" — the single most common real-world shape per `OperationsHistory`'s own Javadoc. **Verdict: rejected.** The alert lookup (`findById` on `operation_alerts`) and the rows query are fully independent, unbranching code paths already exercised separately (rows+alert together in the card-4019 test, neither in the empty-card tests); a third seeded card would add coverage-completism, not catch a plausible bug, and the frozen Boundaries & Constraints explicitly scope the fixture to `card-4019` only — inventing a new seeded card contradicts that deliberate scoping decision.
- **Finding (Blind Hunter):** no test confirms a genuinely unmapped path (no matching route at all, as opposed to a matched route with an unknown `cardId`) still returns the spine's JSON error envelope rather than Spring Boot's default error page. **Verdict: medium if real, but defer.** This is a service-wide Spring Boot default-error-handling question identical across all nine services' scaffolds; not introduced or affected by this story's diff.
- **Finding (Blind Hunter):** no test covers an edge-case `cardId` (very long, special/URL-reserved, non-ASCII) despite the story promising `200` for "any `cardId`". **Verdict: false.** `cardId` only ever flows into a parameterized JPA query parameter (`findByCardIdOrderByOpDateDesc(String)` / `findById(String)`) — never concatenated or parsed — so there is no code path that could behave differently for unusual characters; the existing "unknown card" test already proves the general empty-result behavior for any value not present in the table.
- **Finding (Edge Case Hunter):** none reported (empty findings list).
- **Finding (Verification Gap):** none reported — confirmed all 4 I/O matrix rows are covered by `OperationsControllerIntegrationTest`, run against real Testcontainers Postgres, and pass.

## Design Notes

The two-table split (`operations` rows vs. `operation_alerts`) avoids nullable alert columns on every row and mirrors Story 3's precedent of separate tables for separate concepts on the same `cardId` (`cards` vs. `documents`). Through the Gateway the path reads `.../operations/cards/{cardId}/operations` — "operations" appears twice (once as the Gateway's service-routing prefix, stripped before forwarding; once as the actual resource segment, mirroring `client.js`) — the same incidental doubling Story 2 already accepted for `offers`, not a defect to fix here.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build operations-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `operations-service` and its Postgres report healthy
  - **Result: PASS.** Full 20-container stack healthy, both before and after the post-review patch. `operation_alerts`/`operations` schema inspected directly via `psql \d`: PK on `operations.id`, index `ix_operations_card_id`, PK on `operation_alerts.card_id` — matches spec exactly.
- `curl -s http://localhost:8080/operations/cards/card-4019/operations` -- expected: `200`, matches `mockData.operations` with kopecks/ISO corrections
  - **Result: PASS.** `failedAlert` + 5 rows, newest-first, kopecks signed correctly (`-500000`…`+45000`), dates `2026-09-08`…`2026-09-12`, `status` unchanged Russian strings.
- `curl -s http://localhost:8080/operations/cards/card-7274/operations` -- expected: `200`, `{"failedAlert":null,"rows":[]}`
  - **Result: PASS.**
- `curl -s http://localhost:8080/operations/cards/nope/operations` -- expected: same empty `200` shape for a wholly unknown card (not in the original command list, added independently)
  - **Result: PASS.**
- `curl -i -X POST http://localhost:8080/operations/cards/card-4019/operations` -- expected: `405`, `Allow` header present
  - **Result: PASS.** `Allow: GET`, body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `Tests run: 4, Failures: 0, Errors: 0` — re-run independently in a throwaway `eclipse-temurin:25` container against real Testcontainers Postgres, both before and after the post-review ordering-tiebreaker patch. Stack torn down after verification.
