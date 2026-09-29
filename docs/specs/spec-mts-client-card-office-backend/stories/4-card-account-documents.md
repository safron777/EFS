---
title: 'Story 4 — CAP-4: Account documents'
type: 'feature'
created: '2026-09-29'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '709c313fbd0a2227ea3019584c4a037f2eaa72f4'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `card-account-service` (Stories 0/3) serves cards but not documents. CAP-4 (expanded during this story's planning — see spec memlog) needs the frontend's `getDocuments`, `sendDocument`, and `printDocument` calls served: listing the client's account documents, and letting the employee send or print one.

**Approach:** Add a `documents` table via a new Liquibase changeset (`003-...`, never editing `001`/`002` — Story 1's changeset-immutability lesson) seeded with the three static documents from `mockData.documents`, plus three endpoints mirroring `client.js` exactly: `GET /clients/{clientId}/cards/{cardId}/documents`, `POST /clients/{clientId}/documents/{documentId}/send`, `POST /clients/{clientId}/documents/{documentId}/print`.

## Boundaries & Constraints

**Always:**
- `GET` response is a JSON array mirroring `mockData.documents` (`id`, `title`, `subtitle`) — AD-8. The mock is unconditional (same 3 documents regardless of client/card, like Story 2's offer), so the seed is global, not per-client/card — same simplification Story 2 already made for the same reason.
- `send`/`print` return `{"ok": true}` on success, matching `client.js`'s existing stub contract exactly.
- `send`/`print` on an unknown `documentId` return `404` with `{"error":{"code":"NOT_FOUND",...}}` — a real correctness check the mock itself never had to make (it's stateless), consistent with Story 1's precedent of adding a real not-found path beyond what the mock exercises.
- New Liquibase changeset only (`003-create-documents-and-seed.xml` or split further) — `001`/`002` from Story 3 are never edited.
- This service already has the corrected `CardExceptionHandler extends ResponseEntityExceptionHandler` (Story 3, post-fix) — reuse it for the new `NOT_FOUND` mapping, do not create a second advice class.

**Never:**
- No `channel` validation/branching on `send` — `client.js` always calls it with the default `"email"` and no UI ever varies it; accept an optional `channel` field in the request body without validating its value.
- No cross-check that `documentId` belongs to the given `clientId`/`cardId` in the path — documents are global in this demo (see above), so the path's `clientId`/`cardId` are accepted at face value (AD-9 also rules out a cross-service check anyway).
- No real document generation/PDF content, no real email/print dispatch — `send`/`print` stay stub actions returning `{ok: true}`, matching the mock; Deferred alongside every other external-system stub in the spine.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| List documents | `GET /clients/client-1/cards/card-4019/documents` | `200`, array of 3 documents matching `mockData.documents` | N/A |
| Send known document | `POST /clients/client-1/documents/doc-statement/send` | `200`, `{"ok": true}` | N/A |
| Print known document | `POST /clients/client-1/documents/doc-statement/print` | `200`, `{"ok": true}` | N/A |
| Send/print unknown document | `POST /clients/client-1/documents/nope/send` | — | `404` with the error envelope |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `card-account` owns account statement documents (already named in the spine)
- `ARCHITECTURE-SPINE.md` AD-8 — REST resource shape mirrors `client.js`
- `ARCHITECTURE-SPINE.md` AD-12 — Liquibase; changesets are immutable once shipped (Story 1's lesson — see that story's Implementation Notes for the exact failure mode of editing one in place)
- `docs/specs/spec-mts-client-card-office-backend/SPEC.md` CAP-4 (expanded this story) and its `.memlog.md` entry recording the gap/resolution
- `src/api/mockData.js` `documents` export — exact fixture shape and values
- `src/api/client.js` `getDocuments(clientId, cardId)`, `sendDocument(clientId, documentId, channel)`, `printDocument(clientId, documentId)` — the three calls this story serves
- `src/pages/BalancePage.jsx` — confirms `sendDocument`/`printDocument` are live, wired UI actions (not dead code), and that the frontend never varies `channel` from its default
- `src/components/shared/DocRow.jsx` — confirms `onSend`/`onPrint` are simple fire-and-forget calls, no request body UI
- `backend/card-account-service/` (Story 3) — existing `Card`/`CardController`/`CardExceptionHandler` (the corrected shape) and `001`/`002` changesets to extend, not edit
- `backend/gateway/src/main/resources/application.yml` — existing `/card-account/**` route already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `.../domain/Document.java` -- framework-free entity (`id`, `title`, `subtitle`), `DocumentRepository` port (`List<Document> findAll()`, `boolean existsById(String id)`)
- [x] `.../application/GetDocumentsUseCase.java`, `SendDocumentUseCase.java`, `PrintDocumentUseCase.java` -- the latter two throw a domain `DocumentNotFoundException` on an unknown id
- [x] `.../adapters/in/web/DocumentController.java` + response DTOs (`DocumentResponse[]`, `{"ok":true}`) -- `GET /clients/{clientId}/cards/{cardId}/documents`, `POST /clients/{clientId}/documents/{documentId}/send`, `POST /clients/{clientId}/documents/{documentId}/print`
- [x] `CardExceptionHandler` -- add a `@ExceptionHandler(DocumentNotFoundException.class)` mapping to `404`/`NOT_FOUND`, alongside the existing framework-exception handling
- [x] `.../adapters/out/persistence/` -- JPA entity, Spring Data repository, adapter implementing the domain port
- [x] `backend/card-account-service/src/main/resources/db/changelog/changes/003-create-documents-and-seed.xml` -- new changeset (never touching `001`/`002`), referenced from `db.changelog-master.xml`, creating `documents` + seeding the three from `mockData.documents`
- [x] `backend/card-account-service/src/test/java/.../DocumentControllerIntegrationTest.java` -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` against Testcontainers Postgres, covering all four I/O-matrix rows

**Acceptance Criteria:**
- [x] Given the seeded documents, when `GET /clients/client-1/cards/card-4019/documents` is called, then the response is `200` with all 3 documents matching `mockData.documents` exactly.
- [x] Given a known `documentId`, when `POST .../documents/doc-statement/send` or `.../print` is called, then the response is `200` with `{"ok": true}`.
- [x] Given an unknown `documentId`, when `POST .../documents/nope/send` is called, then the response is `404` with the error envelope and `code: "NOT_FOUND"`.
- [x] Given the stack is brought up via `docker compose up -d --wait` (unchanged compose file), when the same requests are made through the Gateway, then the same responses come back.
- [x] Given `docker compose up -d --wait` from a clean volume, when `card-account-service` starts, then both the Story 3 `cards` schema and this story's `documents` schema apply cleanly in one run (proves the new changeset doesn't collide with or invalidate the existing ones).

## Implementation Notes

- `sort_order` was added to `DocumentEntity`/the `documents` table (not named in the story's task list, which only specifies `id`/`title`/`subtitle` on the domain `Document`) because `mockData.documents`' array order — statement, balance certificate, flow certificate — is neither alphabetical by `id` nor otherwise derivable from the row's own fields, unlike `Card`, which could reuse its meaningful `is_main` flag to reproduce main-card-first order (Story 3's own ordering solution). Without an explicit ordering column, an unordered Postgres `SELECT` gives no guarantee of matching the mock's order, and the I/O matrix's "array of 3 documents matching `mockData.documents`" reads as order included (same reading Story 3 applied to its own two-row case). `sort_order` stays persistence-only — the domain `Document` and `DocumentResponse` keep exactly the three fields the story specifies.
- `CardExceptionHandler` is extended, not replaced: `DocumentNotFoundException` gets its own `@ExceptionHandler` method (mirroring `client-identity-service`'s `ClientNotFoundException` mapping) alongside the class's existing `handleExceptionInternal` (405/415/etc.) and catch-all `Exception` handling, added in Story 3 already in its corrected, post-fix shape (Story 1/2's original bare `@ExceptionHandler(Exception.class)` bug was never reintroduced here).
- `channel` on the `send` request body is accepted via an optional `SendDocumentRequest` record (`@RequestBody(required = false)`) that is deserialized but never read by the use case, matching the story's explicit "accept without validating" instruction; a request with no body at all, and one with an unrecognized `channel` value, both succeed identically.
- Caught before it ever ran: the first draft of `003-create-documents-and-seed.xml`'s header comment used `--` inside an XML `<!-- -->` comment (documentation style already used elsewhere in this repo's Java Javadoc). XML forbids `--` inside comments; Liquibase's parser rejected the changelog outright (`"databaseChangeLog" expected as root element`). This is the exact trap Story 3's Implementation Notes already names as reproduced there despite reading Story 1's note first — reproduced a third time here, then fixed the same way: reworded the comment (commas/semicolons in place of `--`) rather than working around the parser.

## Spec Change Log

## Review Triage Log

- **`DocumentController` imports are out of alphabetical order** — `low` — Verified: `PathVariable`/`PostMapping`/`RequestBody` precede `GetMapping`, breaking the strict-alpha convention `CardController` follows. → **patch**: reorder.
- **`clientId`/`cardId` path variables are bound but never used, with nothing at the call site distinguishing "intentionally ignored" from "forgotten"** — `low` — Verified: neither method body references them; the story's own Boundaries section documents this as deliberate (documents are global), but that context isn't visible in the code itself. → **patch**: add a short comment/Javadoc noting they're intentionally unused per the global-documents design.
- **No test demonstrates GET with an unrelated/nonexistent `clientId`/`cardId` still returns all 3 documents** — `low` — Verified: every existing test uses `client-1`/`card-4019`; the documented "documents are global, no cross-check" design has no test pinning it down, so a future regression could silently narrow the result set without any test failing. → **patch**: add one test call with a different/nonexistent `clientId`/`cardId` asserting the same 3-document response.
- **`DocumentEntity` has no explicit `@Column(length=...)` matching the Liquibase schema's `varchar(64)`/`varchar(255)` bounds** — `low` — Verified: the entity is the natural place a future reader looks for the field's intended size and doesn't say. → **patch**: add the length attributes.
- **`SendDocumentUseCase`/`PrintDocumentUseCase` are near-duplicates (same one-line existence guard)** — `low`, rejected — The story's own Tasks asked for three separate use cases mirroring the three REST actions (matching Story 1's one-use-case-per-action precedent); the duplicated logic is a single guard clause, and introducing a shared abstraction for it is more machinery than the duplication justifies.
- **`DocumentController` doesn't factor a shared `@RequestMapping` prefix the way `CardController` does** — `low`, rejected — `CardController` has one endpoint under one prefix; `DocumentController`'s three endpoints span two different logical prefixes (`.../cards/{cardId}/documents` for the list, `.../documents/{documentId}/...` for send/print) that don't reduce to one clean class-level mapping — explicit paths are the clearer choice here, not an inconsistency.
- **No dedicated unit tests for the use cases in isolation** — `low`, rejected — Same conclusion Story 3 reached for its own use case: the branching (a single existence check) is already exercised end-to-end by the integration test's dedicated not-found case.
- **`ErrorResponse.details` is documented but never populated** — `low`, rejected — No caller in any story so far has needed it; adding a payload no one consumes yet is speculative ahead of actual need.
- **No uniqueness constraint on `documents.sort_order`** — `low`, rejected — The table is fully seeded by one Liquibase changeset with no insertion pathway anywhere in the system yet; guarding against a hand-edit that can't currently happen is premature.
- **I/O matrix has no row for an unrelated/nonexistent `clientId`/`cardId` on the GET** — rejected — Its fix would mean editing the frozen spec block, which the workflow's own rule forbids routing to patch; the underlying behavior is covered instead by the new test above.

## Design Notes

Documents are modeled as a global, unscoped list (no `clientId`/`cardId` foreign key) because `mockData.documents` itself is unconditional — the same three documents regardless of which client or card requested them. This mirrors Story 2's decision to treat `offers` fixture data as client-agnostic for the same reason: the mock doesn't vary, so a scoping column would model a distinction the data doesn't actually make yet.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build card-account-service` -- expected: image builds
  - **Result: PASS.** Image built cleanly.
- `docker compose -f backend/docker-compose.yml down -v && docker compose -f backend/docker-compose.yml up -d --wait` -- expected: clean-volume start, both Story 3 and Story 4 changesets apply, `card-account-service` healthy
  - **Result: PASS.** Full 20-container stack reported healthy. Logs confirm all four changesets ran in one pass: `001-create-cards-table`, `002-seed-cards-client-1`, `003-create-documents-table`, `003-seed-documents` -- "UPDATE SUMMARY ... Run: 4 ... Total change sets: 4", rows affected: 5.
- `curl -s http://localhost:8080/card-account/clients/client-1/cards/card-4019/documents` -- expected: `200`, 3 documents matching `mockData.documents`
  - **Result: PASS.** `[{"id":"doc-statement","title":"Выписка по счёту •••• 4019","subtitle":"За последние 30 дней · PDF"},{"id":"doc-balance-cert","title":"Справка об остатке денежных средств","subtitle":"На сегодняшнюю дату"},{"id":"doc-flow-cert","title":"Справка о движении средств","subtitle":"За произвольный период, для налоговой/визы"}]` -- HTTP 200, all 3 rows in `mockData.documents`' own order.
- `curl -s -X POST http://localhost:8080/card-account/clients/client-1/documents/doc-statement/send` -- expected: `200`, `{"ok":true}`
  - **Result: PASS.** `{"ok":true}`.
- `curl -s -X POST http://localhost:8080/card-account/clients/client-1/documents/doc-statement/print` -- expected: `200`, `{"ok":true}`
  - **Result: PASS.** `{"ok":true}`.
- `curl -s -w '%{http_code}' -X POST http://localhost:8080/card-account/clients/client-1/documents/nope/send` -- expected: `404`
  - **Result: PASS.** `404` with body `{"error":{"code":"NOT_FOUND","message":"Document not found: nope"}}`. Also checked the same for `.../nope/print` (not in the original command list): identical `404`/`NOT_FOUND`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `mvn -pl card-account-service -am test`: `CardControllerIntegrationTest` (4 tests, pre-existing, unaffected) + `DocumentControllerIntegrationTest` (6 tests: list-matches-mock, send/print known, send/print unknown 404, optional-channel-body accepted) -- `Tests run: 10, Failures: 0, Errors: 0` total, against a real Testcontainers Postgres with the actual Liquibase-applied schema/seed. Also re-verified `GET /clients/client-1/products/debit-mts-dengi/cards` (Story 3's endpoint) through the Gateway during the same stack run -- unaffected. Stack torn down afterward with `docker compose down -v`.
- Post-patch re-verification (independent, not just the implementation report): rebuilt, `down -v` + `up -d --wait` from a clean volume again (all 4 changesets re-applied cleanly), re-checked the same 4 curl scenarios plus the new `GET /clients/nope/cards/nope/documents` case from the added test.
  - **Result: PASS.** Identical 3-document body for both `client-1`/`card-4019` and `nope`/`nope`, confirming the "documents are global" guarantee holds regardless of path parameters; `send`/`404` behavior and Story 3's cards endpoint unaffected.
