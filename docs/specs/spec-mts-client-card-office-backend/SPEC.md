---
id: SPEC-mts-client-card-office-backend
companions:
  - ../../planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# mts-client-card-office backend

## Why

The React frontend prototype (`src/api/client.js`) currently mocks all backend behavior client-side — `fetchJson` just delays and echoes static data from `mockData.js`. This spec covers the real backend that replaces those mocks for the office/light-network employee-facing debit-card servicing screen: client identity, cards, operations, risk blocks, cashback, notifications, issuance, and incident/complaint case management. It is a **demo/prototype tier** backend — stub adapters stand in for real bank systems (SFM, FSSP, antifraud, offer engine, monitoring) — so the full DDD/microservices shape can be built and demonstrated now without waiting on real integration contracts with those systems.

## Capabilities

- **CAP-1** — Client identity
  - **intent:** Office employee opens a client and sees who they are and what needs attention (profile, badges, alerts/triggers, open-case summary).
  - **success:** Given a `clientId`, the identity endpoint returns profile + badges + triggers + open cases, matching the shape `client.js`'s `getClientCard` already expects.

- **CAP-2** — Top offer
  - **intent:** Employee sees one relevant personalized offer for the client.
  - **success:** Given a `clientId`, the offers endpoint returns the current top offer (title/text/moreCount) via its stub adapter to the external offer engine.

- **CAP-3** — Cards and balances
  - **intent:** Employee picks which of the client's cards to work with and sees its current state.
  - **success:** Given `clientId` + `productId`, returns the card list with balances/status; switching the active card re-fetches card-scoped data.

- **CAP-4** — Account documents
  - **intent:** Employee retrieves the documents needed to answer a client request on the spot.
  - **success:** Given a `cardId`, returns the document list (statement, balance certificate, flow certificate) matching `mockData.documents`.

- **CAP-5** — Card issuance
  - **intent:** Employee acts on a client's request to reissue or newly issue a card, and sees prior issuance history.
  - **success:** Given `clientId` + `productId`, returns the current application plus history; a reissue/new-card action creates a new application record.

- **CAP-6** — Operations history
  - **intent:** Employee investigates a client's recent card operations, including failed SBP transfers.
  - **success:** Given a `cardId`, returns operation rows plus any failed-transfer alert, matching `mockData.operations`.

- **CAP-7** — Risk blocks
  - **intent:** Employee sees why a card is restricted without contacting three separate back-office systems.
  - **success:** Given a `cardId`, returns whichever of SFM / FSSP-arrest / antifraud blocks are active, each served through its own stub port.

- **CAP-8** — Cashback
  - **intent:** Employee answers cashback questions without a separate lookup.
  - **success:** Given a `cardId`, returns accumulated total, category rates, and active promo, matching `mockData.cashback`.

- **CAP-9** — Notifications
  - **intent:** Employee adjusts how a client is notified and sees their current primary contact channel (ОМТ).
  - **success:** Given a `cardId`, returns notification toggles plus the notification method; a toggle action flips one channel's on/off state.

- **CAP-10** — Mass incidents
  - **intent:** Employee is proactively aware of systemic issues affecting this client, without polling.
  - **success:** `incident-case-service` consumes `MassIncidentOpened`/`Updated`/`Closed` Kafka events and the mass-incidents list reflects them with no REST poll to the monitoring system; the employee can attach a client to a mass incident.

- **CAP-11** — Single incidents and complaints
  - **intent:** Employee opens a formal case when self-service can't resolve the client's issue.
  - **success:** Given a `clientId`, create/list endpoints for single incidents and complaints match `mockData` shapes and `client.js`'s `createSingleIncident`/`createComplaint` contract.

## Constraints

- Every capability's service boundary, data ownership, and inter-service rule is fixed by the adopted architecture spine (AD-1..AD-11 in `ARCHITECTURE-SPINE.md`) — this spec does not restate them.
- Must preserve the existing frontend's REST resource shape (`src/api/client.js`) so the frontend needs only a `fetchJson` body swap, not a rewrite (spine AD-8).
- Demo/prototype scope only: no real integration with SFM, FSSP, antifraud, the offer engine, or the monitoring system — all sit behind stub adapters (spine AD-10). Rules out any capability requiring real bank-system credentials or protocols this iteration.

## Non-goals

- Real integration protocols for SFM, FSSP, antifraud, the offer engine, or the monitoring system.
- Production-grade authentication (OAuth2/OIDC via corporate SSO) — the demo uses a Gateway trusted-header stub (spine AD-7).
- Multi-role support (КЦ/Чат) — only the Office role exists.
- Production deployment/environment envelope (cloud provider, Kubernetes, CI/CD, environment promotion).
- Tablet-responsive frontend layout and the Granat 2.0 design-kit work — a separate frontend track, entirely outside this backend spec.

## Success signal

The `docker-compose` stack (Gateway + 9 services + Kafka + 9 PostgreSQL instances) runs locally, and the existing React frontend — with `client.js`'s `fetchJson` swapped to call the Gateway — renders every current screen (client card, all 6 product tabs, the incidents section) from live backend responses instead of `mockData.js`. A simulated `MassIncidentOpened` event published to Kafka appears in the incidents screen without a page reload or REST poll.

## Assumptions

- Assumed the frontend adopts the backend by pointing its single `fetchJson` base URL at the Gateway (AD-6), not by the backend adapting to the frontend's current per-function mock signatures beyond matching response shape.
- Assumed "Office role only" scope (per the spine's Deferred section) means this spec's success signal is scoped to the Office frontend shell only — Voice/Chat role shells are out of scope for demonstrating success, not just out of scope to build.

## Open Questions

- AD-11 fixes the Kafka envelope and topic naming but not the actual `data` payload fields for `MassIncidentOpened`/`Updated`/`Closed`. Likely mirrors `mockData.massIncidents` (theme, product, start date, affected count, `matchesClient`, status) but this is not yet confirmed as the wire contract — needed before CAP-10's stories can be written precisely.
