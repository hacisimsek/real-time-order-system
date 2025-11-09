# Reporting Service – Requirements & Data Mapping

This document captures the KPIs, source systems, retention strategy, and aggregation cadence
for the reporting module (2025 Performance Target – Phase 5).

## 1. Key Stakeholder Questions

1. **How many orders did we process in a period?**
   - Metrics: total order count, revenue in cents/TRY.
   - Windows: daily, weekly (ISO week, Monday start), monthly (calendar month).
2. **Who are the top customers by revenue or volume?**
   - Ranking limited to top 5–20 customers with tie-breaking by revenue.
3. **Are report exports refreshed and available within SLA (< 2 min)?**
   - Track last refresh timestamp and latency via Micrometer metrics.
4. **How far behind is the consumer from the order event stream?**
   - Compare RabbitMQ timestamp (message header) with processing time.

## 2. Data Sources

| Source | Purpose | Access Path |
|--------|---------|-------------|
| `orders` table (order-service DB) | Raw transactional data for top-customer queries | Read via `NamedParameterJdbcTemplate` over JDBC (service account with read-only permissions) |
| `report_order_rollup_daily` | Incremental rollups maintained by `OrderRollupService` | JPA entity `DailyOrderRollup` |
| `report_snapshots` | Aggregated snapshots exposed to API | JPA entity `ReportSnapshot` |
| `report_message_log` | Idempotency log to dedupe Rabbit messages | JPA entity `MessageLog` |
| RabbitMQ `order.events` (queue `dev.reporting.order-created`) | Source of truth for order.created events | Listener `OrderEventsListener` |
| Micrometer metrics `reporting_*` | Operational KPIs for Grafana | Actuator `/actuator/prometheus` |

## 3. KPIs & Transformations

| KPI | Definition | Calculation |
|-----|------------|-------------|
| Total Orders | Count of order.created events per bucket | `rollup.total_orders` and `snapshot.totalOrders` |
| Total Revenue | Sum of `amountCents` per bucket | Stored in cents + decimal projection in DTOs |
| Average Order Value (AOV) | Total revenue / total orders | Derived in UI or downstream dashboard |
| Top Customers | Ranked customers by revenue (tie by amount) | SQL aggregate over `orders` table |
| Processing Latency | Event timestamp vs processed timestamp | `reporting_order_processing_latency` metric |
| Orders Processed | Counter for audit | `reporting_orders_processed_total` metric |

## 4. Retention & Windowing

- **Raw rollups**: 24 months retention (database policy). The scheduler refresh command truncates only the requested window, leaving historic data.
- **Snapshots**: The scheduled refresh (default cron `0 0 1 * * *`) rebuilds the trailing 12 months by default. Manual refresh allows forcing arbitrary windows.
- **Message log**: 30 days retention (enforced by DB cleanup task to be added later). Ensures dedupe entries do not grow without bound.
- **Top-customer queries**: Operate on raw `orders` data limited by request window (default trailing 7/12/12 units per period). Requires indexes added in `V3__reporting_indexes.sql`.

## 5. Aggregation Cadence

1. **Streaming updates** – `OrderEventsListener` increments daily rollups and snapshots in near-real time per message.
2. **Nightly reconciliation** – `ReportSnapshotScheduler` triggers `refresh(null, null)` to rebuild the last 12 months to catch stragglers or manual corrections.
3. **Manual refresh** – `/reports/orders/refresh` endpoint lets operators request ad-hoc rebuilds (defaults to 12 months, override via payload).

## 6. Security & Access

- JWT authorities
  - `ROLE_REPORTING_READ`: list/totals/top-customers endpoints.
  - `ROLE_REPORTING_EXPORT`: CSV export + refresh trigger.
- Database access uses the reporting-service Postgres credentials defined in `deploy/.env` (`POSTGRES_*`).
- RabbitMQ queue is provisioned by `AmqpConfig` using dev routing key `order.created.v1`.

## 7. Acceptance Checklist

- [x] KPIs documented (this file) and referenced from README/runbook.
- [x] Data sources and flows traceable from queue → rollup → snapshot → API → dashboard.
- [ ] Data retention policy automation (cleanup job) – **future**.
- [ ] SLA dashboard alert definitions – scope for observability phase.

Maintain this document when new KPIs or data sources are added to keep the execution plan requirements satisfied.
