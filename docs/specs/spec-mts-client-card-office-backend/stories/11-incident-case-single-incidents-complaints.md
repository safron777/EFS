---
title: 'Story 11 — CAP-11: Single incidents and complaints'
type: 'feature'
created: '2026-10-12'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '717b15cd59693520eaf91b901866cf6822ccc72f'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `incident-case-service` (Stories 0, 10) has mass incidents but nothing for single incidents or complaints. CAP-11 needs `getSingleIncidents(clientId)`/`createSingleIncident(clientId, payload)` and `getComplaints(clientId)`/`createComplaint(clientId, payload)` served — the final capability in the spec.

**Approach:** Two ordinary client-scoped CRUD-lite resources (no Kafka, no stub ports — plain Liquibase + JPA, like `cashback`/`notifications`) in the same service: `GET`/`POST /clients/{clientId}/incidents/single` and `GET`/`POST /clients/{clientId}/complaints`. Creation accepts only `theme` — the frontend's own `TODO` (`README.md` "Что осознанно оставлено как TODO") states the create form's required fields are still undefined by product; inventing more would be guessing a requirement nobody has specified.

## Boundaries & Constraints

**Always:**
- `GET /clients/{clientId}/incidents/single` returns `[{id, theme, created, line, status}]`; `GET /clients/{clientId}/complaints` returns `[{id, theme, created, term, status}]` — mirroring `mockData.singleIncidents`/`mockData.complaints` (AD-8). `created` is ISO-8601, not the mock's `dd.MM.yyyy` (Consistency Conventions). An unseeded/unknown `clientId` returns `200 []`, the same collection-empty precedent every list endpoint since Story 3 established — never `404`.
- `POST /clients/{clientId}/incidents/single` and `POST /clients/{clientId}/complaints` each accept `{theme}` only (matching exactly what `createSingleIncident`/`createComplaint` currently send — `README.md` explicitly flags the rest of the create form as pending product input, not this story's to invent) and return `201 Created` with the full created row — Consistency Conventions' "additive-only" allowance beyond the baseline `{ok: true}`, the same precedent `card-issuance-service`'s `POST .../applications` (Story 5) already set for a genuine creation endpoint.
- New single incidents get `id` formatted `ЕИ-<n>` from a dedicated sequence; new complaints get `id` formatted `complaint-<n>` — both prefixes already named in `ARCHITECTURE-SPINE.md`'s Consistency Conventions table. The two sequences start comfortably above the seeded rows' own numbers, purely so a freshly created id is visually distinct from the seeded ones, not because anything parses or compares them.
- The two seeded rows in each table reproduce `mockData.singleIncidents`/`mockData.complaints` verbatim, including their literal `id` strings (`"ЕИ-118225"`, `"Жалоба №118240"`, etc.) — seed data always matches the mock exactly regardless of the generation scheme new rows get, the same deliberate divergence every prior story's seed data has drawn.
- New single incidents default to `line: "2 линия"`, `status: "В работе"` — the only values either field takes anywhere in the mock. New complaints default to `term: "—"`, `status: "На 2 линии"` — `"—"` is already a valid mock value (the second seeded complaint has it) and `"На 2 линии"` is the only non-terminal status the mock shows for this resource.
- Both services' controllers reuse `incident-case-service`'s existing `IncidentCaseExceptionHandler` (Story 10) — one advice class per service, already established.

**Never:**
- No fields beyond `theme` accepted or assumed on create — not priority, description, attachments, or a complaint/claim type distinction (the mock's two complaint rows differ only in their display `id` string, not a separate type field).
- No cross-service call to validate `clientId` against `client-identity-service` (AD-9).
- No attempt to keep `client-identity-service`'s own seeded `openCases` reference data (which already lists a `complaint-118240` entry from its own Story 1 seed) in sync with this service — AD-2 is satisfied by both sides referencing the same id scheme; nothing here writes to or reads from `client-identity-service`.
- No `"req-"`-prefixed resource — that prefix appears in the spine's Consistency Conventions table but belongs to a capability no `SPEC.md` capability ever defined; CAP-11 only ever produces `ЕИ-`/`complaint-` ids.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known client, single incidents | `GET /clients/client-1/incidents/single` | `200`, 2 rows matching the mock, `created` as ISO-8601 | N/A |
| Unknown client, single incidents | `GET /clients/nope/incidents/single` | `200`, `[]` | N/A |
| Create single incident | `POST /clients/client-1/incidents/single` body `{"theme": "Тест"}` | `201`, full row with a new `ЕИ-<n>` id, `line: "2 линия"`, `status: "В работе"`; a subsequent `GET` includes it | N/A |
| Create single incident, blank theme | `POST /clients/client-1/incidents/single` body `{"theme": ""}` | — | `400 VALIDATION_ERROR`, clean message |
| Known client, complaints | `GET /clients/client-1/complaints` | `200`, 2 rows matching the mock | N/A |
| Create complaint | `POST /clients/client-1/complaints` body `{"theme": "Тест"}` | `201`, full row with a new `complaint-<n>` id, `term: "—"`, `status: "На 2 линии"` | N/A |
| Create complaint, blank theme | `POST /clients/client-1/complaints` body `{"theme": ""}` | — | `400 VALIDATION_ERROR`, clean message |
| Non-POST method on either create path | `PUT /clients/client-1/complaints` | — | `405` with the spine's error envelope, `Allow` header present |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `incident-case` owns single incidents/complaints (plus mass incidents, Story 10); AD-2 — reference by id only; AD-8 — REST shape mirrors `client.js`; AD-9 — no synchronous inter-service calls; AD-12 — Liquibase migrations; Consistency Conventions — the `ЕИ-`/`complaint-`/`req-` id-prefix table, mutations are POST returning at minimum `{ok: true}` additive-only
- `src/api/mockData.js` exports `singleIncidents`, `complaints` — exact seed shapes; `client.openCases`' `complaint-118240` entry — independent confirmation that `complaint-` is the service's real id prefix, distinct from the display string `mockData.complaints` itself uses
- `src/api/client.js` `getSingleIncidents(clientId)`/`createSingleIncident(clientId, payload)`, `getComplaints(clientId)`/`createComplaint(clientId, payload)`
- `src/pages/incidents/SingleIncidentsPage.jsx`, `ComplaintsPage.jsx` — both call create with only `{theme: "..."}`, both carry a `TODO` citing `README.md` for the undefined required-fields question
- `README.md` "Что осознанно оставлено как TODO" — explicit product-level gap on create-form required fields; this story deliberately does not fill that gap
- `backend/card-issuance-service/` (Story 5) — precedent for a genuine creation endpoint: `201 Created` + full created row, `@NotBlank`/`@Size` request validation, a dedicated id-generation scheme distinct from seeded legacy rows
- `backend/incident-case-service/` (Story 10) — existing `IncidentCaseExceptionHandler`/`ErrorResponse` (reuse as-is, no changes) and the service's own Liquibase changelog set (append new changesets, never edit `001-create-incident-case-schema.xml`)
- `backend/gateway/src/main/resources/application.yml` — existing `/incident-case/**` route already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `db/changelog/changes/002-create-single-incidents-and-complaints-schema.xml` -- create `single_incidents` (id, client_id, theme, created_date, line, status) and `complaints` (id, client_id, theme, created_date, term, status) tables, plus the two id-generating sequences -- AD-12; a new changeset, never editing `001-create-incident-case-schema.xml`
- [x] `db/changelog/changes/003-seed-single-incidents-and-complaints.xml` -- seed the mock's 2 `singleIncidents` + 2 `complaints` rows for `client-1`, verbatim ids
- [x] `domain/SingleIncident.java`, `Complaint.java` -- framework-free value objects
- [x] `domain/SingleIncidentRepository.java`, `ComplaintRepository.java` -- ports: `List<X> findByClientId(String clientId)`; `X create(String clientId, String theme)`
- [x] `application/GetSingleIncidentsUseCase.java`, `CreateSingleIncidentUseCase.java`, `GetComplaintsUseCase.java`, `CreateComplaintUseCase.java`
- [x] `adapters/out/persistence/SingleIncidentEntity.java`, `ComplaintEntity.java`, their Spring Data interfaces, `JpaSingleIncidentRepository.java`, `JpaComplaintRepository.java` -- id generation via each table's own Postgres sequence, formatted `ЕИ-<n>`/`complaint-<n>`
- [x] `adapters/in/web/SingleIncidentController.java` + `CreateSingleIncidentRequest.java` (`@NotBlank @Size(max=255) theme`) + response DTO -- `GET`/`POST /clients/{clientId}/incidents/single`
- [x] `adapters/in/web/ComplaintController.java` + `CreateComplaintRequest.java` (same validation shape) + response DTO -- `GET`/`POST /clients/{clientId}/complaints`
- [x] `src/test/.../SingleIncidentControllerIntegrationTest.java`, `ComplaintControllerIntegrationTest.java` -- cover all 8 I/O Matrix rows against a real Testcontainers Postgres

**Acceptance Criteria:**
- Given the seed, when `GET /clients/client-1/incidents/single` is called, then `200` with 2 rows matching the mock exactly, dates as ISO-8601.
- Given a theme, when `POST /clients/client-1/incidents/single` is called, then `201` with a new `ЕИ-<n>` id, `line: "2 линия"`, `status: "В работе"`, and a subsequent `GET` for the same client includes it.
- Given the seed, when `GET /clients/client-1/complaints` is called, then `200` with 2 rows matching the mock exactly.
- Given a theme, when `POST /clients/client-1/complaints` is called, then `201` with a new `complaint-<n>` id, `term: "—"`, `status: "На 2 линии"`.
- Given a blank `theme` on either create endpoint, then `400` with a clean validation message.
- Given an unknown `clientId`, when either `GET` is called, then `200` with `[]`, never `404`.

## Implementation Notes

- The two-resource split (`SingleIncidentController`/`ComplaintController`, each its own domain/application/persistence set reusing `incident-case-service`'s existing `IncidentCaseExceptionHandler` from Story 10 unchanged), the `ЕИ-<n>`/`complaint-<n>` sequence-backed id generation, and the seed-verbatim-vs-new-generated-id split all landed exactly as planned in Code Map/Tasks — no deviation. No new Maven dependencies or package changes were needed beyond what Story 10 already added to this service.
- Both sequences (`single_incident_id_seq`, `complaint_id_seq`) start at `200000`, well above the seeded rows' own numbers, purely for visual distinctiveness — confirmed live: a freshly created single incident got id `ЕИ-200000`, a freshly created complaint got `complaint-200000`.
- Liquibase changesets were appended as new files (`002-create-single-incidents-and-complaints-schema.xml`, `003-seed-single-incidents-and-complaints.xml`); `001-create-incident-case-schema.xml` (Story 10) was never touched.
- All four package-info.java files in this service (`domain`, `application`, `adapters/in/web`, `adapters/out/persistence`) still only mentioned Story 10's classes after implementation; self-caught during independent review and updated to also name this story's additions.
- **Post-review patch:** two reviewers independently converged on the same gap — `knownClientReturns200WithTwoSeededRowsMatchingMockExactly` in both test classes only checked that both seeded rows existed somewhere in the list, never that they were actually ordered newest-first as the repository's own Javadoc promises; a dropped/reversed `OrderBy` would have shipped undetected. Added one ordering assertion to each test (the ordering behavior itself was already correct, independently confirmed live before the patch). Also added one test per resource for the untested `@Size(max=255)` boundary on `theme`.
- Full-stack re-verification (clean `docker compose up -d --wait`, all containers) performed independently, both before and after the patch: all 8 I/O Matrix rows confirmed via `curl` through the Gateway — including that a created row is visible in a subsequent `GET`, that an oversized clientId's edge case was traced (pre-existing, identical gap across every mutation endpoint since Story 9, not unique to this story), and that `Allow: POST, GET` appears on a `405`. Module test suite re-run independently in a throwaway `eclipse-temurin:25` container (host JDK is 17, per the project's Docker-only build constraint): all 4 test classes in the service pass, 27/27 total (9 Kafka listener + 6 mass-incident REST + 6 complaints + 6 single-incidents, up from 25 pre-patch).

## Spec Change Log

## Review Triage Log

- **Finding (Blind Hunter + Edge Case Hunter, convergent):** `knownClientReturns200WithTwoSeededRowsMatchingMockExactly` in both test classes located each expected row via `.stream().filter(...).findFirst()`, so neither test actually verified the newest-first ordering `findByClientIdOrderByCreatedDateDescIdAsc`'s own Javadoc promises — a dropped or reversed `OrderBy` would still pass. **Verdict: patch (applied).** Added one assertion to each test pinning element 0 to the more-recently-created seeded row and element 1 to the other. The ordering behavior itself was already correct (independently confirmed live before this patch — a freshly created row appeared first in a subsequent `GET`); only the test assertion was missing.
- **Finding (Blind Hunter):** neither test class exercised the `@Size(max = 255)` boundary on `theme` — only the blank-theme case was covered. **Verdict: patch (applied).** Added one test per class posting a 256-character `theme` and asserting `400 VALIDATION_ERROR`.
- **Finding (Blind Hunter):** diff adds the story spec with no `.ru.md` sibling visible in its own path scope. **Verdict: false.** The Russian duplicate exists on disk, created alongside spec approval; it was outside the path scope the reviewer's diff was generated with, not a real gap.
- **Finding (Blind Hunter):** no unit test for the domain value objects' `Objects.requireNonNull` invariants. **Verdict: rejected.** Matches the established, project-wide integration-test-only convention already accepted identically in Stories 7-10.
- **Finding (Blind Hunter):** `clientId` accepted with no blank/whitespace validation — a path segment like a single space would persist an effectively-unreachable row. **Verdict: rejected.** No service in this codebase validates path-variable `clientId`/`cardId`/`incidentId` format; the frontend always supplies a real, context-derived value, and manually crafted malformed requests are a threat class this backend has never defended against anywhere.
- **Finding (Blind Hunter):** cross-client isolation of `findByClientId` is only tested against `client-1` and the zero-row `nope`, never two distinct real clients with data. **Verdict: rejected.** A standard parameterized `WHERE client_id = ?` derived query, trivially correct by construction, no shared/cached state that could leak across clients — coverage-completism without a plausible failure mode.
- **Finding (Blind Hunter):** `ComplaintEntity`/`SingleIncidentEntity` don't override `equals`/`hashCode`. **Verdict: rejected.** Consistent with every other JPA entity in this codebase; none implement them either.
- **Finding (Blind Hunter):** no test for a fully-missing `theme` key (`{}`) distinct from `{"theme":""}`. **Verdict: rejected/false.** Jakarta's `@NotBlank` explicitly treats a missing (null) field the same as blank — not a distinct code path from what's already tested.
- **Finding (Blind Hunter):** `created` uses server `LocalDate.now()` with no explicit zone, and the test asserts exact equality against a second `LocalDate.now()` call, a latent midnight-boundary flake. **Verdict: rejected.** Same pattern already used identically elsewhere in this codebase for server-stamped creation dates (e.g. Story 5); exceedingly low real-world probability, not unique to this story.
- **Finding (Blind Hunter):** the two new Liquibase changesets have no `<rollback>` elements. **Verdict: defer.** Pre-existing across literally every changeset in every service's changelog since Story 1; not introduced or worsened here.
- **Finding (Edge Case Hunter):** `clientId` longer than the `varchar(64)` column on `POST` would hit an uncaught `DataIntegrityViolationException` surfacing as `500` instead of `400`. **Verdict: defer.** Identical, pre-existing gap for every path-variable id (`notificationId`, `incidentId`, etc.) across every mutation endpoint since Story 9; not unique to or worsened by this story.
- **Finding (Verification Gap):** none reported beyond the convergent ordering gap above (already patched) — confirmed all 8 I/O matrix rows are covered by `SingleIncidentControllerIntegrationTest`/`ComplaintControllerIntegrationTest`, run against real Testcontainers Postgres, and pass.

## Design Notes

Two scope decisions worth recording. First, create only ever accepts `theme`: `README.md` itself says the real required fields (priority, description, attachments) await product input, and inventing them here would mean this story quietly deciding a product question nobody has answered — the one thing explicitly out of bounds. Second, new rows get a different id format than the two seeded legacy rows (`ЕИ-<n>`/`complaint-<n>` vs. the mock's `"ЕИ-118225"`/`"Жалоба №118240"`): this isn't an inconsistency so much as the same split every prior story with legacy seed data has drawn — seed data mirrors the mock's history exactly, new data follows the architecture's own forward-looking convention, confirmed independently here by `client-identity-service`'s own `openCases` sample already using `complaint-118240` rather than `"Жалоба №118240"` for the same real-world entity.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build incident-case-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `incident-case-service` and its Postgres report healthy
  - **Result: PASS.** Full stack (gateway + all nine services + Kafka + Postgres instances) healthy.
- `curl -s http://localhost:8080/incident-case/clients/client-1/incidents/single` -- expected: `200`, 2 rows matching the mock
  - **Result: PASS.** Both seeded rows (`ЕИ-118225`, `ЕИ-117904`) returned with `created` as ISO-8601 (`2026-09-02`/`2026-08-14`) and `theme`/`line`/`status` matching the mock exactly.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"theme":"Тест"}' http://localhost:8080/incident-case/clients/client-1/incidents/single` -- expected: `201`, new `ЕИ-<n>` row
  - **Result: PASS.** `201`, `{"id":"ЕИ-200000","theme":"Тестовый инцидент","created":"2026-10-12","line":"2 линия","status":"В работе"}`; a subsequent `GET` showed it first (newest-first order).
- `curl -s http://localhost:8080/incident-case/clients/client-1/complaints` -- expected: `200`, 2 rows matching the mock
  - **Result: PASS.** Both seeded rows (`Жалоба №118240`, `Претензия №117580`) returned matching the mock exactly, including `term: "—"` for the second row.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"theme":"Тест"}' http://localhost:8080/incident-case/clients/client-1/complaints` -- expected: `201`, new `complaint-<n>` row
  - **Result: PASS.** `201`, `{"id":"complaint-200000","theme":"Тестовая жалоба","created":"2026-10-12","term":"—","status":"На 2 линии"}`.
- `curl -i -X POST -H 'Content-Type: application/json' -d '{"theme":""}' http://localhost:8080/incident-case/clients/client-1/complaints` -- expected: `400`
  - **Result: PASS.** `400`, `{"error":{"code":"VALIDATION_ERROR","message":"theme: must not be blank"}}`.
- `curl -i -X PUT http://localhost:8080/incident-case/clients/client-1/complaints` -- expected: `405`, `Allow` header present
  - **Result: PASS.** `405`, `Allow: POST, GET`, body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'PUT' is not supported"}}`.
- Module test suite -- expected: all pass
  - **Result: PASS.** 27/27 across all 4 test classes in the service, re-confirmed independently post-patch. Stack torn down after manual verification.
