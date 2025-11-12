# Real-Time Order System (RTOS)

RTOS is a microservice-based reference implementation for processing customer orders in real time. The system demonstrates reliable event publication, resilient consumers, and observability using widely adopted open-source technologies.

## At a Glance

- **Services**: Order (publish events), Inventory (reserve/release stock), Notification (mock notifications)
- **Messaging**: RabbitMQ with retry and DLQ support
- **Persistence**: PostgreSQL
- **Caching / future work**: Redis
- **Observability**: Micrometer → Prometheus → Grafana
- **Reliability patterns**: Outbox pattern, manual acknowledgements, idempotency store, DLQ replay utility

## Repository Layout

```
real-time-order-system/
├─ backend/
│  ├─ order-service/
│  ├─ inventory-service/
│  └─ notification-service/
├─ deploy/
│  ├─ docker-compose.yml
│  └─ observability/
└─ docs/
```

## Prerequisites

- Java 17 (JDK)
- Maven ≥ 3.9.9 (wrapper is included, but local Maven is useful)
- Docker Desktop with Compose v2
- Optional: `jq` for pretty-printing JSON

```bash
java -version
mvn -v            # optional, wrapper is provided
docker -v
docker compose version
```

## Quick Start

1. **Clone and enter the project**
   ```bash
   git clone git@github.com:hacisimsek/real-time-order-system.git
   cd real-time-order-system
   ```

2. **Start infrastructure services (Postgres, Redis, RabbitMQ)**
   ```bash
   cd deploy
   docker compose up -d postgres redis rabbitmq
   ```
   Credentials are defined in `deploy/.env`:
   - Postgres: `app` / `app`, database `appdb`
   - RabbitMQ: `rtos` / `rtos`

3. **Build and start application services**
   ```bash
   docker compose build order-service notification-service inventory-service reporting-service
   docker compose up -d order-service notification-service inventory-service reporting-service
   ```
   Prefer running locally from IntelliJ? Import the Maven project, then for each service (`backend/*-service`) create a Spring Boot run configuration pointing to the `...ServiceApplication` class, set `SPRING_PROFILES_ACTIVE=dev` and the DB/Rabbit env variables from `deploy/.env`, and press **Run**. Detailed environment exports live in `docs/setup/running.md#running-services-locally-no-docker-images`.

4. **Check health endpoints**
   ```bash
   curl -s http://localhost:8081/actuator/health
   curl -s http://localhost:8082/actuator/health
   curl -s http://localhost:8083/actuator/health
   curl -s http://localhost:8084/actuator/health
   ```

5. **Fetch the developer JWT token** (printed on startup)
   ```bash
   docker compose logs order-service | grep "DEV ADMIN TOKEN"
   ```
   Use the returned `Bearer ...` token in `Authorization` headers when calling protected endpoints.

6. **(Optional) Run service tests using the Maven wrapper**  
   Install the shared security library once, then execute each service’s suite. The `HOME` export keeps Maven’s cache inside the repo.
   ```bash
   # from the repo root
   backend/order-service/mvnw -q -f ../common-security/pom.xml install

   RABBIT_USER=rtos RABBIT_PASS=rtos HOME=$(git rev-parse --show-toplevel) \
     backend/order-service/mvnw -q test

   RABBIT_USER=rtos RABBIT_PASS=rtos HOME=$(git rev-parse --show-toplevel) \
     backend/notification-service/mvnw -q test

   RABBIT_USER=rtos RABBIT_PASS=rtos HOME=$(git rev-parse --show-toplevel) \
     backend/inventory-service/mvnw -q test

   RABBIT_USER=rtos RABBIT_PASS=rtos HOME=$(git rev-parse --show-toplevel) \
     (cd backend && mvn -pl reporting-service test)
   ```

7. **Manual end-to-end smoke test**
   ```bash
   # Seed inventory
   curl -s -X PUT http://localhost:8083/inventory/ABC-001/adjust \
     -H 'Content-Type: application/json' \
     -d '{"delta": 20, "reason": "seed"}'

   # Create an order
   curl -s -X POST http://localhost:8081/orders \
     -H 'Content-Type: application/json' \
     -H 'Authorization: Bearer <DEV_TOKEN>' \
     -d '{"customerId":"C-1001","amountCents":1999,"currency":"TRY","items":[{"sku":"ABC-001","qty":1}]}'

   # Update the order status
   curl -s -X PATCH http://localhost:8081/orders/1/status \
     -H 'Content-Type: application/json' \
     -H 'Authorization: Bearer <DEV_TOKEN>' \
     -d '{"status":"FULFILLED"}'

   # Inspect inventory levels
   curl -s http://localhost:8083/inventory/ABC-001
   ```

8. **Reporting service smoke test**
   ```bash
   # Trigger a CSV export (JWT token required)
   curl -s "http://localhost:8084/reports/orders?period=DAILY&refresh=true" \
     -H 'Authorization: Bearer <DEV_TOKEN>' | jq

   curl -s "http://localhost:8084/reports/orders/totals?period=DAILY" \
     -H 'Authorization: Bearer <DEV_TOKEN>' | jq

   curl -s "http://localhost:8084/reports/orders/top-customers?limit=5" \
     -H 'Authorization: Bearer <DEV_TOKEN>' | jq
   ```

9. **Shut everything down**
   ```bash
   cd deploy
   docker compose down
   ```

## Runtime Modes

- **Docker Compose (recommended for demos):** Builds/pulls the four services plus Postgres, RabbitMQ, Prometheus, and Grafana. Follow `docs/setup/running.md#running-everything-via-docker-compose` for the full checklist (env overrides, smoke checks, teardown).
- **Local JVM (hot-reload friendly):** Start infra containers only (`postgres`, `rabbitmq`, `prometheus`, `grafana`) and run each Spring Boot app via `./mvnw spring-boot:run`. Environment exports and verification steps live in `docs/setup/running.md#running-services-locally-no-docker-images`.
- **Deep dive & metrics:** `docs/setup/runtime-technical.md` contains the sequence diagrams, KPI PromQL queries, and alert thresholds that back the dashboards/alerts referenced below.

## Reporting Service

The reporting service consumes `order.created.v1` events directly from RabbitMQ and maintains daily/weekly/monthly snapshots and rollups. Before starting the service, ensure the queue is provisioned (the embedded `RabbitAdmin` will create it automatically when the application connects):

- Exchange: `order.events`
- Routing key: `order.created.v1`
- Queue: `dev.reporting.order-created`

The following endpoints back dashboards and exports:

| Endpoint | Description |
|----------|-------------|
| `GET /reports/orders` | Paginated snapshot list (`page`, `size`, `sort`, `refresh` query params) |
| `GET /reports/orders/totals` | Aggregate totals for the selected window |
| `GET /reports/orders/top-customers` | Leaderboard of the top customers (configurable `limit`) |
| `GET /reports/orders/export` | CSV export of the current window |

Micrometer publishes metrics used by the Grafana dashboard under the `reporting_*` namespace (`reporting_orders_processed_total`, `reporting_order_processing_latency`, `reporting_order_amount_cents`, `reporting_last_order_timestamp_seconds`). Import the dashboard located at `deploy/observability/dashboards/reporting-overview.json` to visualise throughput, latency, and export activity.

> **Auth diagnostics**: When the service boots with `SPRING_PROFILES_ACTIVE=dev`, `SecurityDiagnosticsRunner` logs the issuer, audience, and number of configured secrets (e.g. `Reporting security config → issuer='rtos' ...`). Combined with the now-default `DEBUG` logging for `com.hacisimsek.security` and `org.springframework.security`, this makes it easy to pinpoint token wiring issues directly from `docker compose logs reporting-service`.

**Performance & Caching**

- `GET /reports/orders/totals` and `GET /reports/orders/top-customers` responses are cached via Caffeine (TTL 60s, max 500 entries by default). Override with `APP_REPORTING_CACHE_TTL` / `APP_REPORTING_CACHE_MAX_SIZE` in `.env`.
- Manual cache flush: `curl -X DELETE http://localhost:8084/actuator/caches/reportTotals` (or `reportTopCustomers`).
- Reporting-friendly indexes live in `backend/order-service/src/main/resources/db/migration/V3__reporting_indexes.sql` and must be applied wherever the `orders` table exists.
- Requirements & data-mapping reference: `docs/reporting/requirements.md`.
- Dashboard coverage & drill-down links: `docs/reporting/dashboard.md`.
- Operational runbook (cache tuning, refresh cadence, fallback steps): `docs/reporting/runbook.md`.
- Container hardening checklist (multi-stage builds, non-root users, healthchecks): `docs/containerization/baseline.md`.
- Container release & rollback playbook: `docs/containerization/release.md`.
- Runtime guide (Docker vs local workflows + metrics checklist): `docs/setup/running.md`.
- Technical runtime deep-dive (flows, metrics, alert thresholds): `docs/setup/runtime-technical.md`.

## Observability & Alerts

- Prometheus configuration: `deploy/observability/prometheus.yml` (scrapes all services plus RabbitMQ and loads `alerts.yml`).
- Alert rules: `deploy/observability/alerts.yml` (service-down, high latency, staleness, queue backlog).
- Grafana provisioning: `deploy/observability/grafana-datasource.yml`, `deploy/observability/grafana-dashboards.yml`, and dashboards in `deploy/observability/dashboards/`.
- Reporting dashboard: `deploy/observability/dashboards/reporting-overview.json`, featuring filters for `$instance`/`$source` and drill-down links to `/reports/**`.
- Order outbox metrics: `order_outbox_dispatch_total{result=*}` counters and `order_outbox_pending_events` gauge surface relay health in Prometheus.

> Docker Compose v2 ignores the legacy `version` key and may show a warning. It is safe to ignore or remove the `version` line if desired.

## Services & Ports

| Service             | Port | Description                               |
|---------------------|------|-------------------------------------------|
| Order Service       | 8081 | Order API + outbox publisher              |
| Notification Service| 8082 | Event consumer (mock notifications)       |
| Inventory Service   | 8083 | Stock reserve/release API + consumers     |
| PostgreSQL          | 5432 | Primary database `appdb`                  |
| RabbitMQ            | 5672 | AMQP broker (UI on 15672)                 |
| Prometheus          | 9090 | Metrics scraper                           |
| Grafana             | 3000 | Dashboards (credentials via `GF_SECURITY_ADMIN_*`, defaults to `admin`/`admin`) |

## Security

- All HTTP endpoints (except `actuator/**`) require a JWT Bearer token.
- Tokens are validated in every service using shared key rotation support (see `backend/common-security`).
- Default developer token (printed on startup in `dev` profile) carries the following authorities:

| Authority                 | Purpose                                        |
|---------------------------|------------------------------------------------|
| `ROLE_ORDER_READ`         | Fetch order details                            |
| `ROLE_ORDER_WRITE`        | Create orders and change order status          |
| `ROLE_INVENTORY_READ`     | Read inventory levels                          |
| `ROLE_INVENTORY_WRITE`    | Adjust/reserve/release/consume inventory       |
| `ROLE_INVENTORY_OPS`      | Trigger DLQ replay endpoints                   |
| `ROLE_NOTIFICATION_READ`  | Access notification-service protected routes   |
| `ROLE_REPORTING_READ`     | View reporting snapshots/totals/top customers  |
| `ROLE_REPORTING_EXPORT`   | CSV exports and snapshot refresh operations    |

Refresh or rotate JWT secrets by updating `app.security.secrets` in each service configuration; the first entry is used for signing, subsequent entries remain valid for verification during rollovers.

## Messaging Topology

- **Exchange**: `order.events` (topic)
- **Routing keys**:
  - `order.created.v1`
  - `order.status-changed.v1`
- **Queues**:
  - Inventory: `dev.inventory.order-created`, `dev.inventory.order-status-changed`
  - Notification: `dev.notifications.order-created`, `dev.notifications.order-status-changed`
- **Retry / DLQ**:
  - `<queue>.retry` routes messages back to the main exchange after TTL
  - `<queue>.dlq` stores dead-lettered messages
- **DLQ Replay**: Inventory service exposes a `DlqReplayer` helper to move messages from the DLQ to retry queues.

## Outbox Pattern (Exactly-once Publish)

The order service writes business events to the `outbox_events` table during the same transaction as the domain change.  
A background publisher drains the table and emits events to RabbitMQ, ensuring:

- No message is published if the database transaction rolls back
- Failed publish attempts can be retried without losing the original payload
- Operational visibility into outstanding events

Schema excerpt:

```sql
CREATE TABLE outbox_events (
  id            BIGSERIAL PRIMARY KEY,
  event_id      VARCHAR(64) NOT NULL UNIQUE,
  aggregate_id  BIGINT NOT NULL,
  aggregate_type VARCHAR(64) NOT NULL,
  event_type    VARCHAR(64) NOT NULL,
  exchange      VARCHAR(128) NOT NULL,
  routing_key   VARCHAR(128) NOT NULL,
  payload       JSONB NOT NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  attempts      INT NOT NULL DEFAULT 0,
  last_error    TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_outbox_status_created_at ON outbox_events(status, created_at);
```

## Observability

- Micrometer exports metrics to Prometheus; sample endpoint: `http://localhost:8081/actuator/prometheus`
- Grafana dashboards are provisioned via files under `deploy/observability/`
- RabbitMQ management UI: `http://localhost:15672` (user `rtos`, password `rtos`)

## Configuration

- Base environment resides in `deploy/.env`
- Service-level configuration lives in each service’s `application.yml`
- JWT secret, database connection details, and messaging topology can be overridden via environment variables

## Troubleshooting

- **Cannot connect to RabbitMQ**: Ensure docker-compose has started the `rabbitmq` service and ports 5672/15672 are available.
- **Maven wrapper cannot download**: Make sure Docker Desktop has network access; the wrapper stores artifacts in the project directory if `HOME` is set accordingly.
- **Health checks failing in Docker**: Containers rely on the `/actuator/health` endpoint. Review service logs with `docker compose logs <service>` for details.

Happy hacking!
