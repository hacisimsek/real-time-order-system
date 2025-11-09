# Reporting Service Runbook

This runbook covers the day-two operations for the reporting service. It focuses on the
new performance hardening work (caching, query tuning, and validations) and explains how
to fall back safely when the service is degraded.

## 1. Performance Validation Checklist

- **Cache-enabled endpoints**  
  - `GET /reports/orders/totals` and `GET /reports/orders/top-customers` are cached.
  - Cache manager: `Caffeine` via `CacheConfig` (`backend/reporting-service/.../CacheConfig.java`).
  - Cache names: `reportTotals`, `reportTopCustomers` (`ReportCacheNames` constants).
  - Defaults: TTL 60s, max 500 entries (override via `APP_REPORTING_CACHE_TTL` / `APP_REPORTING_CACHE_MAX_SIZE`).
- **Database indexes**  
  - `orders` table now has `idx_orders_created_at` and `idx_orders_created_customer`
    (`backend/order-service/.../V3__reporting_indexes.sql`) to accelerate high-cardinality
    top-customer queries.
- **Automated verification**  
  - Run `mvn -pl reporting-service test -Dtest=ReportServiceCachingTest`
    to ensure repeated calls hit the cache and that the limit parameter affects keys.

## 2. Cache Operations & Overrides

| Action | Steps |
|--------|-------|
| Inspect caches | `curl -s http://localhost:8084/actuator/caches | jq` |
| Flush caches | `curl -X DELETE http://localhost:8084/actuator/caches/reportTotals`<br>`curl -X DELETE http://localhost:8084/actuator/caches/reportTopCustomers` |
| Change TTL | Set `APP_REPORTING_CACHE_TTL=30s` (or ISO-8601 duration) in `.env`, rebuild/restart. |
| Change max size | Set `APP_REPORTING_CACHE_MAX_SIZE=1000`, rebuild/restart. |

> Manual flushes are rarely needed because TTL is short, but flushing is useful after
> backfills or when reconciling historic data.

## 3. Snapshot Refresh & Fallback Procedures

1. **Trigger on-demand rebuild**
   ```bash
   curl -X POST http://localhost:8084/reports/orders/refresh \
     -H 'Authorization: Bearer <DEV_TOKEN>'
   ```
   - This recomputes snapshots for the last 12 months and evicts all caches.

2. **Rabbit backlog / DLQ handling**
   - Primary queue: `dev.reporting.order-created`
   - DLQ: `dev.reporting.order-created.dlq`
   - If DLQ grows, inspect with RabbitMQ UI (http://localhost:15672) and either
     requeue (move messages back to main queue) or export for offline replay.

3. **Hot standby instructions**
   - Scale replicas via Docker Compose: `docker compose up -d --scale reporting-service=2`.
   - Confirm both replicas registered with Prometheus and metrics `reporting_orders_processed_total`
     continue to increase.

4. **Data reconciliation**
   - If Postgres maintenance rewrites the `orders` table, rerun Flyway migrations
     (`backend/order-service/src/main/resources/db/migration/V3__reporting_indexes.sql`)
     to guarantee the reporting indexes exist.
   - After any manual data fixes, trigger a refresh (step 1) and flush caches (section 2).

## 4. Load / Performance Test Outline

1. Seed realistic data in `report_order_rollup_daily` (see `OrderRollupServiceTest` fixture).
2. Run `ReportServiceTest` for functional coverage, followed by
   `ReportServiceCachingTest` for cache validation and `ReportControllerTest`
   for full-stack authorization/perf smoke.
3. Monitor Prometheus (`reporting_order_processing_latency`) while executing
   the smoke script in README step 8. Target p95 < 250ms for totals/top-customers.

## 5. Operational Quick Reference

- **Health:** `GET /actuator/health`
- **Metrics:** `GET /actuator/prometheus` (filter `reporting_*`)
- **Logs:** `docker logs reporting-service | tail -n 200`
- **Dashboard:** `deploy/observability/dashboards/reporting-overview.json`

Keep this file updated when new operational tasks are introduced (e.g., new cache
groups or queue bindings) to ensure the reporting module stays production-ready.
