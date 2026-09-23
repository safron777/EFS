# syntax=docker/dockerfile:1
# Multi-stage build for incident-case-service. Both stages pin eclipse-temurin:25
# (ARCHITECTURE-SPINE.md Stack) so the image is never at the mercy of whatever
# JDK happens to be on the host (currently 17) -- builds and runs happen only
# inside Docker, by design (see backend/README.md).
#
# Build context for this Dockerfile is expected to be `backend/` (see
# docker-compose.yml `build.context: .` / `build.dockerfile: incident-case-service/Dockerfile`),
# so the parent POM is reachable at ../pom.xml relative to this module.

FROM eclipse-temurin:25 AS build

ARG MAVEN_VERSION=3.9.16
# Maven is installed from the official binary distribution here, independently
# of the checked-in Maven Wrapper (backend/mvnw), which is for host-side
# editing convenience only and is not what this build gate runs (see
# docs/specs/spec-mts-client-card-office-backend/stories/0-infra-scaffold.md).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && rm /tmp/maven.tar.gz \
    && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/apache-maven-${MAVEN_VERSION}/bin:${PATH}"

WORKDIR /build

# Parent POM first (dependencyManagement + stack pins), installed with -N so
# Maven does not need the other 9 module directories to be present yet.
COPY pom.xml ./pom.xml
RUN mvn -N -q install

# This module's POM next, to let Maven cache dependency resolution in its own
# Docker layer before the source tree (which changes far more often) is added.
COPY incident-case-service/pom.xml ./incident-case-service/pom.xml
RUN mvn -q -f incident-case-service/pom.xml dependency:go-offline

COPY incident-case-service/src ./incident-case-service/src
RUN mvn -q -f incident-case-service/pom.xml package -DskipTests

FROM eclipse-temurin:25

# curl is installed only so the container healthcheck declared in
# docker-compose.yml can probe the Actuator health endpoint.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/incident-case-service/target/incident-case-service.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
