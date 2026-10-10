---
title: 'Story 7 — CAP-7: Risk blocks'
type: 'feature'
created: '2026-10-10'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '84368164e22bb34cbcdc4329168913abaa107122'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `risk-blocks-service` (Story 0) only has a health endpoint. CAP-7 needs the frontend's `getBlocks(cardId)` call served with SFM / FSSP-arrest / antifraud block status, each sourced through its own external-system facade.

**Approach:** Implement `GET /cards/{cardId}/blocks` as three independent AD-10 stub ports (`SfmPort`, `FsspPort`, `AntifraudPort`), each with its own stub adapter in the Story-0-scaffolded `adapters/out/external/{sfm,fssp,antifraud}/` packages, composed into one response — mirroring `offers-service`'s already-shipped single-stub-port pattern, times three. One deliberate wire-format correction the mock doesn't make: the arrest block's amount (AD-3 explicitly binds "`risk-blocks` (arrest sum)") is pulled out into a structured kopecks field alongside the unchanged display text.

## Boundaries & Constraints

**Always:**
- Response shape `{sfm: {title, text}, arrest: {title, text, amount}, af: {title, text}}` at `GET /cards/{cardId}/blocks`, mirroring `mockData.blocks` — AD-8.
- `arrest.amount` is a signed integer in kopecks (AD-3) — the mock's `"сумма ареста 15 000 ₽"` embedded in `arrest.text` becomes a new field, `1500000`; `arrest.text` stays byte-for-byte the mock's string (the frontend renders it as-is and this story makes no frontend change).
- Each block is served through its own port (`SfmPort`, `FsspPort`, `AntifraudPort`) and its own stub adapter (AD-10) — one port per external system, never a shared/catch-all port.
- All three blocks are unconditionally present for any `cardId` in this demo fixture, matching `offers-service`'s `StubOfferEngineAdapter` precedent (unconditional fixture regardless of input) and `BlocksPage.jsx`'s current unconditional `blocks.sfm.title`/`blocks.arrest.title`/`blocks.af.title` access with no null guards. CAP-7's "whichever blocks are active" (SPEC.md) is, for this stub-backed demo, this fixture's entire always-active set.
- `cardId` accepted as-is, never validated against `card-account-service` (AD-9).
- This service's `@RestControllerAdvice` ships in the already-final shape from the start (same shape every service has carried since Story 6).

**Never:**
- No persistence, no Liquibase changeset — like `offers-service` (Story 2), `risk-blocks-service-postgres` stays provisioned but unused; this is a pure stub-port facade with no data of its own to own.
- No simulated upstream failures in the stub adapters — matching `offers-service`'s deterministic-fixture-only precedent. `UPSTREAM_UNAVAILABLE` is already reserved in the shared error-code vocabulary (resolved during Story 1 planning — see Code Map) for a future real-integration failure path; this story does not manufacture one synthetically.
- `sfm.text`'s own embedded amount (`"проверка операции на 5 000 ₽"`) is NOT extracted into a field — AD-3's binding names "arrest sum" specifically; the SFM figure describes which transaction triggered the check, not a block-level monetary fact like the arrest's seizure amount.
- No write/mutation endpoints — CAP-7's success criterion is read-only.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Any card | `GET /cards/card-4019/blocks` | `200`, `sfm`+`arrest`+`af` matching `mockData.blocks`, `arrest.amount` added as `1500000` | N/A |
| Different card | `GET /cards/card-7274/blocks` | `200`, identical fixture (unconditional — not yet personalized, same as Story 2's offer) | N/A |
| Non-GET method | `POST /cards/card-4019/blocks` | — | `405` with the spine's error envelope, `Allow` header present |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `risk-blocks` facades 3 stub adapters; AD-3 — binds "`risk-blocks` (arrest sum)" to kopecks; AD-8 — REST shape mirrors `client.js`; AD-9 — no synchronous inter-service calls; AD-10 — one port per external system (`SfmPort`/`FsspPort`/`AntifraudPort`)
- `ARCHITECTURE-SPINE.md` Consistency Conventions — error-code vocabulary (`NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`, `UPSTREAM_UNAVAILABLE`) already resolved during Story 1 planning — this closes SPEC.md's former open question 1 and `stories.yaml`'s Story 7 `invoke_dev_with` note; no new decision needed, `UPSTREAM_UNAVAILABLE` stays reserved and unused by this story's deterministic stub (see Boundaries)
- `src/api/mockData.js` export `blocks` — exact fixture; `arrest.text`'s `"15 000 ₽"` is what gets extracted into `amount`
- `src/api/client.js` `getBlocks(cardId)` → `GET /cards/{cardId}/blocks`
- `src/pages/BlocksPage.jsx` — unconditional `blocks.sfm.title`/`blocks.arrest.title`/`blocks.af.title` access, no null guards — proof all three blocks must always be present
- `backend/offers-service/` (Story 2) — sibling precedent for the entire shape: `OfferEnginePort`/`StubOfferEngineAdapter`'s unconditional-fixture pattern, `Offer`/`GetTopOfferUseCase`/`OfferController`/`OfferExceptionHandler` layering (copy shape, times three ports here), and `offers-service-postgres` staying provisioned-but-unused
- `backend/risk-blocks-service/` (Story 0) — existing empty scaffold; already has `adapters/out/external/{sfm,fssp,antifraud}/package-info.java` pre-created for this story
- `backend/gateway/src/main/resources/application.yml` — existing `/risk-blocks/**` route (`StripPrefix=1`) already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `domain/SfmBlock.java`, `ArrestBlock.java`, `AntifraudBlock.java` -- framework-free value objects; only `ArrestBlock` carries `amountMinorUnits`
- [x] `domain/RiskBlocks.java` -- aggregate `{sfm, arrest, af}` returned by the use case
- [x] `domain/SfmPort.java`, `FsspPort.java`, `AntifraudPort.java` -- one port per external system (AD-10)
- [x] `application/GetRiskBlocksUseCase.java` -- calls all three ports, composes `RiskBlocks`
- [x] `adapters/out/external/sfm/StubSfmAdapter.java`, `adapters/out/external/fssp/StubFsspAdapter.java`, `adapters/out/external/antifraud/StubAntifraudAdapter.java` -- each returns its fixed fixture regardless of `cardId`, per `StubOfferEngineAdapter`'s precedent
- [x] `adapters/in/web/RiskBlocksController.java` + response DTOs -- `GET /cards/{cardId}/blocks`
- [x] `adapters/in/web/RiskBlocksExceptionHandler.java` + `ErrorResponse.java` -- final shape from the start
- [x] `src/test/.../RiskBlocksControllerIntegrationTest.java` -- `@SpringBootTest` + `WebTestClient`, DataSource/JPA autoconfig excluded (no real Postgres needed, per `OfferControllerIntegrationTest`'s precedent); covers all 3 I/O Matrix rows

**Acceptance Criteria:**
- Given the stub adapters, when `GET /cards/card-4019/blocks` is called directly on the service, then `200` with `sfm`+`arrest`+`af` matching `mockData.blocks`, `arrest.amount` equal to `1500000`.
- Given a different `cardId`, when `GET /cards/card-7274/blocks` is called, then `200` with the identical fixture.
- Given the stack raised via `docker compose up -d --wait`, when `GET http://localhost:8080/risk-blocks/cards/card-4019/blocks` is called through the Gateway, then the same `200` payload arrives.
- Given a non-`GET` method, when `POST` is sent, then `405` with the spine's error envelope and an `Allow` header.

## Implementation Notes

- The three-port/three-stub-adapter shape, the `RiskBlocks` aggregate, and the `RiskBlocksExceptionHandler` final shape (copied verbatim from `operations-service`'s `OperationsExceptionHandler`, itself the already-final form every service has carried since Story 6) all landed exactly as planned in Code Map/Tasks -- no deviation.
- `RiskBlocksResponse` is a single file with three nested records (`SfmResponse`/`ArrestResponse`/`AfResponse`), mirroring `OperationsResponse`'s nested-record precedent rather than three separate top-level DTO files.
- `risk-blocks-service/pom.xml` needed one addition beyond the Story 0 scaffold: a test-scoped `spring-boot-starter-webflux` dependency, so `WebTestClient` (used by `RiskBlocksControllerIntegrationTest`) is available against this Servlet-stack app -- the same addition `offers-service`'s pom already carries.
- `RiskBlocksServiceApplication.java`'s Javadoc was left unchanged (still says "No business capability is implemented here yet") -- matching `offers-service`'s own precedent of not updating that class's Javadoc once its capability landed.

## Spec Change Log

## Review Triage Log

- **Finding (Blind Hunter):** diff has no sibling `7-risk-blocks.ru.md` translation. **Verdict: false.** The Russian duplicate exists on disk, created alongside spec approval; it was outside the path scope the reviewer's diff was generated with, not a real gap.
- **Finding (Blind Hunter):** a wholly unmapped route (no matching handler at all) falls through to Spring Boot's default error page rather than the spine's JSON envelope. **Verdict: medium if real, but defer.** Identical, pre-existing gap across all nine services' scaffolds (same as Story 6's equivalent finding); not introduced or affected by this story's diff.
- **Finding (Blind Hunter):** the generic (non-validation, non-4xx) branch of `handleExceptionInternal` echoes raw `exception.getMessage()` while the separate `handleUnexpected` catch-all always returns a fixed generic message, inconsistently leaking detail depending on which path catches the failure. **Verdict: medium if real, but defer.** Byte-for-byte identical to every other service's exception handler since the shape was finalized in Story 6; pre-existing, not caused here.
- **Finding (Blind Hunter):** no unit test exercises `AntifraudBlock`/`ArrestBlock`/`SfmBlock`/`RiskBlocks`'s `Objects.requireNonNull` guards. **Verdict: rejected.** All three stub adapters only ever pass hardcoded non-null string/long literals to these constructors — there is no code path in this story that could pass null, so the guards are unreachable defensive code, not an undertested behavior.
- **Finding (Blind Hunter):** the `MethodArgumentNotValidException` branch is never exercised by any test. **Verdict: rejected.** Confirmed via grep: this service has zero `@Valid`/`@NotBlank`/`@NotNull`/`@Size` annotations anywhere — CAP-7 has no request body to validate (read-only, no mutation endpoints), so this branch is provably unreachable in this service, kept only for shape-parity with siblings that do use it.
- **Finding (Blind Hunter):** the `handleUnexpected`/500 `INTERNAL_ERROR` path has no test coverage. **Verdict: low, defer.** Same gap exists identically in every sibling service; none of their read-only slices have a natural trigger for an unrecognized exception either.
- **Finding (Blind Hunter):** `GetRiskBlocksUseCase.execute` calls all three stub ports sequentially rather than concurrently, which will become a latency concern once real network-backed adapters replace the stubs. **Verdict: rejected.** Premature optimization for a scenario AD-10 explicitly defers (the real integrations don't exist yet); today's stub calls are in-memory and instant. Revisit when a real adapter actually lands, not speculatively now.
- **Finding (Blind Hunter):** `RiskBlocksServiceApplication`'s class Javadoc still reads "No business capability is implemented here yet," left stale on purpose. **Verdict: rejected.** Confirmed `offers-service`'s equivalent `OffersServiceApplication` carries the identical stale wording since Story 2 — this is pre-existing, consistent codebase style, not a regression this story introduces.
- **Finding (Blind Hunter):** no test covers a blank/unusual `cardId` path segment despite it being explicitly unvalidated. **Verdict: false.** Every one of the three stub adapters ignores the `cardId` parameter entirely and returns a hardcoded fixture regardless of its value — there is no code path that reads or acts on `cardId`'s content at all in this story, so no input value could produce different behavior.
- **Finding (Blind Hunter):** the new `arrest.amount` wire field has no "MinorUnits"-style suffix, leaving a future API consumer unable to tell it's kopecks from the payload alone. **Verdict: false.** This matches the codebase-wide, already-established convention (Story 3's `balance`, Story 6's `sum`) of bare field names for monetary values — AD-3 documents the kopecks rule universally at the architecture level, once, rather than per-field; a suffix here would break naming consistency with its siblings, not improve it.
- **Finding (Edge Case Hunter):** `MethodArgumentNotValidException` handling would produce an empty `message` string if `BindingResult` has only global errors and zero field errors (no fallback to `getGlobalErrors()`). **Verdict: medium if real, but defer.** Pre-existing in the inherited exception-handler shape, identical in every sibling service that also has this branch; and, as established above, this service has no validation annotations at all, so the branch is unreachable here specifically. Worth fixing at the shape's origin, not per-service.
- **Finding (Verification Gap):** none reported — confirmed all 3 I/O matrix rows are covered by `RiskBlocksControllerIntegrationTest`, which boots the real DI-wired application (not mocked) and passes.

## Design Notes

Two scope decisions worth recording. First, `arrest.amount` is new — the mock has no such field, only a figure baked into `arrest.text`'s prose. AD-3's explicit, narrow binding ("`risk-blocks` (arrest sum)") is read as deliberate: only the arrest block's seizure amount gets a structured field; `sfm.text`'s own embedded `"5 000 ₽"` (describing which transaction triggered the SFM check, not a block-level monetary fact) is left alone, matching the spine's precise wording rather than over-applying AD-3 to every number that happens to appear in any block's text. Second, all three blocks are returned unconditionally for any `cardId` — `BlocksPage.jsx` dereferences `blocks.sfm.title` etc. with zero null checks today, so a `null` block for an unrecognized card would crash the existing frontend; returning the same always-populated fixture regardless of `cardId` (exactly like `offers-service`'s `StubOfferEngineAdapter`) is the only shape that doesn't risk that, and SPEC.md's "whichever blocks are active" is satisfied trivially since, in this stub-backed demo, all three always are.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build risk-blocks-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `risk-blocks-service` reports healthy (no migrations; its Postgres container stays up but unused, like `offers-service-postgres`)
  - **Result: PASS.** Full 20-container stack healthy (`docker compose up -d --wait`), including `risk-blocks-service-postgres` healthy-but-unused.
- `curl -s http://localhost:8080/risk-blocks/cards/card-4019/blocks` -- expected: `200`, matches `mockData.blocks` with `arrest.amount` added
  - **Result: PASS.** `{"sfm":{"title":"Блокировка СФМ / Списки","text":"Карта •••• 4019 · с 15.09.2026 · причина: проверка операции на 5 000 ₽"},"arrest":{"title":"Арест ФССП","text":"Пристав: ОСП по г. Москве · сумма ареста 15 000 ₽ · с 02.08.2026","amount":1500000},"af":{"title":"Блокировка АФ","text":"Карта •••• 7274 · неактивна"}}`.
- `curl -s http://localhost:8080/risk-blocks/cards/card-7274/blocks` -- expected: `200`, identical body
  - **Result: PASS.** Byte-identical to the `card-4019` response.
- `curl -i -X POST http://localhost:8080/risk-blocks/cards/card-4019/blocks` -- expected: `405`, `Allow` header present
  - **Result: PASS.** `HTTP/1.1 405`, `Allow: GET`, body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `Tests run: 3, Failures: 0, Errors: 0` -- run independently in a throwaway `eclipse-temurin:25` container (host JDK is 17, per the project's Docker-only build constraint). Stack torn down (`docker compose down`) after verification.
