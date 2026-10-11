---
title: 'Story 10 — CAP-10: Incident-case mass incidents (Kafka)'
type: 'feature'
created: '2026-10-11'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '665b96e9ae9553b1faa337274e4be86d8d618a12'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `incident-case-service` (Story 0) only has a health endpoint. CAP-10 needs mass incidents to reach the frontend's `GET /incidents/mass` (`getMassIncidents()`) purely from Kafka events — never a REST poll to the monitoring system (AD-5) — plus `attachClientToIncident(clientId, incidentId)` wired to a real mutation.

**Approach:** Three `@KafkaListener`s (one per topic: `incident-case.mass-incident.{opened,updated,closed}`, AD-11) write to this service's own read-model table, idempotently upserting by incident `id`; an event whose `version` the consumer doesn't support is republished to a dead-letter topic rather than applied or silently dropped. `GET /incidents/mass` and `POST /incidents/mass/{incidentId}/attach` read/write that same table — no synchronous call to any other service (AD-9), and no REST call to the monitoring system (AD-5).

## Boundaries & Constraints

**Always:**
- `GET /incidents/mass` returns a JSON array `[{id, theme, product, start, status, affected, matchesClient}]`, mirroring `getMassIncidents()` — AD-8. `start` is ISO-8601 (Consistency Conventions), not the mock's `dd.MM.yyyy`.
- Three Kafka topics, one per event verb (AD-11): `incident-case.mass-incident.opened`, `.updated`, `.closed`. Each envelope is `{eventId, eventType, occurredAt, version, data}` (AD-11). `data` fields, resolved during this story's planning (closing SPEC.md's former open question 2):
  - `opened`: `{id, theme, product, start, status, affected}` — everything fixed at creation.
  - `updated`: `{id, status, affected}` — only the two fields that can legitimately change after opening.
  - `closed`: `{id, status}`.
- Consuming `opened` for an `id` already on file, or `updated`/`closed` for an unknown `id`, is handled without crashing the consumer: `opened` upserts (idempotent re-delivery), `updated`/`closed` for an unknown `id` is logged and skipped (Kafka's at-least-once delivery and topic ordering make both reachable in normal operation, not just a theoretical edge).
- An event whose `version` isn't the one supported value is never applied — its full original envelope is republished verbatim to `incident-case.mass-incident.dead-letter` (AD-11: "route to a dead-letter topic rather than fail silently"), and the consumer acknowledges it (no retry loop, no crash).
- Unknown fields inside `data` are ignored (AD-11 forward-compatibility) — deserialize `data` leniently, never fail on an extra field.
- `matchesClient` is always `false` on every response. It is explicitly not part of any Kafka event's `data` (the monitoring system, AD-11's producer, has no notion of a specific bank-office client) and nothing in this story computes it (a real correlation would need a synchronous call into `operations-service`, forbidden by AD-9) — a decision made explicitly during this story's planning, not an oversight. The frontend's "Похоже на совпадение" suggestion banner (`matched = incidents.find(i => i.matchesClient...)`) simply never fires until a future story adds real correlation (Deferred).
- `POST /incidents/mass/{incidentId}/attach` with body `{clientId}` records that client against that incident and returns `{ok: true}` (Consistency Conventions baseline mutation response), mirroring `attachClientToIncident(clientId, incidentId)`. Attaching the same `clientId` to the same incident twice is idempotent (no duplicate row, no error) — the UI lets an employee click the button more than once.
- Attaching to an unknown `incidentId` returns `404 NOT_FOUND` — the same single-resource `NotFoundException` precedent Stories 1/4/9 already established (mirror `NotificationChannelNotFoundException`'s shape exactly).
- Kafka topics are declared explicitly via `NewTopic` beans (`KafkaAdmin`), not left to broker auto-create — self-documenting and correct even if auto-create is ever disabled.
- This service's exception handler ships in the already-final shape from the start, extended with the one `NotFoundException` mapping above.

**Never:**
- No synchronous HTTP call out of this service to any other service, ever (AD-9) — not to validate `clientId`, not to compute `matchesClient`.
- No REST polling of the monitoring system — the read model is populated exclusively by the three Kafka listeners (AD-5).
- No business-logic retry/requeue of a bad-version event back onto its own topic — it goes to the dead-letter topic once and is acknowledged, full stop.
- `affected` stays the mock's free-text approximate string (e.g. `"~3 200 клиентов"`) unchanged — it's a description, not a number, so AD-3 doesn't apply to it.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| No incidents consumed yet | `GET /incidents/mass` | `200`, `[]` | N/A |
| `opened` event consumed | Publish a valid `opened` envelope | A subsequent `GET /incidents/mass` includes that incident, `matchesClient: false` | N/A |
| `updated` event consumed for a known incident | Publish `updated` changing `status`/`affected` | `GET` reflects the new `status`/`affected`; `theme`/`product`/`start` unchanged | N/A |
| `closed` event consumed | Publish `closed` | `GET` reflects the new `status` | N/A |
| Unsupported-version event | Publish an envelope with `version: 99` | Not applied to the read model; the full envelope appears on `incident-case.mass-incident.dead-letter`; consumer keeps running | N/A |
| Attach known incident | `POST /incidents/mass/{id}/attach` body `{"clientId": "client-1"}` | `200 {"ok": true}`, idempotent on repeat | N/A |
| Attach unknown incident | `POST /incidents/mass/nope/attach` body `{"clientId": "client-1"}` | — | `404 NOT_FOUND` |
| Non-POST method on attach path | `PUT /incidents/mass/{id}/attach` | — | `405` with the spine's error envelope, `Allow` header present |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `incident-case` owns mass-incident state; AD-2 — reference by id only; AD-5 — Kafka push, never polling; AD-8 — REST shape mirrors `client.js`; AD-9 — no synchronous inter-service calls, ever; AD-11 — topic naming/envelope (now extended with the `data` shapes resolved this story) and dead-letter handling; AD-12 — Liquibase migrations
- `src/api/mockData.js` export `massIncidents` — exact row shape (minus `matchesClient`'s real meaning, resolved above)
- `src/api/client.js` `getMassIncidents()` → `GET /incidents/mass` (no `clientId` — a global, unscoped list); `attachClientToIncident(clientId, incidentId)` → `POST /incidents/mass/{incidentId}/attach`
- `src/pages/incidents/MassIncidentsPage.jsx` — renders the "Привязать клиента" button regardless of `matchesClient` (only its color varies) and a conditional "Похоже на совпадение" banner keyed off `matchesClient` — confirms attach is always clickable and the banner is cosmetic, not something this story must make fire correctly
- `backend/card-account-service/`'s `DocumentNotFoundException` + `CardExceptionHandler` mapping (Story 4), reused identically in Story 9's `NotificationChannelNotFoundException` — the exact shape for the attach-to-unknown-incident `404`
- `backend/card-issuance-service/` (Story 5) — precedent for `@NotBlank`/clean validation-message shape on a request body
- `backend/incident-case-service/` (Story 0) — existing scaffold; already has `adapters/in/messaging/package-info.java` documenting this exact Kafka-consumer plan, and `adapters/out/persistence/package-info.java` confirming this service owns data
- `backend/docker-compose.yml` lines ~358-396 — Kafka (KRaft, single broker) already stood up with a comment anticipating this exact story; no compose changes needed
- `backend/gateway/src/main/resources/application.yml` — existing `/incident-case/**` route (`StripPrefix=1`) already proxies here; no gateway change needed
- `backend/pom.xml` — `testcontainers-bom` (2.0.5) already imported ahead of Boot's own pin, so a `testcontainers-kafka` test dependency resolves the matching version automatically; Kafka client version is pinned transitively by whatever `spring-kafka` version the Spring Boot 4.1.1 BOM manages — verify on the web that this pairs cleanly with the broker's `apache/kafka:4.3.1` image before assuming compatibility

## Tasks & Acceptance

**Execution:**
- [x] `db/changelog/changes/001-create-incident-case-schema.xml` -- create `mass_incidents` (id, theme, product, start_date, status, affected) and `mass_incident_attachments` (incident_id, client_id, attached_at; unique on `(incident_id, client_id)`) tables -- AD-12
- [x] `domain/MassIncident.java` -- framework-free value object (no `matchesClient` field — it's a response-layer constant `false`, not domain state)
- [x] `domain/MassIncidentNotFoundException.java` -- mirrors `NotificationChannelNotFoundException`'s shape exactly
- [x] `domain/MassIncidentRepository.java` -- port: `List<MassIncident> findAll()`; `void upsertOpened(...)`; `void applyUpdate(id, status, affected)` (no-op + log if unknown id); `void applyClose(id, status)` (same); `void attachClient(incidentId, clientId)` throwing `MassIncidentNotFoundException` for an unknown id, idempotent on a repeat attach
- [x] `application/GetMassIncidentsUseCase.java`, `AttachClientUseCase.java` -- orchestrate the port for the REST side
- [x] `application/ApplyMassIncidentEventUseCase.java` -- the one use case all three listeners call, dispatching to the right repository method per event type
- [x] `adapters/out/persistence/MassIncidentEntity.java`, `MassIncidentAttachmentEntity.java`, their Spring Data interfaces, `JpaMassIncidentRepository.java` -- maps both tables, upsert via `save` on a found-or-new entity
- [x] `adapters/in/messaging/MassIncidentEventEnvelope.java` -- generic envelope record `{eventId, eventType, occurredAt, version, data: JsonNode}`, shared by all three listeners
- [x] `adapters/in/messaging/MassIncidentOpenedData.java`, `MassIncidentUpdatedData.java`, `MassIncidentClosedData.java` -- typed `data` shapes per Boundaries & Constraints, lenient on unknown fields
- [x] `adapters/in/messaging/MassIncidentEventListener.java` -- three `@KafkaListener` methods (one per topic); each validates `version`, republishes to the dead-letter topic on mismatch, otherwise converts `data` and calls `ApplyMassIncidentEventUseCase`
- [x] `adapters/in/messaging/KafkaTopicsConfig.java` -- `NewTopic` beans for the three event topics + the dead-letter topic
- [x] `adapters/in/web/MassIncidentController.java` + `AttachClientRequest.java` (`@NotBlank String clientId`) + response DTOs -- `GET /incidents/mass`, `POST /incidents/mass/{incidentId}/attach`
- [x] `adapters/in/web/IncidentCaseExceptionHandler.java` + `ErrorResponse.java` + `OkResponse.java` -- final shape from the start, plus the `MassIncidentNotFoundException` → `404` mapping
- [x] `src/test/.../MassIncidentEventListenerIntegrationTest.java` -- a real embedded/Testcontainers Kafka broker + Testcontainers Postgres: publishes `opened`/`updated`/`closed`/bad-version envelopes, asserts the read model (via the repository or `GET`) and the dead-letter topic's contents
- [x] `src/test/.../MassIncidentControllerIntegrationTest.java` -- covers the REST-only I/O Matrix rows (empty list, attach known/unknown, wrong method)

**Acceptance Criteria:**
- Given no events consumed, when `GET /incidents/mass` is called, then `200 []`.
- Given an `opened` event published to `incident-case.mass-incident.opened`, when `GET /incidents/mass` is called afterward, then the incident appears with `matchesClient: false` and the event's fields, `start` as ISO-8601.
- Given that incident, when an `updated` event changes its `status`/`affected`, then a subsequent `GET` reflects exactly those two fields changed and nothing else.
- Given an event with an unsupported `version`, when it's published, then it never appears in the read model and its full envelope appears on `incident-case.mass-incident.dead-letter`.
- Given a known incident, when `POST /incidents/mass/{id}/attach` is called twice with the same `clientId`, then both calls return `200 {"ok": true}` and only one attachment row exists.
- Given an unknown incident, when the same `POST` is called, then `404` with the spine's error envelope.

## Implementation Notes

- Spring Boot 4 splits Kafka support the same way it already splits Liquibase: the dependency to declare is `spring-boot-starter-kafka` (which pulls in `spring-kafka` plus the new `spring-boot-kafka` autoconfiguration module), not a bare `spring-kafka`/`spring-boot-autoconfigure` pairing. Confirmed by inspecting the resolved jar directly (`spring-boot-kafka-4.1.1.jar` contains `KafkaAutoConfiguration` and, under `org/springframework/boot/kafka/testcontainers/`, `ApacheKafkaContainerConnectionDetailsFactory` -- the class that makes `@ServiceConnection` on a `org.testcontainers.kafka.KafkaContainer` field work out of the box). Test-side, `spring-boot-starter-kafka-test` is the matching starter.
- `kafka-clients` 4.2.1 (pulled in transitively by `spring-kafka` 4.1.1, both pinned by the Spring Boot 4.1.1 BOM) against the broker's `apache/kafka:4.3.1` image: verified to interoperate in practice, not just assumed -- the full `MassIncidentEventListenerIntegrationTest` suite (a real Testcontainers `apache/kafka:4.3.1` broker) and the full-stack manual verification below both produce, consume, and admin-create topics against that exact image without any protocol/version error.
- This codebase's Jackson usage is Jackson 3, not Jackson 2: annotations stay under `com.fasterxml.jackson.annotation.*` (unchanged), but `ObjectMapper`/`JsonNode` moved to `tools.jackson.databind.*` (confirmed by a real compilation failure before correcting the import, then cross-checked against `card-account-service`'s existing `CardControllerIntegrationTest`, which already imports `tools.jackson.databind.JsonNode`/`ObjectMapper`). `MassIncidentEventEnvelope`, `MassIncidentOpenedData`/`UpdatedData`/`ClosedData`, and `MassIncidentEventListener` all use the `tools.jackson.databind` package accordingly.
- The Testcontainers Kafka module's real class is `org.testcontainers.kafka.KafkaContainer` (artifact `testcontainers-kafka`, managed by the `testcontainers-bom` 2.0.5 already imported in the parent POM) -- verified by extracting the resolved jar and running `javap` on it rather than guessing: it exposes a `KafkaContainer(String dockerImageName)` constructor, which both test classes use with `"apache/kafka:4.3.1"` to match the broker image pinned in `docker-compose.yml` exactly.
- Dead-letter routing forwards the listener's raw `String` Kafka payload unchanged, never a re-serialized Java object -- `@KafkaListener` methods here are declared `(String payload)`, not `(MassIncidentEventEnvelope envelope)`, specifically so "republished verbatim" (Boundaries & Constraints) is a byte-for-byte guarantee, not an approximation that could reorder fields. Verified directly: the dead-letter integration test asserts the consumed record's value `isEqualTo` the exact literal string that was published.
- No listener method ever lets an exception escape (a private `withValidEnvelope` helper swallows and logs both JSON-parse failures and apply failures), so "the consumer acknowledges it, no retry loop, no crash" (Boundaries & Constraints) falls out of Spring Kafka's default container behavior for free -- no custom `CommonErrorHandler` or manual `Acknowledgment` was needed.
- `mass_incident_attachments` uses a composite primary key (`incident_id`, `client_id`) via JPA's `@IdClass`, the same precedent as `client-identity-service`'s `client_badges`/`client_open_case_ids` link tables, rather than a surrogate id. This both satisfies the task's literal "unique on `(incident_id, client_id)`" and makes a repeat attach a plain `save`-as-merge of the same row (no duplicate-row check needed to stay idempotent) -- confirmed by inspecting the table directly after two `POST .../attach` calls: exactly one row.
- `MassIncidentEventListenerIntegrationTest` and `MassIncidentControllerIntegrationTest` both start a Testcontainers Kafka broker via `@ServiceConnection`, even though the controller test never publishes anything itself -- without one, `KafkaAdmin`'s startup topic-creation would otherwise spend real time retrying against an unreachable `kafka:9092` host (non-fatal, since `spring.kafka.admin.fail-fast` defaults to `false`, but needlessly slow); the controller test does publish one setup `opened` event through the real Kafka path to get its one known incident, rather than seeding the database directly, to stay honest about CAP-10's actual boundary (AD-5).
- Three new Liquibase XML comments initially used a literal double-hyphen (`--`) inside `<!-- -->`, which XML disallows mid-comment -- caught by a real Liquibase parse failure (`"databaseChangeLog" expected as root element`) during verification, not a lint guess, and reworded to avoid it. The exact same pitfall recurred while editing `pom.xml`'s own XML comments (a plain Maven POM parse failure this time) before either file compiled/built. This is the same recurring pitfall Story 9's Implementation Notes already flagged (itself citing Stories 1/3/4/5) -- now recurred in two more files in this story alone.
- `version` in `MassIncidentEventEnvelope` is a primitive `int`, not `Integer`: a missing/non-numeric `version` field deserializes to `0` rather than throwing, which the listener's `!= SUPPORTED_VERSION` check already treats as unsupported and routes to dead-letter -- one fewer null-check, same correct outcome.
- Package-info Javadocs for all five `incident-case-service` packages (`domain`, `application`, `adapters/in/{web,messaging}`, `adapters/out/persistence`) were updated from their Story 0 "empty scaffold" wording to name what this story populated, following the exact precedent `notifications-service`'s five package-infos already set in Story 9.
- **Post-review patch:** all three independent review layers converged on the same gap — a valid-version event whose `data` failed to convert/apply was logged and silently dropped, never dead-lettered, unlike the other two failure paths. `withValidEnvelope`'s apply-failure catch now calls `sendToDeadLetter` too, so all three failure modes dead-letter consistently; `sendToDeadLetter` itself now logs if the dead-letter publish fails; the unparseable-payload branch now logs the exception object, not just its message. A new test (`dataConversionFailureIsRoutedToDeadLetterAndConsumerKeepsRunning`) confirms the fix and that the consumer survives and keeps consuming afterward. Independently re-verified: rebuilt the image, reproduced the scenario live via `kafka-console-producer`/`-consumer` against a clean stack, and re-ran the full module suite (15/15 pass).

## Spec Change Log

## Review Triage Log

- **Finding (Blind Hunter + Edge Case Hunter + Verification Gap, convergent):** a valid-`version` event whose `data` fails to convert/apply only hit the generic catch in `withValidEnvelope` and was silently logged-and-dropped — unlike the other two failure paths (unparseable JSON, unsupported version), it was never dead-lettered, and no test exercised it. **Verdict: patch (applied).** All three independent reviewers converged on this exact gap. Fixed: the apply-failure catch now also calls `sendToDeadLetter(payload)`; a new test (`dataConversionFailureIsRoutedToDeadLetterAndConsumerKeepsRunning`) publishes a `version: 1` `opened` envelope with an unparseable `start` date and confirms it never applies, lands verbatim on the dead-letter topic, and that the consumer keeps working afterward (a valid event published right after is still picked up). Re-verified independently (9/9 listener tests pass; manually reproduced via `kafka-console-producer`/`-consumer` against a clean stack).
- **Finding (Edge Case Hunter + Blind Hunter, convergent):** `sendToDeadLetter`'s `kafkaTemplate.send(...)` discarded its returned future, so a failure to publish to the dead-letter topic itself was silent. **Verdict: patch (applied).** Bundled into the same fix: `.whenComplete(...)` now logs if the dead-letter publish itself fails.
- **Finding (Blind Hunter):** the unparseable-payload catch block logged only `exception.getMessage()`, with no exception object (no stack trace), unlike the other two branches. **Verdict: patch (applied).** Bundled into the same fix: now passes `exception` as the final log argument, consistent with the other branches.
- **Finding (Edge Case Hunter):** an event whose `eventType` doesn't match the topic it was published to (e.g. a `MassIncidentClosed`-shaped payload sent to the `opened` topic) isn't explicitly validated. **Verdict: subsumed by the patch above, no separate fix needed.** Tracing the actual failure mode: the mismatched shape would fail `objectMapper.treeToValue` binding against the listener's own typed `data` record (missing/wrong fields), which is exactly the apply-failure path just fixed — it is not "silently applied as the wrong type" as initially framed; it now dead-letters like any other conversion failure.
- **Finding (Blind Hunter):** the I/O & Edge-Case Matrix omits rows for "attach with blank `clientId` → 400," "re-delivered `opened` for an already-known id," and "`updated`/`closed` for an unknown id," even though all three are implemented, described in Boundaries & Constraints prose, and covered by dedicated tests. **Verdict: rejected.** The fix would mean editing the frozen Matrix post-approval for a documentation-completeness gap, not a functional one — the behavior itself is correctly implemented and tested in both cases; the Matrix's representative-rows convention (established since Story 2) was never meant to be an exhaustive enumeration of every case Boundaries & Constraints prose already describes.
- **Finding (Blind Hunter):** diff adds `10-incident-case-mass-incidents.md` with no `.ru.md` sibling visible in the diff's own path scope. **Verdict: false.** The Russian duplicate exists on disk, created alongside spec approval; it was outside the path scope the reviewer's diff was generated with, not a real gap.
- **Finding (Blind Hunter):** the non-4xx branch of `handleExceptionInternal` echoes `exception.getMessage()` while the separate `handleUnexpected` catch-all always returns a generic message — two 5xx paths disagreeing on whether to leak internal text. **Verdict: defer.** Byte-for-byte identical to every sibling service's exception handler since Story 6; pre-existing, not introduced here.
- **Finding (Blind Hunter):** `MethodArgumentNotValidException` would produce an empty message if `BindingResult` has only global errors and zero field errors. **Verdict: defer.** Pre-existing in the inherited exception-handler shape, identical in every sibling service that has this branch; `AttachClientRequest` has no class-level constraint, so it's unreachable in this service specifically, same disposition as Stories 7/9's equivalent finding.
- **Finding (Blind Hunter):** mapping every `statusCode.is4xxClientError()` to `VALIDATION_ERROR` means an unrelated 404 (e.g. a mistyped route) comes back with that code instead of `NOT_FOUND`. **Verdict: defer.** Same pre-existing, identical-everywhere pattern already deferred in every prior story.
- **Finding (Blind Hunter):** a repeat attach overwrites `attached_at` with the latest timestamp rather than preserving the original. **Verdict: rejected.** The spec only requires idempotency (no duplicate row, no error), not timestamp preservation; "last attached at" is a defensible, arguably more operationally useful semantic, and no requirement was violated.
- **Finding (Blind Hunter):** the consumer group id `"incident-case-service"` is a literal duplicated across three `@KafkaListener` annotations and `application.yml`, with no shared constant. **Verdict: rejected.** Cosmetic maintenance concern; a mismatch would be caught immediately by any functional test (consumers would simply split groups, not crash or corrupt data) — not a latent silent risk worth a patch round.
- **Finding (Edge Case Hunter):** concurrent `updated`/`closed` listeners (different topics, different consumer threads) racing on the same incident id could lose an update; concurrent duplicate attach requests could race past the existence check into a constraint violation. **Verdict: rejected, both.** Same disposition as Story 9's equivalent concurrency finding: real in principle, but disproportionate for this explicitly single-operator, demo/prototype-scope backend (already logged as "not production-grade" at the architecture level); the fixes (optimistic locking + retry, or `SELECT ... FOR UPDATE`) add meaningful complexity for a scenario this story's scope doesn't anticipate.

## Design Notes

This is the first Kafka-consuming service in the backend, so it carries more new machinery than any prior story: a generic envelope wrapping per-topic typed `data`, explicit `NewTopic` declarations, idempotent upsert semantics for at-least-once delivery, and dead-letter routing for a forward-incompatible `version`. The dead-letter topic is a single shared `incident-case.mass-incident.dead-letter` (not one per source topic) because AD-11 says "a dead-letter topic" (singular) and a human investigating bad events benefits from one place to look regardless of which of the three topics produced them. `matchesClient` reading `false` unconditionally is this story's one user-visible behavior change versus the original static mock (resolved explicitly with the user during planning, not inferred) — the suggestion banner that depended on it simply goes quiet until a future, explicitly out-of-scope correlation story exists.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build incident-case-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `incident-case-service`, its Postgres, and `kafka` report healthy
  - **Result: PASS.** Full stack (gateway + all nine services + Kafka + Postgres instances) healthy.
- `curl -s http://localhost:8080/incident-case/incidents/mass` -- expected: `200 []` on a clean stack
  - **Result: PASS.**
- Publish an `opened` envelope to `incident-case.mass-incident.opened` (e.g. via `kafka-console-producer` in the `kafka` container) -- expected: a subsequent `curl` on the same endpoint shows the new incident
  - **Result: PASS.** Published via `kafka-console-producer.sh` for `MI-MANUAL-1`; the subsequent `GET` returned it with `theme`/`product`/`start`/`status`/`affected` exactly matching the published `data`, `start` as ISO-8601 (`"2026-09-12"`), and `matchesClient:false`.
- Publish an `updated` then a `closed` envelope for that same `id` -- expected: each `curl` afterward reflects the change
  - **Result: PASS.** `updated` changed `status`→`"Устраняется"` and `affected`→`"~4 000 клиентов"` while `theme`/`product`/`start` stayed unchanged; `closed` then changed `status`→`"Устранён"` with `affected` left exactly as `updated` had set it.
- Publish an envelope with an unsupported `version` -- expected: it never appears via `curl`; `kafka-console-consumer` against `incident-case.mass-incident.dead-letter` shows it
  - **Result: PASS.** Published a `version:99` envelope for `MI-MANUAL-BAD`; it never appeared in any subsequent `GET`, and `kafka-console-consumer.sh --from-beginning` against `incident-case.mass-incident.dead-letter` returned that exact envelope byte-for-byte.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"clientId":"client-1"}' http://localhost:8080/incident-case/incidents/mass/{id}/attach` (twice) -- expected: both `200 {"ok":true}`
  - **Result: PASS.** Both calls returned `{"ok":true}`; a direct `psql` query against `mass_incident_attachments` afterward showed exactly one row for `(MI-MANUAL-1, client-1)`.
- `curl -i -X POST ... /incidents/mass/nope/attach` -- expected: `404`
  - **Result: PASS.** `404`, body `{"error":{"code":"NOT_FOUND","message":"Mass incident not found: nope"}}`.
- `curl -i -X PUT .../incidents/mass/{id}/attach` -- expected: `405`, `Allow` header present
  - **Result: PASS.** `405`, `Allow: POST`, body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'PUT' is not supported"}}`.
- Post-patch, independently re-verified: publish a valid-version, malformed-`data` event (`"start":"not-a-date"`) -- expected: never applied, lands verbatim on the dead-letter topic, consumer keeps running
  - **Result: PASS.** `GET` never showed the malformed incident; `kafka-console-consumer` against the dead-letter topic showed it byte-for-byte; a valid event published immediately afterward was picked up normally.
- Module test suite -- expected: all pass
  - **Result: PASS.** `MassIncidentEventListenerIntegrationTest`: `Tests run: 9, Failures: 0, Errors: 0` (opened/re-delivered-opened/updated/updated-unknown/closed/closed-unknown/bad-version-never-applied/bad-version-on-dead-letter-verbatim/data-conversion-failure-dead-lettered, against a real Testcontainers `apache/kafka:4.3.1` broker + Postgres — the 9th test added by the post-review patch). `MassIncidentControllerIntegrationTest`: `Tests run: 6, Failures: 0, Errors: 0` (empty list, known-incident shape, attach-twice-idempotent, attach-unknown-404, blank-clientId-400, wrong-method-405). 15/15 total, re-confirmed independently post-patch. Stack torn down after manual verification.
