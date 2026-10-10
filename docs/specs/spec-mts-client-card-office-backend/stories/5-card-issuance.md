---
title: 'Story 5 — CAP-5: Card issuance'
type: 'feature'
created: '2026-10-04'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '0529d64d8ec59f2e05edf60b211635b7867ad08f'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `card-issuance-service` (Story 0) only has a health endpoint. CAP-5 needs `getIssuance(clientId, productId)` served, plus the "reissue/new-card action creates a new application record" success criterion SPEC.md already promises — but unlike CAP-4's `sendDocument`/`printDocument`, **no `client.js` function exists for this action at all**: `IssuancePage.jsx`'s "Активировать карту"/"Начать оформление" buttons and `BalancePage.jsx`'s "Перевыпустить карту" button all have no `onClick` handler. There is no frontend call shape to mirror (AD-8 has nothing to point at).

**Approach:** Serve `GET /clients/{clientId}/products/{productId}/issuance` mirroring `mockData.issuance` exactly (with the established money-free but date-bearing ISO-8601 correction to `history`), and **design a new endpoint from scratch** — `POST /clients/{clientId}/products/{productId}/issuance/applications` — covering exactly the one action SPEC.md's CAP-5 already commits to (creating a new application record), not the other two dead buttons, which stay out of scope.

## Boundaries & Constraints

**Always:**
- `GET` response mirrors `mockData.issuance`'s three top-level fields (`currentApplication`, `newCard`, `history`) exactly in shape — AD-8 (this is still mirroring the one real client.js call, `getIssuance`).
- `currentApplication` and `newCard` are static (not backed by a table) — same simplification Story 2 made for `offers`, since nothing in the mock or any button ever changes them.
- `history` is a real, persisted, ordered list (Liquibase + JPA) seeded with the three mock rows in their exact given order (not date-sorted — the mock's own order isn't chronological: `app1` is the newest, `app2`/`app3` are older but not in descending order either). A `sort_order` column preserves this, same pattern as Stories 1/3's ordered lists.
- `history[].date` is ISO-8601. `app1`'s mock value (`"01.09"`) has no year; read as day.month of the demo's current year (2026) per this session's own dating — becomes `"2026-09-01"`. `app2`/`app3` already carry a year and convert directly (`"2025-01-12"`, `"2025-10-20"`).
- `POST .../issuance/applications` — **a new endpoint invented for this story, no existing client.js call to mirror** — accepts `{"card": string, "method": string}`, both required; creates one new `history` row with a generated id (`UUID`, visibly different from the seeded `appN` ids — these are two different provenances, not meant to look alike), today's server date, `status: "В обработке"` (reusing the exact status string `mockData.operations` already uses for a pending state, not inventing new vocabulary), and returns `201` with the created row. It does **not** touch `currentApplication`.
- A missing/blank `card` or `method` in the `POST` body returns `400` with `{"error":{"code":"VALIDATION_ERROR",...}}` — the first real use of this code (established in Story 1, unused until now).

**Never:**
- No endpoint for "Активировать карту" or "Изменить способ получения" — SPEC.md's CAP-5 success criterion names only the reissue/new-card action; the other two dead buttons stay unimplemented and out of scope.
- No modification of `currentApplication`/`newCard` by the `POST` — they remain static for this story.
- No real correspondence with a card-manufacturing/delivery system — the new application record is just a persisted row, consistent with every other stub-backed concern in the spine; Deferred.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Get issuance state | `GET /clients/client-1/products/debit-mts-dengi/issuance` | `200`, matches `mockData.issuance` (ISO dates in `history`, same row order) | N/A |
| Create application | `POST .../issuance/applications` `{"card":"Zero","method":"Курьер"}` | `201`, the new row (generated id, today's date, `status:"В обработке"`), and a follow-up `GET` shows it appended to `history` | N/A |
| Create application, missing field | `POST .../issuance/applications` `{"card":"Zero"}` | — | `400` with `{"error":{"code":"VALIDATION_ERROR",...}}` |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `card-issuance` owns issuance applications and history
- `ARCHITECTURE-SPINE.md` AD-8 — REST shape mirrors `client.js` where a call exists; this story's `POST` has none to mirror, called out explicitly above rather than silently invented
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase; new changeset, same immutability discipline as every prior story
- `ARCHITECTURE-SPINE.md` § Consistency Conventions — `VALIDATION_ERROR` code (defined Story 1, first real use here), ISO-8601 dates
- `src/api/mockData.js` `issuance` export — exact fixture shape/values to mirror for `GET`
- `src/api/client.js` `getIssuance(clientId, productId)` — the one real call this story serves
- `src/pages/IssuancePage.jsx` — confirms all three action buttons have no `onClick` (dead UI); the "История заявок" table's columns (`date`, `card`, `method`, `status`) ground `history`'s row shape
- `src/pages/BalancePage.jsx` — confirms "Перевыпустить карту" is likewise a dead button (`canReissue`), not a lead to a second mutation endpoint
- `src/pages/OperationsPage.jsx` / `mockData.operations` — source of the `"В обработке"` status string reused for a new application's initial state
- `backend/client-identity-service/`, `backend/card-account-service/` — established hexagonal/Liquibase/`ResponseEntityExceptionHandler` shape to copy; the latter's `handleExceptionInternal` already generically handles `MethodArgumentNotValidException` (Bean Validation) → this story is the first to actually trigger that path
- `backend/gateway/src/main/resources/application.yml` — existing `/card-issuance/**` route already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `.../domain/IssuanceApplication.java` -- framework-free entity (`id`, `date`, `card`, `method`, `status`), `CurrentApplication`/`NewCardInfo` static value objects (hardcoded, matching the mock's static text), `IssuanceRepository` port (`List<IssuanceApplication> findAllOrdered()`, `IssuanceApplication save(...)`)
- [x] `.../application/GetIssuanceUseCase.java`, `CreateApplicationUseCase.java` -- the latter validates `card`/`method` (Jakarta Bean Validation on the request DTO, relying on `CardExceptionHandler`'s existing generic `handleExceptionInternal` for the `400`)
- [x] `.../adapters/in/web/IssuanceController.java` + DTOs (`IssuanceResponse`, `CreateApplicationRequest` with `@NotBlank` fields, `ApplicationResponse`) + `IssuanceExceptionHandler extends ResponseEntityExceptionHandler` (the corrected shape from the start, per Stories 1-3's lesson) -- `GET /clients/{clientId}/products/{productId}/issuance`, `POST /clients/{clientId}/products/{productId}/issuance/applications`
- [x] `.../adapters/out/persistence/` -- JPA entity for `history` rows only (not for the static `currentApplication`/`newCard`), Spring Data repository, adapter implementing the domain port
- [x] `backend/card-issuance-service/src/main/resources/db/changelog/` -- `issuance_applications` table (`sort_order` for seeded-order preservation) + one changeset seeding the three mock rows with ISO dates
- [x] `backend/card-issuance-service/src/test/java/.../IssuanceControllerIntegrationTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` against Testcontainers Postgres, covering all three I/O-matrix rows

**Acceptance Criteria:**
- [x] Given the seeded data, when `GET /clients/client-1/products/debit-mts-dengi/issuance` is called, then the response is `200` matching `mockData.issuance` in shape and row order, with `history[].date` in ISO-8601.
- [x] Given a valid body, when `POST .../issuance/applications` is called, then the response is `201` with the new row, and a subsequent `GET` shows it appended to `history` (unchanged `currentApplication`/`newCard`).
- [x] Given a body missing `card` or `method`, when `POST .../issuance/applications` is called, then the response is `400` with the error envelope and `code: "VALIDATION_ERROR"`.
- [x] Given the stack is brought up via `docker compose up -d --wait` (unchanged compose file), when the same requests are made through the Gateway, then the same responses come back.

## Implementation Notes

- `CurrentApplication`/`NewCardInfo` carry their hardcoded fixture values via a static `fixture()` factory method on each class itself (not a port+stub-adapter pair like `offers-service`'s `OfferEnginePort`/`StubOfferEngineAdapter`): AD-10's port abstraction is reserved for actual external-system facades (`risk-blocks`, `offers`), and these two values have no such future swap-in point named anywhere in the spine, so the simpler static-factory shape was used instead.
- `IssuanceApplicationEntity`'s date column is named `application_date`, not `date`: no functional requirement forces this, but it keeps the column name unambiguous across SQL tooling without relying on `date` not being a reserved word in Postgres specifically.
- `JpaIssuanceRepository.save` assigns the next `sort_order` as `(select max(sort_order) from issuance_applications) + 1` via a small `@Query`, not a row count, so a newly created application is always appended after every existing row even if some were ever removed.
- Caught before it ran: the first draft of `001-create-issuance-applications-and-seed.xml`'s header comment used `--` inside an XML `<!-- -->` comment (the same trap Stories 3/4's own Implementation Notes already name as reproduced in their changelogs). Liquibase's XML parser rejected the changelog outright (`"databaseChangeLog" expected as root element`). Fixed the same way those stories did: reworded the comment instead of working around the parser.
- Verified full-stack via `docker compose build/up --wait` plus the exact `curl` commands in this story's Verification section, through the Gateway (`localhost:8080/card-issuance/...`), in addition to the Testcontainers integration test. The reactor's full `mvn test` (all nine services + gateway) was also re-run and remains green, confirming no other module regressed.
- Host Java is 17, but the stack pins Java 25 (ARCHITECTURE-SPINE.md Stack); verification used a locally-fetched Temurin 25 JDK against the host's own Docker daemon (Testcontainers nested inside a container hit Docker-in-Docker networking issues in this sandboxed session, unrelated to the implementation) and, for the Docker-image build/run itself, the project's own `Dockerfile`-based build.
- **Post-review patches:** `MethodArgumentNotValidException` now builds its message from `BindingResult.getFieldErrors()` instead of the raw exception string; `CreateApplicationRequest` gained `@Size(max=255/64)` matching the entity's column widths; a new changeset `002-add-sort-order-unique-constraint.xml` adds a unique constraint on `sort_order` (changeset `001` untouched); the integration test gained `@TestMethodOrder`/`@Order` pins plus a new sequential-two-create test. The same `--`-in-XML-comment mistake was reproduced and fixed again while writing the new changeset's header comment.
- **Cross-service retroactive fix (same root cause found here, applied everywhere):** `handleExceptionInternal` in this service, `card-account-service`, `client-identity-service`, and `offers-service` all silently dropped Spring's framework-computed `headers` (e.g. `Allow: GET` on a `405`) — introduced by the Story 2/3 exception-handler fix, uncaught until this story's review. All four now call `.headers(headers)` on the response.

## Spec Change Log

## Review Triage Log

- **`400` error message for a missing/blank field leaks the full Spring internal exception string** — `medium` — Verified independently (not just by a reviewer): `curl` with `{"card":"Zero"}` returned `message` containing the entire `MethodArgumentNotValidException` toString — controller/method signature, parameter index, binding-result internals. → **patch**: extract a clean message from the `BindingResult`'s field errors (e.g. `"method: must not be blank"`) instead of `exception.getMessage()` for this exception type.
- **Missing `@Size` bounds on `CreateApplicationRequest`, so an over-length value 500s instead of 400ing** — `medium` — Verified: `card`/`method` carry only `@NotBlank`; the backing columns are `varchar(255)`/`varchar(64)`. A too-long value passes Bean Validation, then fails at JPA flush with an unchecked exception that falls through to the catch-all `Exception.class` handler → `500`, defeating the story's own "surface client input errors as clean `400`" goal. → **patch**: add `@Size(max = 255)`/`@Size(max = 64)` matching the entity's column lengths.
- **Unguarded race in `sort_order` assignment; no schema constraint to at least fail loudly** — `low` (schema-hardening slice patched; full concurrency-safe redesign deferred) — Verified: `JpaIssuanceRepository.save` computes `findMaxSortOrder()+1` and inserts in two non-atomic steps, no `SELECT...FOR UPDATE`/sequence/unique constraint; two concurrent `POST`s could land duplicate `sort_order` values with non-deterministic resulting order, and no test exercises two creates. A full fix (DB sequence or row-level locking) is more machinery than this demo-scale, single-operator story committed to. → **patch**: add a unique constraint on `sort_order` (turns a silent race into a loud, debuggable failure rather than silent corruption) and add a test creating two applications sequentially asserting distinct, correctly-ordered `sort_order`. → **defer**: the atomic-allocation redesign itself.
- **`handleExceptionInternal`'s `Allow`/other framework-computed headers are silently dropped from every mapped error response** — `medium` — Verified: the method signature receives `HttpHeaders headers` (Spring populates e.g. `Allow: GET` for a `405`) but the response is built as `ResponseEntity.status(statusCode).body(...)`, never calling `.headers(headers)`. **This is pre-existing in `client-identity-service`, `offers-service`, and `card-account-service` too** — introduced by the Story 2/3 cross-service fix, never caught until this story's review. → **patch**: thread `headers` into the response builder in all four services.
- **Shared Testcontainers/Spring context across all 4 test methods with no ordering pin or cleanup, risking order-dependent false failures** — `low` — Verified: `IssuanceControllerIntegrationTest` has no `@TestMethodOrder`, `@Transactional` rollback, or `@DirtiesContext`; the size-3 assertion in the GET test would fail if JUnit happened to run the create-test first — a false failure, not a missed regression, but a real flakiness risk for a pattern future stories will keep copying. → **patch**: pin method execution order (`@TestMethodOrder`/`@Order`).
- **4xx statuses other than real validation failures (e.g. `405`) are labeled `"VALIDATION_ERROR"`** — `low`, rejected — Pre-existing, deliberate design from Story 1's minimal 4-code vocabulary (`NOT_FOUND`/`VALIDATION_ERROR`/`INTERNAL_ERROR`/`UPSTREAM_UNAVAILABLE`), already shipped identically in 3 prior services; not a defect this story introduced, and adding a 5th code for this distinction is a spine-level vocabulary change with no current consumer that needs it.
- **No idempotency protection against a double-submitted `POST`** — `low`, rejected — Speculative; no deduplication requirement exists anywhere in this project, and an office tool tolerating a double-submit (manually correctable) is consistent with the demo's scope.
- **`clientId`/`productId` accepted without validating they correspond to anything real** — rejected, out of scope by design — Explicitly documented in this story's own Boundaries (`history` is global in this demo), matching the identical simplification Stories 2 and 4 already made for the same reason.
- **Unbounded `history` growth with no pagination** — `low`, rejected — Speculative hardening for a scale concern that doesn't exist in a demo with one seeded client.
- **Tasks & Acceptance checked off `[x]` while the story's own log said no review had happened** — acknowledged, no code action — A workflow-process inconsistency from the implementation pass (it set `status: done` prematurely); corrected by the coordinating session reverting to `in-review` before this review ran, same as every prior story's pattern.
- **No test for a malformed/type-mismatched JSON body** — `low`, rejected — The generic 4xx path this would exercise (`HttpMessageNotReadableException`) is the same code path already covered by the missing-field and 405 tests; a third framework-exception subtype adds diminishing coverage.
- **Code Map's claim that validation "relies on `CardExceptionHandler`'s... `handleExceptionInternal`" is literally false** — `false` — `IssuanceExceptionHandler` is its own class (correctly — services can't share a class across independent deployables); read alongside the Tasks list's precise wording ("`IssuanceExceptionHandler extends ResponseEntityExceptionHandler`, the corrected shape... per Stories 1-3's lesson"), the Code Map's phrasing means the same corrected *pattern*, not a literal cross-service import — not actually misleading.

## Design Notes

This is the first story with no frontend call to mirror for its mutation — the endpoint shape (`POST .../issuance/applications`, the request body fields, the generated-UUID id scheme) is an invented design filling SPEC.md's own already-committed success criterion, not a reverse-engineering of existing frontend behavior. If the frontend later wires up "Начать оформление"/"Перевыпустить карту" to a real call, that integration work (and any shape mismatch it surfaces) belongs to whichever story does it, not this one.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build card-issuance-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml down -v && up -d --wait` -- expected: clean-volume start, `card-issuance-service` and its Postgres report healthy
  - **Result: PASS.** All 20 containers healthy, both changesets (`001`, `002-add-sort-order-unique-constraint`) applied; `\d issuance_applications` confirms `uq_issuance_applications_sort_order` present.
- `curl -s http://localhost:8080/card-issuance/clients/client-1/products/debit-mts-dengi/issuance` -- expected: `200`, matches `mockData.issuance` with ISO dates
  - **Result: PASS.** 3 seeded history rows in the mock's exact order; `app1` dated `2026-09-01`.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"card":"Zero","method":"Курьер"}' http://localhost:8080/card-issuance/clients/client-1/products/debit-mts-dengi/issuance/applications` -- expected: `201`, new row returned
  - **Result: PASS.** Verified a second sequential create too: both land with distinct ids, appended in call order, visible in a follow-up `GET`.
- `curl -s -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{"card":"Zero"}' http://localhost:8080/card-issuance/clients/client-1/products/debit-mts-dengi/issuance/applications` -- expected: `400`
  - **Result: PASS after a fix.** Before the patch, `message` leaked the full internal exception string; now `{"error":{"code":"VALIDATION_ERROR","message":"method: must not be blank"}}`. Also independently checked an over-length `card` value: `400` with `"card: size must be between 0 and 255"`, not a `500`.
- Post-patch cross-service check (not in the original list): `curl -i -X POST` against a `GET`-only endpoint on all four patched services.
  - **Result: PASS.** All four now return `Allow: GET` on their `405` response; previously silently dropped.
- Module test suite -- expected: all pass
  - **Result: PASS.** `card-issuance-service` 6/6 (including the new ordering test), plus `card-account-service` 11/11, `client-identity-service` 10/10, `offers-service` 6/6 re-verified unaffected by the cross-service header fix. Stack torn down afterward.
