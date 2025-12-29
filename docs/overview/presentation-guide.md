# RTOS Presentation Guide (Demo-First)

This project is designed for learning + presentations (not production). Use this guide to deliver a clean story with a repeatable live demo.

Project progress vs. the 20-week plan (with remaining items) is tracked in `docs/overview/project-status.en.md` (EN) and `docs/overview/project-status.md` (TR).

## 0) Audience & Timing (recommended)

- **10–15 min**: Architecture + patterns + a short end-to-end demo
- **20–30 min**: Add observability + DLQ replay + caching comparison

## 1) Pre-flight (5 min before the talk)

1. Start Docker Desktop.
2. From repo root:
   ```bash
   cd deploy
   # (Optional) clean reset for a fresh demo
   docker compose down -v
   docker compose up -d postgres redis rabbitmq prometheus grafana
   docker compose up -d --build order-service inventory-service notification-service reporting-service
   ```
3. Fetch the dev token:
   ```bash
   docker compose logs order-service | grep "DEV ADMIN TOKEN"
   ```
   If the token is missing/expired, restart and re-check:
   ```bash
   docker compose restart order-service
   docker compose logs order-service | grep "DEV ADMIN TOKEN"
   ```
4. Quick health check:
   ```bash
   curl -s http://localhost:8081/actuator/health
   curl -s http://localhost:8084/actuator/health
   ```

## 2) Slide Outline (copy/paste into slides)

1. **Goal**: Real-time orders → events → downstream processing + reporting
2. **Architecture**: 4 services (Order/Inventory/Notification/Reporting) + Postgres + RabbitMQ + Redis + Prometheus/Grafana
3. **Key flow**: REST order → DB → outbox → `order.events` → consumers
4. **Reliability**: Outbox (publisher), manual ACK, idempotency store, DLQ/replay
5. **Security**: JWT + RBAC roles (actuator health remains public)
6. **Observability**: Prometheus scrape + Grafana dashboards + alert rules
7. **Reporting**: snapshots/rollups + exports
8. **Caching**: Reporting totals/top-customers cached (Caffeine or Redis)
9. **Live demo**: happy path + metrics + (optional) DLQ ops
10. **Wrap-up**: what you learned + next optional work (K8s/gateway/load tests)

> For ready-to-copy slide text (12 slides), see `docs/overview/slides-outline.md`.

### Slide 8 — Caching Strategy (Redis vs Caffeine)

- **Where it applies**: Reporting totals/top-customers are cached to keep dashboards fast.
- **Redis**: shared cache across instances (better for scale/consistency), a bit more latency.
- **Caffeine**: per-instance in-memory cache (fastest), resets on restart, not shared.
- **Demo tip**: compare `totals#1` vs `totals#2` to show cache hit speedup.

## 3) Live Demo Script (15–25 min)

### A) Happy path (end-to-end)

Use the full flow in `docs/testing/manual-happy-path.md`.

### B) Observability quick show (2–3 min)

- **Grafana**: http://localhost:3000 (default `admin/admin`)
- **Prometheus**: http://localhost:9090
- Suggested PromQL:
  - Throughput: `rate(reporting_orders_processed_total[5m])`
  - Reporting staleness: `time() - reporting_last_order_timestamp_seconds`
  - Queue backlog: `rabbitmq_queue_messages_ready{queue="dev.reporting.order-created"}`

### C) Caching demo (Redis vs Caffeine) (3–5 min)

Measure totals latency quickly:
```bash
DEV="Bearer <DEV_TOKEN>"
curl -s -o /dev/null -w "totals#1=%{time_total}\n" \
  -H "Authorization: $DEV" \
  "http://localhost:8084/reports/orders/totals?period=DAILY"
curl -s -o /dev/null -w "totals#2=%{time_total}\n" \
  -H "Authorization: $DEV" \
  "http://localhost:8084/reports/orders/totals?period=DAILY"
```

Switch cache provider:
- **Redis (default in `deploy/.env`)**:
  - `APP_REPORTING_CACHE_PROVIDER=redis`
- **In-memory**:
  - set `APP_REPORTING_CACHE_PROVIDER=caffeine` in `deploy/.env` (or `deploy/.env.local`)
  - restart reporting:
    ```bash
    cd deploy
    docker compose up -d --no-deps --force-recreate reporting-service
    ```
  - if you use `.env.local`, run with `--env-file .env.local` (Compose reads `.env` by default)

### D) Traffic demo (optional, 3–5 min)

Option 1: Postman Runner
- Set `sku_mode=order` and `sku_pool` in the Postman environment.
- Run **Orders → Create Order** for 50–200 iterations.

Option 2: k6
- Install k6 (if needed).
- Seed inventory for the SKU pool before running.
- Example:
  ```bash
  DEV_TOKEN="<DEV_TOKEN>" \
  SKU_POOL="ABC-001,ABC-002,ABC-003,ABC-004,ABC-005,ABC-006,ABC-007,ABC-008,ABC-009,ABC-010" \
  k6 run docs/load/k6-runsmoke.js
  ```
- Tune load: `ORDER_RATE`, `INVENTORY_RATE`, `REPORTING_RATE`, `DURATION`.
- Note: the script pulls `k6-utils` from `jslib.k6.io` (needs internet).

### E) DLQ replay ops (optional, 3–5 min)

If you have messages in DLQs, you can replay them via ops endpoints:

- Inventory:
  ```bash
  curl -s -X POST "http://localhost:8083/ops/replay/dev.inventory.order-created.dlq?max=50" \
    -H "Authorization: Bearer <DEV_TOKEN>"
  ```
- Reporting:
  ```bash
  curl -s -X POST "http://localhost:8084/ops/replay/dev.reporting.order-created.dlq?max=50" \
    -H "Authorization: Bearer <DEV_TOKEN>"
  ```

If there is no DLQ backlog during the demo, show the queue depths in RabbitMQ UI:
- RabbitMQ UI: http://localhost:15672 (rtos/rtos)

## 4) Troubleshooting (fast)

- Logs:
  - `docker compose logs -f order-service`
  - `docker compose logs -f reporting-service`
- Rabbit queues:
  - `dev.reporting.order-created` / `.dlq`
  - `dev.inventory.order-created` / `.retry` / `.dlq`
- Common fix: restart a single service
  ```bash
  cd deploy
  docker compose up -d --no-deps --force-recreate reporting-service
  ```

## 5) Teardown

```bash
cd deploy
docker compose down
```
