# mts-client-card-office backend

Story 0 (infra scaffold) output: a Maven multi-module skeleton for the
`mts-client-card-office` backend -- an API Gateway plus nine bounded-context
microservices (per `ARCHITECTURE-SPINE.md` AD-1), wired together with
docker-compose, Kafka and one PostgreSQL instance per service. Each service is
currently just a booting Spring Boot app with a health endpoint -- no business
capability (CAP-1..11) is implemented yet. Those arrive in the stories that
follow this one.

See `docs/specs/spec-mts-client-card-office-backend/` for the spec and
`docs/planning/architecture/architecture-mts-client-card-office-2026-09-22/ARCHITECTURE-SPINE.md`
for the architecture this scaffold implements.

## Prerequisites

- Docker Desktop (or an equivalent Docker Engine + Compose v2) running locally,
  with at least **6 GB of memory** allocated to it (Docker Desktop > Settings >
  Resources) -- the stack is 20 containers (gateway + 9 JVM services + 9
  Postgres + Kafka).
- Nothing else. The JDK version on your machine does not matter -- see
  "Host JDK vs. build JDK" below.

## Bring the stack up

From the repo root:

```sh
docker compose -f backend/docker-compose.yml build
docker compose -f backend/docker-compose.yml up -d --wait
```

`--wait` blocks until every container (gateway, the 9 services, their 9
Postgres instances, and Kafka) reports healthy, or fails if one doesn't within
its configured `start_period`/`retries` window. A clean pull/build on a slow
connection can take several minutes the first time; subsequent builds reuse
Docker's layer cache.

## Verify it's up

Check container health directly:

```sh
docker compose -f backend/docker-compose.yml ps
```

Only `gateway` should show a published port (`0.0.0.0:8080->8080/tcp`, AD-6) --
every other row's ports column should be empty.

Hit each service's health endpoint through the Gateway (the frontend's only
path in, per AD-6):

```sh
for s in client-identity offers card-account card-issuance operations risk-blocks cashback notifications incident-case; do
  echo "== $s =="
  curl -sf "http://localhost:8080/$s/actuator/health"
  echo
done
```

Each call should print `{"status":"UP"}`. A service whose route returns
502/503 means that backing container isn't healthy yet or crashed -- check
`docker compose -f backend/docker-compose.yml logs <service>`.

## Bring the stack down

```sh
docker compose -f backend/docker-compose.yml down
```

Add `-v` to also drop the 9 Postgres volumes (useful for a truly clean-slate
retest of cold start; not needed for routine shutdown).

**If a rebuild doesn't pick up a change you made** (source, `pom.xml`,
Dockerfile), it's almost always a stale Docker build cache layer -- force a
clean rebuild with `docker compose -f backend/docker-compose.yml build --no-cache`.

## Repository layout

```text
backend/
  pom.xml                       # parent aggregator POM: stack pins (Java 25, Spring Boot 4.1.1 BOM), 10 modules
  docker-compose.yml            # gateway + 9 services + Kafka + 9 Postgres instances
  mvnw, mvnw.cmd, .mvn/         # Maven Wrapper, pinned to 3.9.16 (see below)
  gateway/                      # Spring Cloud Gateway: the only container that publishes a host port
  client-identity-service/      # one directory per bounded context (AD-1), each with the same internal shape:
  offers-service/               #   domain/, application/, adapters/in/web/, adapters/out/persistence/
  card-account-service/         #   (+ adapters/out/external/ for offers and risk-blocks; adapters/in/messaging/
  card-issuance-service/        #    for incident-case)
  operations-service/
  risk-blocks-service/
  cashback-service/
  notifications-service/
  incident-case-service/
```

Every service module follows the hexagonal package layout under
`ru.mtsbank.cardoffice.<service>` (see `ARCHITECTURE-SPINE.md` § Design
Paradigm). In this story those packages exist but are empty (a
`package-info.java` documents each layer's intended contents) -- they're
populated as each capability's story lands. Git doesn't track empty
directories, so `package-info.java` is what makes an otherwise-empty package
show up in the checkout at all, not just documentation.

## Host JDK vs. build JDK -- why they don't match, on purpose

The pinned stack (`ARCHITECTURE-SPINE.md` § Stack) targets **Java 25 LTS** and
**Spring Boot 4.1.1**. The host machine used to scaffold this story runs
**JDK 17**, which cannot compile with `--release 25`. Rather than upgrading the
host toolchain as a side effect of an infra story, Story 0 decided: **all
building and running happens inside Docker**, where every image (build stage
and runtime stage alike) is `eclipse-temurin:25`. The host JDK version is
simply irrelevant to whether this stack builds or runs.

Practically, this means:

- **The build gate is `docker compose -f backend/docker-compose.yml build`**,
  not a host-side `mvn package` or `./mvnw package` -- those would fail on a
  JDK 17 host with a `--release 25` target, which is expected and not a bug.
- **`mvnw`/`mvnw.cmd` are checked in for host-side editing convenience only**
  (e.g. so an IDE or a quick `./mvnw -N validate` has something to resolve
  against) -- they are pinned to Maven 3.9.16 but still run on whatever JDK is
  on your PATH, and are not what CI/the build gate above invokes. Each
  Dockerfile's build stage installs Maven 3.9.16 itself, independently, from
  the official binary distribution.
- If/when the team standardizes on a JDK 25 host toolchain, this note (and the
  Docker-only build constraint) can be revisited -- it was a Story 0 decision
  to unblock the scaffold without a local toolchain change, not a permanent
  architectural rule.

## Gateway routing

The Gateway (Spring Cloud Gateway, reactive/WebFlux) exposes one route per
service, path-prefixed by the service's short name from AD-1, forwarding by
docker-compose service DNS name with the prefix stripped:

| Gateway path prefix | Forwards to |
| --- | --- |
| `/client-identity/**` | `client-identity-service:8080` |
| `/offers/**` | `offers-service:8080` |
| `/card-account/**` | `card-account-service:8080` |
| `/card-issuance/**` | `card-issuance-service:8080` |
| `/operations/**` | `operations-service:8080` |
| `/risk-blocks/**` | `risk-blocks-service:8080` |
| `/cashback/**` | `cashback-service:8080` |
| `/notifications/**` | `notifications-service:8080` |
| `/incident-case/**` | `incident-case-service:8080` |

This is scaffolding to prove AD-6 end to end (single entry point, no direct
service access from outside the compose network). The real business resource
shape (`/clients/{id}`, `/cards/{cardId}/operations`, etc., per AD-8) is
layered on inside each service as its capability story lands -- the Gateway
route table above will very likely need additional/adjusted routes at that
point; it is not yet the final routing contract.

## Kafka

Kafka runs in KRaft mode (no ZooKeeper container -- Kafka 4.x removed
ZooKeeper support entirely). It has no published host port (AD-6): it's
reachable only from other containers on the compose network, as
`kafka:9092`. `incident-case-service` is the only consumer in this story's
scope (and has no consumer code yet -- that arrives with CAP-10's story), but
the broker is shared infrastructure every future event-driven story depends on.

To manually publish a test message from your host while iterating, exec into
the broker container itself, e.g.:

```sh
docker compose -f backend/docker-compose.yml exec kafka \
  /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic incident-case.mass-incident.opened
```

## What's not here yet

- No business endpoints beyond `/actuator/health` -- CAP-1..11 are separate
  stories.
- No CI/CD or cloud deployment config (deferred in the architecture spine).
- No real SFM/FSSP/antifraud/offer-engine/monitoring integration -- those stub
  adapters arrive with their owning capability's story.
