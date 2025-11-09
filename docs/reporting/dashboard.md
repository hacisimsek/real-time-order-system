# Reporting Dashboard Coverage

Grafana dashboard: `deploy/observability/dashboards/reporting-overview.json`  
Datasource: Prometheus (`uid: prometheus_ds`) pulling Micrometer metrics exposed by reporting-service.

## Template Variables (Reusable Filters)

| Variable | Description | Default |
|----------|-------------|---------|
| `$instance` | Filters all panels by Prometheus `instance` label (e.g., `reporting-service:8084`). Multi-select and “All” supported. | `All` |
| `$source` | Filters throughput panel by the `source` label (Rabbit exchange). | `order.events` |

Operators can slice the dashboard per replica or per routing source without cloning panels.

## Panels & Drill-down Links

| Panel | KPI | Link | Role |
|-------|-----|------|------|
| **Order Events Throughput** (`rate(reporting_orders_processed_total)`) | Orders/min over 5m | `/reports/orders?period=DAILY` | BizOps & SRE |
| **Processing Latency** (`histogram_quantile` on latency buckets) | p90/p99 lag between event timestamp and processing completion | `/reports/orders/totals?period=DAILY` | SRE |
| **Revenue Rate** (`rate(reporting_order_amount_cents_sum)/100`) | Revenue per minute (TRY) | `/reports/orders/export?period=DAILY` | Finance / BizOps |
| **Staleness Gauge** (`time() - reporting_last_order_timestamp_seconds`) | Seconds since last message processed | `/actuator/metrics/reporting_last_order_timestamp_seconds` | On-call |

Each panel includes a Grafana link that opens the relevant HTTP endpoint in a new tab for drill-down. This satisfies the execution plan’s requirement for guidance from visualization to secured APIs.

## Usage Notes

1. Import the dashboard via Grafana provisioning (already wired in `deploy/docker-compose.yml`).
2. Time range defaults to last 6 hours. Use Grafana’s time picker for longer periods.
3. For CSV/JSON exports, click the panel link then reuse the developer JWT (`ROLE_REPORTING_EXPORT`).
4. Alerting can be layered on top (e.g., staleness > 300 seconds) once Prometheus alert rules are added.

Update this file whenever new panels or variables are introduced so operators know how to interpret the widgets.
