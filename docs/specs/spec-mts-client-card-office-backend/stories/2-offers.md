---
title: 'Story 2 — CAP-2: Top offer'
type: 'feature'
created: '2026-09-24'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'd19ff3c5c5f69d4ea52de78c5e5972b882b2faaf'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `offers-service` (Story 0) only has a health endpoint. CAP-2 needs the frontend's `getOffer(clientId)` call served — but unlike Story 1's `client-identity-service`, this service's own AD-1 role is a pure facade over the bank's external offer/marketing engine (AD-10), not a data owner.

**Approach:** Implement `GET /clients/{clientId}/offers/top` as a thin hexagonal slice — domain `Offer` value object, an `OfferEnginePort`, and a stub adapter returning the fixture from `mockData.offer` — with no database schema at all. `offers-service-postgres` (provisioned uniformly in Story 0) stays unused; this is the one service this story deliberately keeps schema-free, per AD-10's own facade framing.

## Boundaries & Constraints

**Always:**
- Response mirrors `mockData.offer` exactly (`title`, `text`, `moreCount`) at `GET /clients/{clientId}/offers/top` — AD-8.
- Implemented purely as `OfferEnginePort` (domain) + a stub adapter (`adapters/out/external/offerengine/`) per AD-10 — no JPA, no Liquibase, no persistence for this capability.
- `clientId` is accepted at face value, never verified against `client-identity-service` (AD-9 — no synchronous inter-service calls).

**Never:**
- No write/mutation endpoints.
- No real external offer-engine protocol — stub fixture only (Deferred in the spine).
- No database schema — the one deliberate exception to Story 1's Liquibase pattern, because this service owns no data of its own.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Any client | `GET /clients/client-1/offers/top` | `200`, body matches `mockData.offer` exactly | N/A |
| Different client | `GET /clients/client-2/offers/top` | `200`, same fixture as above (not yet personalized — matches today's mock, which is unconditional) | N/A |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `offers` is a facade over a stub port to the external offer engine, not a data owner
- `ARCHITECTURE-SPINE.md` AD-10 — stub-port pattern (`OfferEnginePort` in domain/application, stub adapter in `adapters/out/external/`)
- `ARCHITECTURE-SPINE.md` AD-8 — REST resource shape mirrors `client.js`
- `ARCHITECTURE-SPINE.md` AD-9 — no synchronous service-to-service calls
- `src/api/mockData.js` `offer` export — exact fixture shape and values
- `src/api/client.js` `getOffer(clientId)` — `/clients/{clientId}/offers/top`
- `backend/offers-service/` (Story 0) — existing `pom.xml`, `application.yml`, hexagonal package skeleton including `adapters/out/external/offerengine/` (already created empty in Story 0)
- `backend/gateway/src/main/resources/application.yml` — existing `/offers/**` route already proxies here; no gateway change needed
- `backend/client-identity-service/` (Story 1) — sibling precedent for controller/exception-handler shape and the `NOT_FOUND`/`INTERNAL_ERROR` envelope vocabulary (not reused here since this capability has no failure path, but the code shape should look like a sibling, not a one-off)

## Tasks & Acceptance

**Execution:**
- [x] `.../domain/Offer.java` -- framework-free value object (`title`, `text`, `moreCount`)
- [x] `.../domain/OfferEnginePort.java` -- port interface, `Offer getTopOffer(String clientId)`
- [x] `.../application/GetTopOfferUseCase.java` -- orchestrates the port, no branching logic beyond the call (no not-found case exists for this capability)
- [x] `.../adapters/out/external/offerengine/StubOfferEngineAdapter.java` -- implements `OfferEnginePort`, returns the fixed `mockData.offer` fixture regardless of `clientId`
- [x] `.../adapters/in/web/OfferController.java` + a response DTO (`title`, `text`, `moreCount`) -- `GET /clients/{clientId}/offers/top`

**Acceptance Criteria:**
- [x] Given the stub adapter, when `GET /clients/client-1/offers/top` is called directly on the service, then the response is `200` and matches `mockData.offer` exactly.
- [x] Given a different `clientId`, when `GET /clients/client-2/offers/top` is called, then the same fixture is returned unchanged.
- [x] Given the stack is brought up via `docker compose up -d --wait` (unchanged compose file), when `GET http://localhost:8080/offers/clients/client-1/offers/top` is called through the Gateway, then the same `200` response comes back.

## Implementation Notes

- `offers-service/pom.xml` already carries `spring-boot-starter-data-jpa` + `postgresql` from Story 0's uniform per-service provisioning; left unchanged since removing it wasn't asked for and the datasource against the still-provisioned (but unused) `offers-service-postgres` continues to start cleanly with zero JPA entities.
- Web-layer test (`OfferControllerTest`) wires `OfferController` with plain `spring-test` `MockMvcBuilders.standaloneSetup(...)` and a Mockito mock of `GetTopOfferUseCase`, rather than `@WebMvcTest`. This was a deliberate fallback: this sandbox's local Maven cache held a truncated `spring-boot-test-autoconfigure-4.1.1.jar` (22 classes, missing the entire `web.servlet` slice including `@WebMvcTest`), and re-resolving it after `dependency:purge-local-repository` fetched the identical truncated artifact again — an environment-level cache issue, not a project dependency problem (the Docker build's own isolated Maven cache resolves the full artifact fine, confirmed by a clean `docker compose build offers-service`). The standalone-MockMvc test needs only `spring-test` + `spring-webmvc` (both already on the classpath via `spring-boot-starter-web`) and is arguably a better fit anyway: it never touches Spring Boot autoconfiguration, so it can't accidentally pull in a `DataSource`/JPA bean and mask the "no persistence for this capability" boundary.
- **Post-review patch — added `OfferExceptionHandler` + `ErrorResponse`** (`adapters/in/web/`): a minimal `@RestControllerAdvice` with a single `@ExceptionHandler(Exception.class)` → `500` + `{"error":{"code":"INTERNAL_ERROR",...}}`, matching `client-identity-service`'s envelope shape and vocabulary. Deliberately no `NOT_FOUND` mapping — no not-found case exists for this capability, so nothing needs one.
- **Post-review patch — added `OfferControllerIntegrationTest`** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`): boots the real, DI-wired app (real component scan, not a hand-built controller) and hits `GET /clients/{clientId}/offers/top` over an actual HTTP round trip via `WebTestClient`, asserting the exact fixture including the Cyrillic text. `spring.autoconfigure.exclude` turns off `DataSourceAutoConfiguration`/`DataSourceTransactionManagerAutoConfiguration`/`HibernateJpaAutoConfiguration` for this test only, so it doesn't need a real Postgres to prove the business wiring boots — consistent with this capability owning no persistence. `WebTestClient` against this servlet-stack app needs `spring-boot-starter-webflux` on the test classpath (not pulled in by `spring-boot-starter-test` alone); added as a `test`-scope dependency to `pom.xml`, exactly mirroring the comment and dependency already present in `client-identity-service/pom.xml`.
- **Post-review patch — `Offer` now rejects a negative `moreCount`** (`IllegalArgumentException`), matching the existing `Objects.requireNonNull` care already given `title`/`text`.
- **Caught during independent re-verification after the review patches, not by any reviewer:** the new `@ExceptionHandler(Exception.class)` catch-all had no visibility into Spring's own framework exceptions — a plain `POST` to this `GET`-only endpoint returned `500 INTERNAL_ERROR` instead of the correct `405`. Fixed by having `OfferExceptionHandler` extend `ResponseEntityExceptionHandler` and override `handleExceptionInternal` to reshape the body into the spine's envelope while preserving the status Spring already determined; the generic `Exception.class` handler now only catches what Spring MVC doesn't already recognize. **The identical bug existed in `client-identity-service` (Story 1, already shipped)** since that class established the exact same bare-catch-all shape this story copied — fixed there too as part of this story's review cycle, since the two are the same root cause.

## Spec Change Log

## Review Triage Log

- **No catch-all exception handler — this service has no error-envelope fallback at all** — `medium` — Verified: unlike `client-identity-service` (Story 1), `offers-service` has zero `@ExceptionHandler`/`@RestControllerAdvice`. The story's "no failure path" claim only covers the current stub; any future exception (including one propagating up from `GetTopOfferUseCase`/`OfferEnginePort`) falls through to Spring's default whitelabel body, breaking the spine's "uniform across all nine services" envelope rule. A use-case-level try/catch would be redundant once this exists, since a controller-advice catch-all intercepts regardless of where the exception originates. → **patch**: add a minimal `@RestControllerAdvice` with `@ExceptionHandler(Exception.class)` → `500` + `{"error":{"code":"INTERNAL_ERROR",...}}`, matching Story 1's vocabulary.
- **No test boots the real Spring context — DI wiring for the new endpoint is never exercised** — `medium` — Verified: both test classes bypass Spring entirely (`OfferControllerTest` hand-constructs the controller with a mocked use case; `StubOfferEngineAdapterTest` calls `new StubOfferEngineAdapter()` directly). Dropping `@Component`/`@Service` from either class would throw `NoSuchBeanDefinitionException` at real boot, yet `mvn test` would stay green. This also means no test currently exercises `GetTopOfferUseCase`'s delegation or the exact UTF-8/Cyrillic bytes on the wire — both fall out for free once a real end-to-end test exists. → **patch**: add one `@SpringBootTest(webEnvironment = RANDOM_PORT)` test mirroring `ClientControllerTest`'s shape, hitting `GET /clients/{clientId}/offers/top` through the real DI-wired app and asserting the exact fixture body.
- **`Offer.moreCount` has no validation** — `low` — Verified: `title`/`text` use `Objects.requireNonNull`, but `moreCount` accepts any `int` including negative values silently. → **patch**: add a simple guard (`if (moreCount < 0) throw new IllegalArgumentException(...)`), matching the care already given the two String fields.
- **No negative-path test for non-GET methods or a blank `clientId`** — `low`, rejected — This is Spring MVC's own default routing behavior (405 for wrong method, standard path-variable handling), not custom logic this story added; neither Story 0 nor Story 1 tests framework defaults either.
- **Implementation Notes records a sandbox-specific Maven-cache workaround** — `false` — This is exactly what the Implementation Notes section is for (Stories 0 and 1 both record similar environment-specific issues encountered during development); not a defect.
- **No forward-looking note about `clientId` validation before a real offer-engine integration** — `low`, rejected — Speculative; nothing today needs it, and the port's own Javadoc already marks the extension point.
- **`OfferResponse`/`GetTopOfferUseCase` don't guard against a null return from the port** — `low`, rejected — The only existing `OfferEnginePort` implementation (the stub) can never return null; guarding against a hypothetical future implementation violating an implicit contract is premature, consistent with similar rejections in Story 1's own triage.

## Design Notes

The through-Gateway path is `/offers/clients/{clientId}/offers/top` — "offers" appears twice: once as the Gateway's service-routing prefix (stripped before forwarding, established in Story 0), once as part of the actual resource path mirrored from `client.js` (AD-8). Stylistically repetitive but not a defect — it's the same pattern Story 1 already established (`/client-identity/clients/{id}`), and changing it would mean deviating from either AD-8's literal path-mirroring or Story 0's already-fixed Gateway routing convention, neither of which this story should revise unilaterally.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build offers-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `offers-service` reports healthy (no migration to run; its Postgres container stays up but unused)
  - **Result: PASS.** Full 20-container stack (gateway + 9 services + 9 postgres + kafka) all reported healthy, including `offers-service` and `offers-service-postgres`.
- `curl -s http://localhost:8080/offers/clients/client-1/offers/top` -- expected: `200`, body matches `mockData.offer`
  - **Result: PASS.** `{"title":"MTS CASHBACK 111 дней","text":"Повышенный кэшбэк на 111 дней при активации подписки","moreCount":1}`, HTTP 200, exact match including Cyrillic text.
- `curl -s http://localhost:8080/offers/clients/client-2/offers/top` -- expected: `200`, identical body
  - **Result: PASS.** Byte-identical response to `client-1`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `mvn -pl offers-service -am test`: `Tests run: 6, Failures: 0, Errors: 0` (`StubOfferEngineAdapterTest` x2, `OfferControllerTest` x2, `OfferControllerIntegrationTest` x2 — the last added post-review to boot the real DI-wired app; re-run after the review patches, scoped to this module only per the coordinator's instruction).
- Additional check beyond the listed commands, after the `ResponseEntityExceptionHandler` fix (Implementation Notes): `curl -X POST http://localhost:8080/offers/clients/client-1/offers/top` and the same against `client-identity-service`.
  - **Result: PASS.** Both now return `405` with `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'POST' is not supported"}}` instead of the pre-fix `500`; all other happy-path/404 behavior on both services re-verified unaffected. Stack torn down afterward with `docker compose down`.
