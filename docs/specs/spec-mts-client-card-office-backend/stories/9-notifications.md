---
title: 'Story 9 — CAP-9: Notifications'
type: 'feature'
created: '2026-10-11'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '61803af44a766a41ac1a02580180f50c311077d0'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `notifications-service` (Story 0) only has a health endpoint. CAP-9 needs the frontend's `getNotifications(cardId)` call served with notification-channel toggles plus the primary contact method, and `toggleNotification(cardId, notificationId, on)` wired to a real mutation — the first genuine update-in-place state change in this backend (every prior mutation, Stories 4/5, only ever created or sent something; nothing before has flipped an existing row's field).

**Approach:** A Liquibase-backed, data-owning slice (like `cashback-service`, Story 8) with `GET /cards/{cardId}/notifications` returning a single demo-wide fixture, and `POST /cards/{cardId}/notifications/{notificationId}` persisting the new `on` state, reusing `card-account-service`'s `DocumentNotFoundException`/404 shape for an unknown channel id.

## Boundaries & Constraints

**Always:**
- `GET /cards/{cardId}/notifications` returns `{items: [{id, title, subtitle, on, action}], method: {title, current}}`, mirroring what `client.js`'s `getNotifications` assembles from `mockData.notifications` + `mockData.notificationMethod` — AD-8.
- `POST /cards/{cardId}/notifications/{notificationId}` with body `{on: boolean}` flips that channel's state and returns `{ok: true}` (Consistency Conventions' baseline mutation response), mirroring `toggleNotification(cardId, notificationId, on)`.
- Notifications and the method are one demo-wide fixture, not scoped per `cardId` — like `offers`/`risk-blocks`/`cashback` (Stories 2/7/8), `cardId` is accepted per AD-8's path shape but never filters anything. Nothing in the mock or `NotificationsPage.jsx`'s unconditional `data.method.title`/`data.items.map` access ties this data to a specific card's identity. Toggling is a genuinely shared, persisted state change — the first true update-in-place mutation in this backend.
- An unknown `notificationId` in the `POST` path returns `404 NOT_FOUND` with the spine's envelope — unlike the always-`200` collection endpoints (Stories 2/6/7/8), this targets one identified resource, the same `NOT_FOUND` precedent Story 1's `/clients/{id}` and Story 4's document send/print already established.
- `action` stays the mock's literal display string (e.g. `"Отключить"`, `"Настроить"`) unchanged — `NotificationsPage.jsx`'s secondary button only displays it with no `onClick` handler at all; it carries no backend meaning.
- The persisted boolean column is named `enabled`, not `on` — `on` is a SQL keyword Postgres reserves for `JOIN ... ON`; avoiding it as a column name avoids having to quote it everywhere. The wire field and domain accessor stay `on`/`isOn()` to mirror the mock exactly.
- This service's exception handler ships in the already-final shape from the start, extended with one `NotFoundException` mapping that mirrors `card-account-service`'s `DocumentNotFoundException` precedent exactly (one advice class, no second one).

**Never:**
- No method-change mutation — CAP-9's success criterion covers only the toggle action; `NotificationsPage.jsx`'s "Изменить" button for the notification method has no `onClick` handler at all, so changing the method is out of scope.
- No per-card seed variation for notifications or the method — one demo-wide fixture (same reasoning as Story 8's cashback), not one row set per card.
- Toggling never validates `cardId` against `card-account-service` (AD-9) and never uses it to scope which channel gets updated — only `notificationId` identifies the target row.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Any card, read | `GET /cards/card-4019/notifications` | `200`, `items`+`method` matching the mock, demo-wide fixture | N/A |
| Different card, read | `GET /cards/card-7274/notifications` | `200`, identical fixture | N/A |
| Toggle known channel | `POST /cards/card-4019/notifications/sms-ops` body `{"on": false}` | `200 {"ok": true}`; a subsequent `GET` reflects `on: false` for `sms-ops` | N/A |
| Toggle unknown channel | `POST /cards/card-4019/notifications/nope` body `{"on": false}` | — | `404 NOT_FOUND` |
| Toggle missing body field | `POST /cards/card-4019/notifications/sms-ops` body `{}` | — | `400 VALIDATION_ERROR`, clean message |
| Non-POST method on toggle path | `PUT /cards/card-4019/notifications/sms-ops` | — | `405` with the spine's error envelope, `Allow` header present |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` AD-1 — `notifications` owns channel-toggle/method data; AD-2 — reference by `cardId` only; AD-8 — REST shape mirrors `client.js`; AD-9 — no synchronous inter-service calls; AD-12 — Liquibase migrations; Consistency Conventions — mutations are POST, baseline `{ ok: true }` response
- `src/api/mockData.js` exports `notifications` (3 channels, all `on: true` today) and `notificationMethod` — exact fixtures
- `src/api/client.js` `getNotifications(cardId)` → `GET /cards/{cardId}/notifications`; `toggleNotification(cardId, notificationId, on)` → `POST /cards/{cardId}/notifications/{notificationId}`
- `src/pages/NotificationsPage.jsx` — every item renders a `<Toggle>` wired to `handleToggle` regardless of its `action` label; the secondary `<Button>{item.action}</Button>` and the method's "Изменить" button both have no `onClick` — proof `action` is decorative and method-change is out of scope
- `backend/card-account-service/` (Story 4) — `DocumentNotFoundException` + `CardExceptionHandler`'s `@ExceptionHandler(DocumentNotFoundException.class)` mapping — the exact shape to mirror for an unknown `notificationId`; `OkResponse` — the exact `{ok: true}` shape to mirror
- `backend/cashback-service/` (Story 8) — sibling precedent for a data-owning (not stub-port) service: Liquibase `001-create-schema` + `002-seed` split, JPA entity/repository layering, and the "one demo-wide fixture, cardId unused" pattern
- `backend/notifications-service/` (Story 0) — existing empty scaffold; already has `adapters/out/persistence/package-info.java`, confirming this service owns data
- `backend/gateway/src/main/resources/application.yml` — existing `/notifications/**` route (`StripPrefix=1`) already proxies here; no gateway change needed

## Tasks & Acceptance

**Execution:**
- [x] `db/changelog/changes/001-create-notifications-schema.xml` -- create `notification_channels` (id, title, subtitle, enabled, action, sort_order) and `notification_method` (id PK singleton, title, current) tables -- AD-12
- [x] `db/changelog/changes/002-seed-notifications.xml` -- seed the 3 `mockData.notifications` channels (`sort_order` 1-3, `enabled=true`) + the 1 `notificationMethod` row
- [x] `domain/NotificationChannel.java`, `NotificationMethod.java`, `NotificationsSummary.java` -- framework-free value objects; `NotificationsSummary` aggregates ordered channels + method
- [x] `domain/NotificationChannelNotFoundException.java` -- mirrors `DocumentNotFoundException`'s shape exactly
- [x] `domain/NotificationsRepository.java` -- port: `NotificationsSummary getSummary(String cardId)`; `void setChannelState(String notificationId, boolean on)` throwing `NotificationChannelNotFoundException` for an unknown id
- [x] `application/GetNotificationsUseCase.java`, `ToggleNotificationUseCase.java` -- orchestrate the port
- [x] `adapters/out/persistence/NotificationChannelEntity.java`, `NotificationMethodEntity.java`, their Spring Data interfaces, `JpaNotificationsRepository.java` -- maps both tables (boolean column `enabled`), orders channels by `sort_order`
- [x] `adapters/in/web/NotificationsController.java` + `ToggleNotificationRequest.java` (`@NotNull Boolean on`) + response DTOs -- `GET /cards/{cardId}/notifications`, `POST /cards/{cardId}/notifications/{notificationId}`
- [x] `adapters/in/web/NotificationsExceptionHandler.java` + `ErrorResponse.java` + `OkResponse.java` -- final shape from the start, plus the `NotificationChannelNotFoundException` → `404` mapping
- [x] `src/test/.../NotificationsControllerIntegrationTest.java` -- covers all 6 I/O Matrix rows against a real Testcontainers Postgres, including that a toggle actually persists (verified via a follow-up `GET`)

**Acceptance Criteria:**
- Given the seed, when `GET /cards/card-4019/notifications` is called directly on the service, then `200` with 3 channels (all `enabled`) and the method, matching the mock.
- Given a known `notificationId`, when `POST /cards/card-4019/notifications/sms-ops` with `{"on": false}` is called, then `200 {"ok": true}`, and a subsequent `GET` shows `sms-ops`'s `on` as `false`.
- Given an unknown `notificationId`, when `POST /cards/card-4019/notifications/nope` is called, then `404` with the spine's error envelope.
- Given a missing `on` field, when `POST /cards/card-4019/notifications/sms-ops` with `{}` is called, then `400` with a clean validation message.
- Given a non-`POST` method on the toggle path, when `PUT` is sent, then `405` with the spine's error envelope and an `Allow` header.

## Implementation Notes

- The two-table split (`notification_channels` + `notification_method` singleton), the `sort_order` deterministic-ordering column, and `NotificationsExceptionHandler`'s final shape (copied verbatim from the already-final form every service has carried since Story 6, extended with exactly the one `NotificationChannelNotFoundException` → `404` mapping) all landed exactly as planned in Code Map/Tasks -- no deviation.
- `notification_method.current` is named `current` literally, matching the task list's own wording -- unlike `on`, `CURRENT` is a non-reserved keyword in PostgreSQL (usable as an unquoted identifier), so no renaming footgun applies there.
- `spring-boot-starter-validation` was added as an explicit Maven dependency (not pulled in transitively by `spring-boot-starter-web`) -- same precedent as `card-issuance-service`'s `@NotBlank` usage, this story's own first use of `@NotNull`.
- Three Liquibase XML comments initially used a literal double-hyphen (`--`) inside `<!-- -->`, which XML disallows mid-comment (caught by a real Liquibase parse failure during verification, not just a lint guess); reworded to avoid it, following Story 8's own note about this exact recurring pitfall (Stories 1, 3, 4, 5).
- The integration test chains `POST` then `GET` in one test method (per Design Notes), and -- because the Spring test context (and its one Testcontainers Postgres) is reused across all six test methods in the class -- uses `@TestMethodOrder`/`@Order` to run the toggle-and-verify test last, and restores the channel to its original `on: true` state at the end so the mutation leaves no observable side effect for any later test run.
- The `toggleWithMissingOnFieldReturns400...` test asserts `$.error.message` is non-empty rather than an exact string, matching `IssuanceControllerIntegrationTest`'s own precedent -- Jakarta Bean Validation's default message interpolator is locale-dependent (confirmed locally: it renders in Russian on a host JVM with a Russian default locale, in English inside the service's own Docker container), so asserting a literal message would be environment-fragile.
- Full-stack re-verification (clean `docker compose up -d --wait`, all containers) performed: all 6 I/O Matrix rows confirmed via `curl` through the Gateway (port 8080), including the toggle-then-GET persistence check and restoring `sms-ops` back to `on: true` afterward. Module test suite re-run in a throwaway `eclipse-temurin:25`-equivalent JDK 25 toolchain (host JDK is 17, per the project's Docker-only build constraint) against a real Testcontainers Postgres: `Tests run: 6, Failures: 0, Errors: 0`. Stack torn down after verification.

## Spec Change Log

## Review Triage Log

- **Finding (Blind Hunter):** diff adds `9-notifications.md` with no committed `9-notifications.ru.md` sibling. **Verdict: false.** The Russian duplicate exists on disk, created alongside spec approval; it was outside the path scope the reviewer's diff was generated with, not a real gap.
- **Finding (Blind Hunter + Edge Case Hunter):** every Spring-detected 4xx (404 unmapped route, 405, 415, malformed body) collapses into `VALIDATION_ERROR`, and this story's own `405` test locks that in. **Verdict: defer.** Byte-for-byte identical to every sibling service's exception handler since Story 6; pre-existing, not introduced here.
- **Finding (Blind Hunter + Edge Case Hunter):** `JpaNotificationsRepository.setChannelState` does a plain find-then-save with no optimistic locking (`@Version`), so two concurrent toggles for the same channel could race and silently lose a write. **Verdict: rejected.** Real in principle, but this is a single-operator demo/prototype backend (architecture spine's explicit, already-logged scope-tier decision: "not production-grade") — one bank-office employee clicking one toggle at a time, not concurrent multi-user access. The fix (a `@Version` field plus a new conflict-response path) is more than a trivial correction and adds public surface (a new error shape) for a scenario the architecture already ruled out of scope.
- **Finding (Blind Hunter):** no domain/use-case-layer unit tests, only one Testcontainers integration test class. **Verdict: rejected.** Matches the established, project-wide integration-test-only convention already accepted identically in Stories 7/8.
- **Finding (Blind Hunter):** `NotificationChannel`/`NotificationMethod`/`NotificationsSummary` implement no `equals()`/`hashCode()`/`toString()`. **Verdict: rejected.** Consistent with every other domain value object in this codebase (`Operation`, `CashbackCategory`, `RiskBlocks`, etc. — none implement them either); not a gap unique to this story.
- **Finding (Blind Hunter + Edge Case Hunter):** a missing `notification_method` singleton row throws a plain `IllegalStateException` with no dedicated `@ExceptionHandler`, so it falls through to the generic 500 handler and loses its diagnostic detail. **Verdict: rejected.** Same category as Story 8's equivalent finding — unreachable in normal operation, since Liquibase's seed changeset either fully applies before the app serves traffic or the app fails to start; falling through to a generic 500 is correct behavior for that genuinely exceptional state.
- **Finding (Blind Hunter):** the toggle's `{"ok": true}` response never echoes the resulting channel state, forcing callers to issue a second `GET`. **Verdict: rejected.** The frozen Boundaries & Constraints and the spine's Consistency Conventions explicitly fix the baseline mutation response as `{ ok: true }` (matching Stories 4/5's identical `OkResponse` shape) — this is an already-settled architectural decision, not a gap to fill.
- **Finding (Blind Hunter):** the toggle-and-verify test relies on `@Order(6)` plus a manual restore step at the end, with no `@AfterEach`/`@AfterAll` safety net if an earlier assertion in that same method fails first. **Verdict: rejected.** It's the last-ordered test in the class, so a mid-method failure can't bleed into any other test in the same run; a fresh Testcontainers Postgres is provisioned for the next independent test run regardless, and the failure itself is already visibly reported.
- **Finding (Blind Hunter):** the new `pom.xml` dependency coordinates (`spring-boot-liquibase`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`) are justified only by inline comments, not demonstrated to resolve. **Verdict: false.** Directly refuted by independent re-verification: the service was rebuilt from a clean Docker image and its full module test suite was re-run successfully in a separate verification pass.
- **Finding (Blind Hunter + Edge Case Hunter):** no test covers a malformed/empty `cardId` at either endpoint. **Verdict: false.** `cardId` is never read by any query or filter in this service — `JpaNotificationsRepository.getSummary(cardId)` never references its parameter, and `setChannelState` doesn't accept one at all — so no value of `cardId` could produce different behavior.
- **Finding (Edge Case Hunter):** `notification_channels` could end up empty while `notification_method`'s row exists, yielding `200` with `items: []` instead of a clear error. **Verdict: rejected.** Same reasoning as Story 8's equivalent finding — only reachable via a crashed mid-migration state, which self-heals on the next application restart via Liquibase's own changelog tracking; not a steady-state risk.
- **Finding (Verification Gap):** the clean-validation-message branch is only asserted as non-empty, not confirmed to actually be the clean `"field: reason"` form rather than the raw exception dump the branch exists to prevent — a regression here would ship silently green. **Verdict: defer**, per the reviewer's own disposition: identical, pre-existing weak-assertion pattern already in `card-issuance-service`'s equivalent test; tightening it here alone wouldn't close the gap repo-wide, so it's a cross-service fix, not a one-off patch to this story.

## Design Notes

The toggle is this backend's first true update-in-place mutation — every prior mutation (Story 4's document send/print, Story 5's application creation) only ever triggered a side effect or inserted a new row; this one flips an existing row's field and the next `GET` must observe it. The integration test therefore chains a `POST` then a `GET` in one test method rather than asserting the `POST` response alone. The `enabled` column naming (vs. the wire's `on`) is a small but deliberate choice: `ON` is part of SQL's `JOIN` syntax and, while technically usable as a quoted identifier in Postgres, naming around it avoids a footgun for any future query against this table.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build notifications-service` -- expected: image builds
  - **Result: PASS.**
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: `notifications-service` and its Postgres report healthy
  - **Result: PASS.** Full stack (gateway + all nine services + Kafka + Postgres instances) healthy.
- `curl -s http://localhost:8080/notifications/cards/card-4019/notifications` -- expected: `200`, matches the mock fixture
  - **Result: PASS.** `items` returned all 3 channels (`sms-ops`, `sms-plan`, `push`) with `on:true` and the mock's exact `title`/`subtitle`/`action` strings; `method` matched `{"title":"Способ уведомлений (ОМТ)","current":"Email · v***@mail.ru"}` exactly.
- `curl -s -X POST -H 'Content-Type: application/json' -d '{"on":false}' http://localhost:8080/notifications/cards/card-4019/notifications/sms-ops` -- expected: `200 {"ok":true}`
  - **Result: PASS.**
- `curl -s http://localhost:8080/notifications/cards/card-4019/notifications` (repeat) -- expected: `sms-ops`'s `on` is now `false`
  - **Result: PASS.** `sms-ops` showed `on:false`; `sms-plan`/`push` unaffected. Restored to `on:true` afterward to leave the demo fixture in its original state.
- `curl -i -X POST http://localhost:8080/notifications/cards/card-4019/notifications/nope -d '{"on":false}' -H 'Content-Type: application/json'` -- expected: `404`
  - **Result: PASS.** `404`, body `{"error":{"code":"NOT_FOUND","message":"Notification channel not found: nope"}}`.
- `curl -i -X PUT http://localhost:8080/notifications/cards/card-4019/notifications/sms-ops` -- expected: `405`, `Allow` header present
  - **Result: PASS.** `405`, `Allow: POST`, body `{"error":{"code":"VALIDATION_ERROR","message":"Request method 'PUT' is not supported"}}`.
- Module test suite -- expected: all pass
  - **Result: PASS.** `Tests run: 6, Failures: 0, Errors: 0` -- covers all 6 I/O Matrix rows against a real Testcontainers Postgres, including the toggle-persists-and-is-observed-by-GET case (with state restored at the end of that test).
