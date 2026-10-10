---
title: 'Story 8 — CAP-8: Cashback'
type: 'feature'
created: '2026-10-10'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '0719daded95e189871a1ac651008186d7374716b'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `cashback-service` (Story 0) only has a health endpoint. CAP-8 needs the frontend's `getCashback(cardId)` call served with accumulated cashback, category rates, and the active promo.

**Approach:** Implement `GET /cards/{cardId}/cashback` as a genuinely data-owning slice (Liquibase schema + JPA, unlike `offers`/`risk-blocks`' stub-port facades — cashback fronts no external system) seeded from `mockData.cashback`, with one deliberate wire-format correction: `accumulated` (AD-3 explicitly binds "cashback") becomes a signed kopecks integer.

## Boundaries & Constraints

**Always:**
- Response shape `{accumulated, categories: [{id, label, percent, tone}], promo: {title, text}}` at `GET /cards/{cardId}/cashback`, mirroring `mockData.cashback` — AD-8.
- `accumulated` is a signed integer in kopecks (AD-3) — the mock's raw-ruble `1240` becomes `124000`.
- `percent` and `tone` stay the mock's exact literal strings (`"5%"`, `"green"`, etc.), unchanged — AD-3 binds money, not percentages; `percent` is a display-only rate rendered directly by `<Pill>` (same category as Story 6's `status` or Story 7's block `text`), not a value needing a kopecks-style correction.
- `categories` are ordered to match the mock's order (`cafe`, `azs`, `pharmacy`, `other`) via an explicit `sort_order` column — never relying on insertion order.
- `cardId` is accepted per AD-8's path shape but does not filter anything — like `offers`/`risk-blocks`, this is one unconditional fixture for any `cardId`. Nothing in the mock ties cashback to a specific card's identity, and `CashbackPage.jsx` dereferences `cashback.accumulated`/`cashback.categories`/`cashback.promo.title` with zero null guards, so a null or partial response for an unrecognized card would crash the existing frontend.
- This service persists its one fixture via Liquibase (AD-12) with real JPA entities — cashback genuinely owns its data (no external system to front), unlike `offers`/`risk-blocks`.
- This service's `@RestControllerAdvice` ships in the already-final shape from the start (same shape every service has carried since Story 6).

**Never:**
- No write/mutation endpoints — CAP-8's success criterion is read-only.
- No per-card seed variation — one demo-wide fixture, not one row per card; nothing in the mock or frontend ties this data to a specific card's identity (unlike Story 6's operations, which genuinely vary by card).
- No `NOT_FOUND` case for any `cardId` — always `200` with the one fixture.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Any card | `GET /cards/card-4019/cashback` | `200`, matches `mockData.cashback` with `accumulated` as `124000`, `percent`/`tone` unchanged, categories in mock order | N/A |
| Different card | `GET /cards/card-7274/cashback` | `200`, identical fixture (unconditional — not yet personalized, same as Story 2's offer / Story 7's blocks) | N/A |
| Non-GET method | `POST /cards/card-4019/cashback` | — | `405` with the spine's error envelope, `Allow` header present |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `cashback` owns accumulated/rate/promo data; AD-3 — binds "cashback" to kopecks; AD-8 — REST shape mirrors `client.js`; AD-9 — no synchronous inter-service calls; AD-12 — Liquibase migrations
- `src/api/mockData.js` export `cashback` — exact fixture; `accumulated: 1240` is what converts to `124000` kopecks
- `src/api/client.js` `getCashback(cardId)` → `GET /cards/{cardId}/cashback`
- `src/pages/CashbackPage.jsx` — unconditional `cashback.accumulated`/`cashback.categories`/`cashback.promo.title` access, no null guards — proof the fixture must always be fully present; `<Pill tone={c.tone}>{c.percent}</Pill>` — proof `percent`/`tone` are opaque display strings, not values to normalize
- `backend/card-account-service/` (Story 3) — sibling precedent for Liquibase `001-create-schema` + `002-seed` split and JPA entity/repository layering
- `backend/card-issuance-service/` (Story 5) — precedent for an explicit `sort_order` column driving deterministic seed ordering
- `backend/offers-service/`, `backend/risk-blocks-service/` (Stories 2, 7) — precedent for an unconditional fixture regardless of the path parameter, justified by the frontend's own unguarded property access
- `backend/cashback-service/` (Story 0) — existing empty scaffold; already has `adapters/out/persistence/package-info.java` (not `adapters/out/external/`), confirming this service was always meant to own data, not stub-front one
- `backend/gateway/src/main/resources/application.yml` — existing `/cashback/**` route (`StripPrefix=1`) already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `db/changelog/changes/001-create-cashback-schema.xml` -- create `cashback_summary` (singleton: id, accumulated_minor_units, promo_title, promo_text) and `cashback_categories` (id, label, percent, tone, sort_order) tables -- AD-12
- [x] `db/changelog/changes/002-seed-cashback.xml` -- seed the one summary row (`accumulated_minor_units=124000`) + the 4 `mockData.cashback.categories` rows with `sort_order` 1-4
- [x] `domain/CashbackCategory.java`, `CashbackPromo.java`, `CashbackSummary.java` -- framework-free value objects; `CashbackSummary` aggregates `accumulatedMinorUnits` + ordered categories + promo
- [x] `domain/CashbackRepository.java` -- port, `CashbackSummary getSummary(String cardId)` (parameter kept for shape consistency with sibling controllers/use-cases; not used to filter — see Boundaries)
- [x] `application/GetCashbackUseCase.java` -- orchestrates the port
- [x] `adapters/out/persistence/CashbackSummaryEntity.java`, `CashbackCategoryEntity.java`, their Spring Data interfaces, `JpaCashbackRepository.java` -- maps both tables, assembles `CashbackSummary`, orders categories by `sort_order`
- [x] `adapters/in/web/CashbackController.java` + response DTOs -- `GET /cards/{cardId}/cashback`
- [x] `adapters/in/web/CashbackExceptionHandler.java` + `ErrorResponse.java` -- final shape from the start
- [x] `src/test/.../CashbackControllerIntegrationTest.java` -- covers all 3 I/O Matrix rows against a real Testcontainers Postgres

**Acceptance Criteria:**
- Given the seed, when `GET /cards/card-4019/cashback` is called directly on the service, then `200` with `accumulated=124000`, 4 categories in mock order with unchanged `percent`/`tone`, and the promo matching the mock.
- Given `card-7274`, when `GET /cards/card-7274/cashback` is called, then `200` with the identical fixture.
- Given the stack raised via `docker compose up -d --wait`, when `GET http://localhost:8080/cashback/cards/card-4019/cashback` is called through the Gateway, then the same `200` payload arrives.
- Given a non-`GET` method, when `POST` is sent, then `405` with the spine's error envelope and an `Allow` header.

## Implementation Notes

- The two-table split (`cashback_summary` singleton + `cashback_categories`), the `sort_order` deterministic-ordering column, and the `CashbackExceptionHandler` final shape (copied verbatim from the already-final form every service has carried since Story 6) all landed exactly as planned in Code Map/Tasks — no deviation.
- `JpaCashbackRepository.getSummary` adds one defensive touch beyond the spec's literal wording: an `orElseThrow(IllegalStateException)` if the singleton `cashback_summary` row is ever missing (should never happen once Liquibase has applied its seed). Flagged and rejected by review as unreachable in normal operation — kept as-is, since falling through to the generic 500 handler is correct behavior for that genuinely exceptional state.
- Liquibase changelog comments avoided the repeated literal `--`-inside-`<!-- -->` bug (Stories 1, 3, 4, 5) — no repeat.
- Full-stack re-verification (clean `docker compose up -d --wait`, all containers) performed independently: all 3 I/O Matrix rows confirmed via `curl` through the Gateway, plus the `cashback_summary`/`cashback_categories` table schema and seeded row order inspected directly in Postgres (`\d` + a `SELECT ... ORDER BY sort_order`) to confirm the PK/column shape and category ordering match the spec exactly. Module test suite re-run independently in a throwaway `eclipse-temurin:25` container (host JDK is 17, per the project's Docker-only build constraint): `Tests run: 3, Failures: 0, Errors: 0`.
- **Self-caught fix during independent re-verification:** the implementation left all four `package-info.java` files (`domain`, `application`, `adapters/in/web`, `adapters/out/persistence`) with their stale Story-0 "Empty ... infra scaffold only" wording, unlike every prior story's equivalent package-info updates. Updated all four to describe what each package now holds.

## Spec Change Log

## Review Triage Log

- **Finding (Blind Hunter):** diff adds `8-cashback.md` with no committed `8-cashback.ru.md` sibling. **Verdict: false.** The Russian duplicate exists on disk (created alongside spec approval, before implementation started); it was untracked only because the diff was generated pre-commit. Both are staged and committed together at finalization, per the established per-story pattern.
- **Finding (Blind Hunter):** `JpaCashbackRepository.getSummary`'s `orElseThrow(IllegalStateException)` for a missing singleton row is new (no sibling service has this pattern) and untested. **Verdict: rejected.** Unreachable in normal operation — Liquibase's seed changeset either fully applies before the app starts serving traffic or the app fails to start at all; there is no steady-state path that leaves the row missing. If ever triggered (e.g. manual DB tampering), falling through to the generic 500 handler is exactly the correct behavior for a genuinely exceptional state, not a bug.
- **Finding (Blind Hunter):** `MethodArgumentNotValidException` branch in the exception handler is admittedly dead code per its own Javadoc. **Verdict: defer.** Identical, pre-existing pattern inherited verbatim from every sibling service since Story 6; same disposition as Stories 6/7's equivalent findings.
- **Finding (Blind Hunter):** all `4xx` framework errors (including this story's own `405` test) collapse into `VALIDATION_ERROR`, which is semantically misleading for a method-not-allowed response. **Verdict: defer.** Byte-for-byte identical to every sibling service's exception handler; pre-existing, not introduced here.
- **Finding (Blind Hunter):** no coverage for a genuinely unmapped route (no matching handler at all). **Verdict: defer.** Identical, pre-existing gap across all nine services' scaffolds (same as Stories 6/7's equivalent finding).
- **Finding (Blind Hunter):** no test for a blank/empty `cardId` path segment. **Verdict: false.** `JpaCashbackRepository.getSummary(String cardId)` never references its `cardId` parameter at all — the query is hardcoded to the one singleton row regardless of input — so no value of `cardId`, blank or otherwise, could produce different behavior.
- **Finding (Blind Hunter):** the spec's Verification section has no recorded `Result: PASS` annotations yet. **Verdict: false — addressed at finalization.** Same stage every prior story's spec was in immediately after implementation and before the reviewer's own independent re-verification; filled in below.
- **Finding (Blind Hunter):** no domain/use-case-layer unit tests, only one Testcontainers integration test class. **Verdict: rejected.** Matches the established, project-wide integration-test-only convention (the reviewer's own finding acknowledges this is "not unique to this diff"); the `Objects.requireNonNull` guards it would cover are unreachable with the current code (only hardcoded, mapped-from-DB non-null values ever reach these constructors).
- **Finding (Blind Hunter):** Implementation Notes/Spec Change Log/Review Triage Log sections were empty placeholders at review time. **Verdict: false — addressed at finalization.** Same stage every prior story's spec was in before its own review pass; filled in now.
- **Finding (Edge Case Hunter):** if `cashback_categories` ends up with 0-4 rows while `cashback_summary`'s row exists, the endpoint would 200 with an empty/partial `categories` array instead of failing like the summary lookup does. **Verdict: rejected.** Only reachable via a crashed mid-migration state (the seed changelog's summary and categories inserts are separate Liquibase changesets); Liquibase's own changelog tracking marks an incomplete changeset as not-yet-run, so a crash during categories seeding self-heals on the next application restart rather than persisting as a silent broken state. Adding a count-guard for this operational-only, self-correcting scenario is disproportionate complexity, and no other service in the codebase guards its own multi-changeset seeds against this identical structural risk either.
- **Finding (Verification Gap):** none reported — confirmed all 3 I/O matrix rows are covered by `CashbackControllerIntegrationTest`, run against real Testcontainers Postgres, and pass. Noted the frontend's `cashback.accumulated.toLocaleString(...)` call doesn't actually observe this story's kopecks conversion today (the mock, not the real backend, still feeds the frontend) — correctly identified as pre-existing and cross-cutting (identical to every other AD-3 money field already shipped), not introduced by this diff.

## Design Notes

Unlike `offers`/`risk-blocks` (facades with no schema), cashback genuinely owns data, so it gets a real Liquibase-backed table pair even though, in this story, the data is still a single unconditional fixture. The `cashback_summary` table models the promo as plain columns on the one singleton row rather than a separate table: unlike Story 7's arrest block (0-or-1-per-card, genuinely optional), this mock's promo is always present and singular, so a dedicated sub-table would be unneeded ceremony for a fixture that never varies. `percent`/`tone` deliberately stay untouched strings — AD-3's "never a formatted string" rule is about money specifically, and a percentage rate rendered straight into a `<Pill>` is categorically the same kind of opaque display value Story 6 already established for `status`.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build cashback-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `cashback-service` and its Postgres report healthy
  - **Result: PASS.** Full stack healthy; `cashback_summary`/`cashback_categories` schema inspected directly via `psql \d`: PK on `cashback_summary.id`, PK on `cashback_categories.id`, all columns match the spec. `SELECT * FROM cashback_categories ORDER BY sort_order` confirmed the exact mock order (cafe, azs, pharmacy, other).
- `curl -s http://localhost:8080/cashback/cards/card-4019/cashback` -- expected: `200`, matches `mockData.cashback` with `accumulated` as `124000`
  - **Result: PASS.** `accumulated:124000`, 4 categories in mock order with unchanged `percent`/`tone`, promo matching the mock exactly.
- `curl -s http://localhost:8080/cashback/cards/card-7274/cashback` -- expected: `200`, identical body
  - **Result: PASS.** Byte-identical to the `card-4019` response.
- `curl -i -X POST http://localhost:8080/cashback/cards/card-4019/cashback` -- expected: `405`, `Allow` header present
  - **Result: PASS.** `Allow: GET`, body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `Tests run: 3, Failures: 0, Errors: 0` — re-run independently in a throwaway `eclipse-temurin:25` container against real Testcontainers Postgres. Stack torn down after verification.
