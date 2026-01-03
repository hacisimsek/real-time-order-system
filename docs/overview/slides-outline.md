# RTOS Slides Outline (20 min, demo-first)

Use this as copy/paste text for PowerPoint or Google Slides. Keep each slide to ~3–5 bullets.

## Slide 1 — Title
- **Real-Time Order System (RTOS)**  
- Demo-first microservices reference project (learning/presentation scope)
- Stack: Spring Boot + Postgres + RabbitMQ + Redis + Prometheus/Grafana

**Visual:** (optional) repo screenshot / simple icon row (DB + MQ + cache + charts)

## Slide 2 — Problem & Goal
- Goal: process orders **in real time** and keep **reporting** up to date
- Design constraints: reliability, observability, and simple operations
- Scope: demo/learning (not production go-live)

**Visual:** 1-sentence “order → event → consumers → reporting”

## Slide 3 — Architecture Overview
- Services: Order (publisher), Inventory (reserve/consume), Notification (mock), Reporting (rollups/exports)
- Infra: Postgres + RabbitMQ + Redis + Prometheus + Grafana
- Communication: REST for commands, events for downstream processing

**Visual:** screenshot of the mermaid diagram from `docs/setup/running.md` or `docs/overview/project-deep-dive.md`

## Slide 4 — End-to-End Flow
- POST `/orders` → store in DB
- Outbox relay publishes `order.created.v1` to RabbitMQ
- Inventory/Reporting consume events (eventual consistency)
- Users query reporting endpoints / dashboards

**Visual:** screenshot of sequence diagram from `docs/setup/runtime-technical.md`

## Slide 5 — Reliability Patterns
- **Outbox**: persists events in DB, publish async (reduces “lost event” risk)
- **Manual ACK**: consumer controls success/failure; failures go to DLQ
- **Idempotency**: safe re-delivery / duplicate messages
- **DLQ replay**: operational tool to reprocess failed messages

**Visual:** RabbitMQ UI “Queues” screenshot (main + retry + dlq queues)

## Slide 6 — Security (JWT + RBAC)
- All endpoints require JWT except `/actuator/**`
- RBAC roles: Order/Inventory/Reporting authorities
- Dev token (1h) printed by order-service for demos

**Visual:** small table of roles (e.g., `ROLE_ORDER_WRITE`, `ROLE_REPORTING_READ`)

## Slide 7 — Reporting
- Event consumer builds rollups/snapshots (DAILY/WEEKLY/MONTHLY)
- APIs: list, totals, top customers, CSV export
- On-demand refresh for demos

**Visual:** example JSON response screenshot (Postman “Totals”)

## Slide 8 — Caching Strategy (Redis vs Caffeine)
- Reporting totals/top-customers are cached
- Provider switch: `APP_REPORTING_CACHE_PROVIDER=redis|caffeine`
- Demo: first call vs second call latency

**Visual:** 2 numbers: “totals #1” vs “totals #2” (or a tiny chart)

## Slide 9 — Observability
- Prometheus scrapes `/actuator/prometheus`
- Grafana dashboards show throughput/latency/staleness/backlog
- Alert rules: service-down, high latency, staleness, queue backlog

**Visual:** Grafana “Reporting Overview” screenshot + 1 PromQL screenshot

Suggested PromQL:
- `rate(reporting_orders_processed_total[5m])`
- `time() - reporting_last_order_timestamp_seconds`
- `rabbitmq_queue_messages_ready{queue="dev.reporting.order-created"}`

## Slide 10 — Operations (Day-2)
- DLQ replay endpoints:
  - Inventory: `POST /ops/replay/dev.inventory.order-created.dlq?max=50`
  - Reporting: `POST /ops/replay/dev.reporting.order-created.dlq?max=50`
- Runbook & troubleshooting are documented

**Visual:** Postman “Ops (DLQ Replay)” request screenshot

## Slide 11 — Live Demo Plan (6–8 min)
- Start stack: `cd deploy && docker compose up -d --build`
- Token: `docker compose logs order-service | grep "DEV ADMIN TOKEN"`
- Postman run order:
  1) Health (all `UP`)
  2) Inventory seed
  3) Create order → auto-sets `order_id`
  4) Fulfil order
  5) Reporting list/totals/top-customers/export
  6) Grafana panel check
  7) (Optional) Ops replay

**Visual:** checklist screenshot (this slide is the checklist)

## Slide 12 — Lessons Learned + Next (Optional)
- What worked: event-driven split, reliable publish, operability, metrics-first
- What was tricky: token expiry, cache serialization, DLQ behavior
- Optional next: K8s manifests, automated load tests, WAF hardening

**Visual:** simple “next steps” list

---

## Timing Suggestion (20 min)
- 0–2: Slide 1–2
- 2–6: Slide 3–4
- 6–10: Slide 5–6
- 10–12: Slide 7–8
- 12–14: Slide 9–10
- 14–20: Slide 11 demo + Slide 12 wrap-up
