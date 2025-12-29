# Yearly Performance Targets
**Hacı Şimşek**  
**Manager:** Osman YALIN  
**Team:** Turkish Web Team  
**Year:** 2025

---

## Table of Contents
1. [Real-Time Order Processing and Messaging System](#real-time-order-processing-and-messaging-system)
    - [Analyze](#analyze)
    - [Timeline – ETA](#timeline--eta)
    - [Project Details](#project-details)
    - [Tech Stack](#tech-stack)
    - [Gains](#gains)

---

## Real-Time Order Processing and Messaging System

### Analyze
This project aims to provide a fully functional **microservice-based architecture** to process high-volume orders and notifications in real time in a single **20-week phase**.  
The solution will adopt an **event-driven design** that encompasses supporting infrastructure such as order acquisition, processing, notification delivery, reporting, caching, messaging, containerization, and monitoring.

- **Redis** → High-speed data caching
- **RabbitMQ** → Inter-service communication, job queue management, message routing
- **PostgreSQL** → Primary database
- The system will be designed with **scalability**, **security**, and **ease of maintenance** in mind.

---

### Timeline – ETA

| Phase | Duration | Period | Key Deliverables |
|-------|-----------|--------|------------------|
| **1. Planning & Analysis** | 3 Weeks | Aug 16 – Sep 5, 2025 | Project kickoff, requirements gathering, environment setup, architecture design |
| **2. Core Microservices Development** | 6 Weeks | Sep 6 – Oct 17, 2025 | Order, Notification, and Inventory services, PostgreSQL integration, RESTful APIs |
| **3. Messaging Infrastructure** | 3 Weeks | Oct 18 – Nov 7, 2025 | RabbitMQ setup, event handling, DLQ and idempotency logic |
| **4. Security Implementation** | 2 Weeks | Nov 8 – Nov 21, 2025 | JWT authentication, RBAC, API Gateway security |
| **5. Reporting Service** | 2 Weeks | Nov 22 – Dec 5, 2025 | Reporting module, export features, dashboard visualization |
| **6. Containerization & Orchestration** | 2 Weeks | Dec 6 – Dec 19, 2025 | Dockerization, Kubernetes orchestration, Prometheus & Grafana monitoring |
| **7. Testing, CI/CD & Production Deployment** | 1 Week | Dec 20 – Dec 27, 2025 | Integration & load testing, CI/CD with GitHub Actions, go-live |

---

### Project Details

#### 1. Planning & Analysis – Aug 16 – Sep 5, 2025 (3 Weeks)
- Project kickoff and requirements gathering
- Finalize full project scope and delivery milestones
- Set up development environment (Java/Spring Boot, PostgreSQL, Redis, RabbitMQ)
- Architecture design: **Microservices + CQRS + Event-driven**
- **Execution Plan**
  - Stakeholder Alignment: identify business capabilities (ordering, inventory, notifications, reporting) and capture a shared glossary.
  - Architecture Boards: draft context/container diagrams and event flows; validate the choice of PostgreSQL, RabbitMQ, Redis against scalability and HA requirements.
  - Environment Bootstrap: provision dev infra (Docker Compose), set up git branching strategy, and define coding standards (Java 17, Spring Boot 3.5, Maven wrapper).
  - Backlog Shaping: break down epics into deliverable user stories with acceptance criteria; prioritize for the subsequent phases.
  - Risk Register: log technical/operational risks (DB capacity, message ordering, security gaps) and mitigation plans.

#### 2. Core Microservices Development – Sep 6 – Oct 17, 2025 (6 Weeks)
- **Order Management Service:** CRUD, workflow management
- **Notification Service:** Email, SMS, push notifications
- **Inventory Service:** Real-time stock updates
- PostgreSQL schema design and integration
- RESTful API development
- Unit testing & error handling
- **Execution Plan**
  - Domain Modeling: define aggregates/entities (Order, OrderItem, InventoryItem, NotificationRequest) and map them to Flyway migrations.
  - Service Skeletons: scaffold Spring Boot apps with REST controllers, DTOs, service layers, repositories, and exception handling patterns.
  - Database Integration: configure JDBC/Hibernate, connection pooling, and transactional semantics; add baseline seed data scripts if needed.
  - Messaging Hooks: implement outbox/event publishing interfaces in the order service so phase 3 can plug in messaging without refactors.
  - API Validation: add bean validation, error responses, and contract tests (MockMvc/WebTestClient).
  - Test Strategy: write JUnit/Mockito suites for business logic and security guards; ensure `mvn -pl <service> -am test` runs clean in CI.

#### 3. Messaging Infrastructure – Oct 18 – Nov 7, 2025 (3 Weeks)
- RabbitMQ setup and configuration
- Exchange, queue, and binding definitions
- Event publish/subscribe integration
- Retry logic, DLQ management, and idempotency
- **Execution Plan**
  - Topology Blueprint: define exchanges (`order.events`, retry/dlx exchanges), routing keys, and queue naming conventions for each consumer.
  - Provisioning Automation: add Rabbit definitions to `deploy/rabbitmq/definitions.json` and ensure services declare queues/bindings via Spring AMQP.
  - Outbox Publisher: finalize reliable publish loop with ack/retry semantics and observability metrics.
  - Consumer Idempotency: implement message log/retry strategy (e.g., for inventory/reporting) and standardize manual ack handling with dead-letter routing.
  - DLQ/Retry Tooling: expose operational endpoints/scripts to replay or purge DLQ messages.
  - Messaging Tests: create integration tests or containerized verification to assert routing keys and error handling logic.

#### 4. Security Implementation – Nov 8 – Nov 21, 2025 (2 Weeks)
- JWT-based authentication
- RBAC for user roles and permissions
- API Gateway security enforcement (rate limiting, validation)
- **Execution Plan**
  - Requirements & Gap Analysis: catalogue protected endpoints, role matrix, and inter-service trust boundaries.
  - Shared Auth Package: implement reusable JWT verification module and rotation strategy.
  - Order-Service Hardening: add Spring Security guards and controller-level authorization policies.
  - Notification & Inventory Hardening: align authentication middleware and tighten management endpoints.
  - API Gateway Policies: enforce rate limiting, TLS termination, and secure defaults (CORS, headers).
  - Security Testing: automate JWT scope tests, dependency scans, and document incident response runbooks.

#### 5. Reporting Service – Nov 22 – Dec 5, 2025 (2 Weeks)
- Reporting (daily, weekly, monthly)
- JSON/CSV export capabilities
- Dashboard visualization
- **Execution Plan**
  - Requirements & Data Mapping: confirm reporting KPIs, data sources, retention windows, and aggregation cadence.
  - Reporting Storage Design: choose OLAP schema or materialized views, wire ETL/outbox pipelines feeding the reporting DB.
  - Report APIs & Exports: build secured REST endpoints for canned reports, pagination, CSV/JSON exports, and schedule delivery hooks.
  - Visualization Layer: provision dashboards (Grafana or custom UI) with reusable filters, role-based widgets, and drill-down links.
  - Performance Validation: run load tests on heavy reports, add caching/indices to meet SLA.<br>Document fallback procedures.
  - Documentation & Handover: publish report catalog, operational runbooks (refresh cadence, alerting), and user onboarding guides.
  - Demo Note: for presentation purposes, the reporting cache can be backed by Redis (`APP_REPORTING_CACHE_PROVIDER=redis`) instead of in-memory cache.
  - Demo Note (ops): if Redis-backed caches cause errors after a code change, flush `reportTotals` / `reportTopCustomers` (e.g., via `DELETE /actuator/caches/...`) and retry.

#### 6. Containerization & Orchestration – Dec 6 – Dec 19, 2025 (2 Weeks)
- Dockerizing all services
- Orchestration with Docker Compose (local demo scope)
- Monitoring & alerting setup (Prometheus, Grafana)
- **Execution Plan**
  - Container Baseline Audit: align all service Dockerfiles, build args, health checks, and multi-arch tags; document image hardening checklist.
  - Compose Baseline: keep docker-compose definitions aligned for app services, RabbitMQ, Postgres, and observability components.
  - Runtime Policies: add readiness/liveness probes and resource hints where applicable for local validation.
  - Observability Wiring: configure Prometheus scrape configs, Grafana dashboard provisioning, and alert rules for key KPIs (latency, staleness, queue depth).
  - Release Documentation: publish runbook for container build/push, local deployment checklist, rollback steps, and troubleshooting flowcharts.

- **Monitoring Checklist (Local Demo)**
  - Prometheus targets: app services + RabbitMQ exporter scraping successfully (`deploy/observability/prometheus.yml`).
  - Grafana dashboards: Reporting Overview + RTOS Services Overview load without errors (`deploy/observability/dashboards/reporting-overview.json`, `deploy/observability/dashboards/rtos-services.json`).
  - Key metrics: throughput, staleness, queue depth visible with recent data (Grafana dashboards above).
  - Alerts: rules loaded and firing state visible in Prometheus UI (`deploy/observability/alerts.yml`).
  - Runbook: quick show URLs and PromQL snippets documented (`docs/reporting/runbook.md`).

#### 7. Testing, CI/CD & Production Deployment – Dec 20 – Dec 27, 2025 (1 Week)
- Integration, load, and performance testing
- CI/CD pipeline (GitHub Actions)
- Operational documentation and production go-live

> **Note (demo/learning scope):** This repository is prepared for learning and presentations, not a production go-live.  
> The “production deployment” outcome is treated as a reproducible **local demo deployment** via Docker Compose, with CI running tests/builds to keep the project stable for demos.

---

### Tech Stack

| Category | Technologies |
|-----------|--------------|
| **Backend** | Java (Spring Boot) |
| **Database** | PostgreSQL |
| **Messaging** | RabbitMQ |
| **Caching** | Redis |
| **Architecture** | Microservices, CQRS, Event-driven |
| **Containerization** | Docker (local demo scope) |
| **Security** | JWT, RBAC |
| **CI/CD** | GitHub Actions |
| **Monitoring** | Prometheus, Grafana |

---

### Gains
A software developer aiming to leverage and enhance technical expertise by utilizing technologies adopted within the team, integrating them with new tools to create robust, secure, and scalable infrastructures.

Experienced in the **end-to-end lifecycle** of microservice-based systems — from design and development to deployment and maintenance — capable of processing **high-volume data streams** with low latency using technologies such as **RabbitMQ**, **PostgreSQL**, **Redis**, **Docker**, and **Kubernetes (Minikube)**.

Skilled in:
- Designing relational schemas and analytics workflows
- Applying **JWT** and **RBAC** for secure access control
- Implementing **containerization**, **orchestration**, **CI/CD**, and **system observability**
- Ensuring scalability, maintainability, and production readiness

---

**Document Status:** Public  
