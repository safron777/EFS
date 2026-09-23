---
title: 'Story 0 — Инфраструктурный каркас: монорепо, Gateway, docker-compose, Kafka'
type: 'chore'
created: '2026-09-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'c7d8a9ddd28437f31a89c18ca518b596c901899f'
context:
  - '{project-root}/docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/specs/spec-mts-client-card-office-backend/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The backend for `mts-client-card-office` doesn't exist yet — there is no repo structure, no Gateway, no way to run the 9 planned microservices locally. Every other story (CAP-1..11) needs somewhere to land.

**Approach:** Stand up the Maven multi-module skeleton at `backend/` inside the existing `EFS` repo — one module per bounded context (per AD-1) plus a Gateway module — with a hexagonal package layout, a `docker-compose.yml` wiring Gateway + 9 services + Kafka + 9 Postgres instances, and just enough per-service code (a booting Spring Boot app with a health endpoint) to prove the stack runs end-to-end. No business capability (CAP-1..11) is implemented here.

## Boundaries & Constraints

**Always:**
- Nine service modules exactly as named in AD-1 (`client-identity`, `offers`, `card-account`, `card-issuance`, `operations`, `risk-blocks`, `cashback`, `notifications`, `incident-case`) plus `gateway` — no more, no fewer.
- Each service module follows the hexagonal package layout from the spine's Design Paradigm section (`domain/`, `application/`, `adapters/in/web/`, `adapters/out/persistence/`; `adapters/out/external/` for `risk-blocks` and `offers`; `adapters/in/messaging/` for `incident-case`), even though most are empty in this story.
- Gateway is the only container that publishes a host port (AD-6); the 9 services and Postgres/Kafka are reachable only on the docker-compose network.
- Each service gets its own PostgreSQL container/database — no shared schema (AD-1).
- Maven Wrapper (`mvnw`) checked in, pinned to Maven 3.9.16, so the build doesn't depend on whatever Maven version is on a given machine.
- Builds and runs happen exclusively inside Docker (multi-stage `Dockerfile`, `eclipse-temurin:25` for build and runtime); host JDK version (currently 17) does not need to match and is not upgraded as part of this story — decided to unblock Story 0 without a local toolchain change.

**Never:**
- No business endpoints beyond a health check — CAP-1..11 are separate stories.
- No CI/CD or cloud deployment config (Deferred in the spine).
- No real SFM/FSSP/antifraud/offer-engine/monitoring integration — those stub adapters arrive with their owning capability's story.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Cold start | `docker compose up` on empty volumes | All 9 services + gateway report healthy within a reasonable startup window | A service whose Postgres isn't ready yet retries via `depends_on` health condition rather than crash-looping |
| Gateway routing | HTTP GET to a Gateway path mapped to one service (e.g. `/client-identity/actuator/health`) | Gateway forwards to the correct service container and returns its response | Target service down → Gateway returns 502/503, not a hang |

</frozen-after-approval>

## Code Map

- `ARCHITECTURE-SPINE.md` § Structural Seed — exact monorepo tree and per-service package layout to replicate under `backend/`
- `ARCHITECTURE-SPINE.md` AD-1 — the 9 service names/owners (do not rename or reorder)
- `ARCHITECTURE-SPINE.md` AD-6 — Gateway-only ingress (drives the docker-compose port-publishing rule above)
- `ARCHITECTURE-SPINE.md` AD-9 + dependency diagram — no service calls another service synchronously; only Gateway→service and (for `incident-case`) Kafka→service edges exist in the compose network
- `ARCHITECTURE-SPINE.md` § Stack — exact pinned versions: Java 25 LTS, Spring Boot 4.1.1, PostgreSQL 18.6, Kafka 4.3.1, Maven 3.9.16, Testcontainers 2.0.5
- `SPEC.md` CAP-1..11 — the capabilities each service module will eventually host; module names/boundaries must already fit these without renaming later
- `package.json` (repo root) — frontend dev server runs on port 5173; Gateway's local port must not collide (use 8080)

## Tasks & Acceptance

**Execution:**
- [x] `backend/pom.xml` -- parent aggregator POM (`packaging: pom`), `<modules>` listing all 10 (gateway + 9 services), `<dependencyManagement>` importing the Spring Boot 4.1.1 BOM, Java release set to 25 -- single place pinning the stack for every module
- [x] `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties` -- Maven Wrapper pinned to 3.9.16 -- for host-side editing convenience only (see host-JDK note below); not what the build gate runs against
- [x] `backend/docker-compose.yml` -- gateway + 9 services + Kafka (KRaft mode, no ZooKeeper — Kafka 4.x dropped it) + 9 named Postgres containers/volumes, healthchecks on every container, `depends_on: condition: service_healthy` -- only `gateway` maps a host port
- [x] `backend/gateway/pom.xml` + minimal Spring Cloud Gateway app + `application.yml` route table (one route per service, by docker-compose service DNS name) + `Dockerfile` (multi-stage, `eclipse-temurin:25`) -- proves AD-6's single-entry-point end to end
- [x] `backend/<service>/pom.xml` + minimal booting Spring Boot app (Spring Boot Actuator health endpoint) + `Dockerfile` (multi-stage, `eclipse-temurin:25`) + `application.yml` (own datasource pointing at that service's Postgres container) + the hexagonal package skeleton -- once per each of the 9 services
- [x] `backend/README.md` -- how to bring the stack up (`docker compose up --build`), verify health, and a note that the host JDK is 17 while the pinned/build-time JDK is 25 (Docker-only builds by design, see Boundaries & Constraints) -- onboarding for the next 11 stories

**Acceptance Criteria:**
- [x] Given a clean checkout, when running `docker compose -f backend/docker-compose.yml build`, then all 10 images (gateway + 9 services) build successfully under `eclipse-temurin:25` — this is the build gate, not a host-side `mvn`/`mvnw` invocation, since the host JDK (17) cannot target `--release 25`.
- [x] Given `docker compose -f backend/docker-compose.yml up -d --wait`, when the wait completes, then every one of the 9 services + gateway reports healthy.
- [x] Given the stack is up, when issuing `GET /<service-path>/actuator/health` against the Gateway for each of the 9 services, then each returns `200 {"status":"UP"}` from the correct backing container.
- [x] Given `docker compose -f backend/docker-compose.yml ps`, then only the `gateway` service shows a published host port.

## Implementation Notes

- **Module directory names carry a `-service` suffix** (`client-identity-service`, ...) to match the Structural Seed's literal tree in the spine exactly; the Boundaries line "modules exactly as named in AD-1" is read as fixing the *set and identity* of the nine bounded contexts (no renaming/reordering/adding), not the literal folder string. Gateway route path prefixes and docker-compose DNS names both use the short AD-1 names/`-service`-suffixed names respectively — see `backend/README.md`'s routing table.
- **Spring Cloud Gateway version pin:** no Spring Cloud release train yet targets Spring Boot 4.1.x (the newest, `2025.1.3`, targets Boot `4.0.8` — verified against Maven Central). Rather than import the whole `spring-cloud-dependencies` BOM (which would drag every `spring-boot-starter-*` back to 4.0.8 via first-import-wins), `gateway/pom.xml` pins only `spring-cloud-starter-gateway-server-webflux:5.0.3` directly, leaving the parent's Spring Boot 4.1.1 BOM in charge of everything else. Verified working end-to-end in Docker.
- **`spring-boot-maven-plugin` needs an explicit `<executions>`** (`repackage` bound to `package`) in the parent's `pluginManagement` — this project pins Spring Boot via BOM import rather than inheriting `spring-boot-starter-parent`, and that parent is what normally wires the execution in implicitly. Without it, `package` silently produces a thin jar with no `Main-Class` manifest entry ("no main manifest attribute" at container startup) instead of an executable Spring Boot jar.
- **PostgreSQL 18 image changed its expected volume mount point**: it now wants the *parent* directory (`/var/lib/postgresql`) mounted, not `/var/lib/postgresql/data` directly, so it can lay out major-version-specific subdirectories. Mounting the old path makes the container refuse to start on a fresh volume. All 9 Postgres services mount `<service>-pgdata:/var/lib/postgresql`.
- **Gateway error handling for a down backend**: by default, a proxying failure (backend container stopped, DNS lookup failing inside the compose network, connection refused) falls through to Spring Boot's generic reactive error handler and surfaces as a bare `500`, and with no explicit HTTP client timeout configured, Reactor Netty can leave the client hanging well past what's reasonable before that even happens (observed: 15s+ with zero bytes back, against a stopped container). This directly violates the frozen I/O matrix ("Target service down -> 502/503, not a hang"), so `gateway/application.yml` sets explicit `spring.cloud.gateway.server.webflux.httpclient.connect-timeout` (3000ms) and `.response-timeout` (5s), and `GatewayErrorHandlerConfiguration` registers a higher-precedence `ErrorWebExceptionHandler` (`@Order(-2)`, ahead of Boot's default at `-1`). It only maps *network-failure* exceptions (`ConnectException`, `UnknownHostException`, `TimeoutException`, Netty connect/read/write-timeout, Reactor Netty's `PrematureCloseException` — matched by walking the cause chain) to `502` with the spine's `{ error: { code, message } }` envelope; anything else is left to `Mono.error(ex)` so a routing/config bug still surfaces as a normal `500` instead of being mislabeled "target service did not respond". Covered by `GatewayErrorHandlerConfigurationTest` (`WebTestClient` + `@SpringBootTest(webEnvironment = RANDOM_PORT)`, adds one extra route via a `RouteLocator` bean pointed at a closed local port). Verified: stopping a backing container turns a 15s+ hang into a `502` in ~75ms.
- **Kafka (KRaft, no ZooKeeper)** uses the official `apache/kafka:4.3.1` image with the single-node KRaft env-var shape taken directly from the image's own upstream example (`docker/examples/docker-compose-files/single-node/plaintext` in the `apache/kafka` source tree), adapted to not publish a host port (AD-6) and to advertise itself as `kafka:9092` on the compose network. `KAFKA_LOG_DIRS` is backed by a named volume (`kafka-data`) mounted at `/home/appuser` (the image's existing non-root home, already owned by that user) rather than at the log-dir path directly under `/tmp` -- a fresh volume mounted straight onto `/tmp/kraft-combined-logs` comes up root-owned with nothing to copy permissions from, which the broker (running as uid 1000) then can't write to. Verified: created a topic, restarted the container, topic was still listed after restart.
- **Known inefficiency, not fixed in this story:** each service's Docker build stage resolves its Maven dependencies independently (no shared `~/.m2` cache mount across the 9+1 build stages), so a full `docker compose build` re-downloads the same Spring Boot/Hibernate/Tomcat jars once per module. Functionally correct (all 10 images build), just slower than necessary; a `--mount=type=cache,target=/root/.m2` on the two `RUN mvn ...` lines in each Dockerfile would fix this if build time becomes a problem.
- Repeated transient TLS/registry hiccups were observed while building against Maven Central and Docker Hub in this sandbox (handshake resets, one truncated dependency download); all were resolved by a plain retry with no config change, i.e. not caused by anything in this scaffold.

## Spec Change Log

## Review Triage Log

- **Gateway error handler treats every exception as upstream-unavailable** — `medium` — Verified: `GatewayErrorHandlerConfiguration.java:39` has no `instanceof` narrowing; any `Throwable` reaching this `@Order(-2)` handler is mapped to `502 UPSTREAM_UNAVAILABLE`, mislabeling a non-network failure (routing/config exception) as "target service did not respond." → **patch**: narrow to `ConnectException`/`TimeoutException`-style network failures; delegate anything else via `Mono.error(ex)`.
- **No automated test for Gateway error-handling behavior** — `medium` — Verified: no test file exists anywhere in the diff (confirmed by grep for `src/test`/`@Test`/`Testcontainers`); `gateway/pom.xml` already declares `spring-boot-starter-test`/`reactor-test` unused; the only check of this frozen I/O-matrix row is a one-time manual `curl` recorded in Implementation Notes — a future regression would ship silently since CI/CD is out of scope here. → **patch**: add a `WebTestClient`/`@SpringBootTest` test asserting `502` + the `UPSTREAM_UNAVAILABLE` envelope against a closed port.
- **Kafka KRaft log dir has no persistent volume** — `low` — Verified: `KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'` in `docker-compose.yml` has no matching volume mount, so broker/topic state is lost on any container restart, not only `down -v` — surprising for `incident-case-service`'s upcoming Kafka consumer work. → **patch**: add a named volume for the Kafka log dir.
- **README has no Docker Desktop memory guidance for a 20-container stack** — `low` — Verified: no minimum-memory note exists; 9 JVM services + 9 Postgres + Kafka could thrash or OOM a modestly-provisioned Docker Desktop. → **patch**: one README line under Prerequisites.
- **README doesn't explain the empty `package-info.java` convention** — `low` — Verified: hexagonal packages are represented via `package-info.java` rather than left absent, with no note on why. → **patch**: one README line under Repository layout.
- **README has no build-cache troubleshooting note** — `low` — Verified: Implementation Notes records hitting transient registry/cache issues during development, but README's teardown section doesn't mention `--no-cache` or how to recover. → **patch**: one README line.
- **`.gitignore` doesn't cover IDE artifacts under `backend/`** — `low` — Verified: `backend/.dockerignore` excludes `.idea/`/`*.iml`/`.DS_Store` from the Docker build context, but `.gitignore` has no equivalent, so opening the Java modules in an IDE would leave those trackable. → **patch**: add `.idea/` and `*.iml` to `.gitignore`.
- **Hardcoded trivial Postgres credentials in docker-compose/application.yml** — `low`, rejected — Real but expected for a local-only demo stack (Non-goals explicitly excludes production deployment); env-var indirection would add more complexity than the defect's everyday impact justifies.
- **`postgres:18.6`/`apache/kafka:4.3.1` pinned by tag, not digest** — `false` — The spine's Stack table only requires exact version pinning, which explicit tags already satisfy; digest-level immutability was never part of the stated goal.
- **No Flyway/Liquibase migration tooling wired** — rejected, out of scope — The frozen Intent's "Never" list explicitly excludes business capabilities (CAP-1..11) from this story; no service has an entity yet, so there is nothing to migrate. Revisit with the first capability story that adds one.
- **Gateway's default 404 for an unmapped route isn't in the I/O matrix** — `false` — Standard, expected Spring Cloud Gateway behavior, not a defect; no bad outcome occurs at the cited location.
- **Possible TOCTOU race between `isCommitted()` and `writeWith()`** — `false` — Verified: both calls happen synchronously within the same single-invocation lambda with no intervening async boundary and no other writer for this exchange; there is no concurrent path that could commit the response between the check and the write.

## Design Notes

Kafka runs in KRaft mode (no ZooKeeper container) — Kafka 4.x removed ZooKeeper support entirely, so a ZK container would not even be an option with the pinned 4.3.1. `incident-case-service` is the only service with a Kafka consumer in this story's scope, but the broker is still shared infrastructure stood up here since every future story that touches events depends on it existing.

## Verification

**Commands:**
- `docker compose -f backend/docker-compose.yml build` -- expected: all 10 images build successfully (this is the build gate; host JDK 17 cannot target `--release 25`, so this replaces a host `mvn package` check)
  - **Result: PASS.** All 10 images (`gateway` + 9 services) built successfully via `docker compose -f backend/docker-compose.yml build`.
- `docker compose -f backend/docker-compose.yml up -d --wait` -- expected: command returns 0, all containers `healthy`
  - **Result: PASS.** Verified twice: once continuing from an already-built state, and once as a true cold start (`down -v` to drop all 9 Postgres volumes, then `up -d --wait` from empty volumes). Both times all 20 containers (gateway + 9 services + 9 Postgres + Kafka) reported `healthy` and the command exited 0.
- `for s in client-identity offers card-account card-issuance operations risk-blocks cashback notifications incident-case; do curl -sf http://localhost:8080/$s/actuator/health; done` -- expected: nine `200 {"status":"UP"}` responses
  - **Result: PASS.** All nine returned HTTP 200 with `"status":"UP"` in the body (Spring Boot auto-adds a `"groups":["liveness","readiness"]` field alongside it when it detects it's running in a container -- not something this config added, and harmless: still 200 and still `"status":"UP"`).
- `docker compose -f backend/docker-compose.yml ps --format json` -- expected: only `gateway`'s entry has a non-empty `Publishers`/ports field
  - **Result: PASS.** `gateway` is the only service with a non-empty `Publishers` array (`0.0.0.0:8080`/`[::]:8080` -> `8080/tcp`); all other 19 entries have an empty `Publishers` array.
- Additional check beyond the listed commands, against the frozen I/O & Edge-Case Matrix's "Gateway routing" row ("Target service down -> Gateway returns 502/503, not a hang"): stopped `offers-service` with the stack otherwise up, then `curl` through the Gateway to `/offers/actuator/health`.
  - **Result: PASS after a fix, now also covered by an automated test.** Initially the request hung for the full 15s client timeout with zero bytes back (Reactor Netty had no explicit connect/response timeout, and the compose network's embedded DNS returns NXDOMAIN for a stopped container's hostname rather than an immediate refusal). Added explicit `httpclient.connect-timeout`/`response-timeout` plus a `GatewayErrorHandlerConfiguration` (see Implementation Notes) that maps network-failure exceptions to `502`. Re-tested manually: `502` returned in ~75ms with `{"error":{"code":"UPSTREAM_UNAVAILABLE",...}}`; restarted `offers-service` afterwards and confirmed the full stack returned to all-healthy. `GatewayErrorHandlerConfigurationTest` now exercises the same scenario (closed local port, no docker-compose network involved) as part of `mvn test`, so it no longer depends on a manual curl to catch a regression.
