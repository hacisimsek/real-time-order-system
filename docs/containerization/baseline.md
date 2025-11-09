# Container Baseline Audit

The application services now share a hardened Docker baseline.  
This document codifies the checklist so new services inherit the same guarantees.

## 1. Build Pattern

- **Multi-stage build** using `maven:3.9.7-eclipse-temurin-17` to compile only the
  required module (`common-security` + target service).
- `# syntax=docker/dockerfile:1.7` is enabled for consistent BuildKit behaviour and
  future optional cache mounts.
- `ARG MAVEN_IMAGE` / `ARG RUNTIME_IMAGE` make it possible to bump base images centrally.
- Build stage runs `mvn -q -DskipTests package`; artefacts are copied into a slim
  `eclipse-temurin:17-jre` runtime image.

## 2. Runtime Hardening

- Every container runs as a dedicated **non-root `spring` user** and owns `/app`.
- OCI metadata (`org.opencontainers.image.*`) is stamped for traceability.
- Default `JAVA_OPTS` cap heap usage via `MaxRAMPercentage`.
- Ports are exposed explicitly (8081–8084) for clarity.

## 3. Health & Readiness

- Docker-level `HEALTHCHECK` hits the Spring Boot `/actuator/health` endpoint with a
  15s interval and 30s start-period. This surfaces in `docker ps` and enables Compose
  / future Kubernetes readiness gates.
- Services rely on the `PORT` env var (injected via Compose) to keep health probes portable.

## 4. Multi-arch Readiness

- `--platform=$BUILDPLATFORM` is declared for the build stage, making cross-compilation
  (e.g., `docker buildx build --platform linux/arm64`) work without editing the Dockerfiles.

## 5. Verification Checklist

1. `docker build -t rtos/order-service:dev -f backend/order-service/Dockerfile backend`
2. `docker run --rm -p 8081:8081 rtos/order-service:dev` and confirm healthcheck reports `healthy`.
3. Repeat for notification, inventory, and reporting services.
4. Run `docker scout cves` (or preferred scanner) and capture the report in release notes.

Keep this baseline document updated when adding new runtime flags, scanners, or user policies.
