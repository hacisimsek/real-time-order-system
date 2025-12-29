# Presentation Preflight Notes (RTOS)

Use this checklist to prep the system before a demo. It is written so it can be shared and followed exactly.

## 0) Preconditions
- Docker Desktop is running.
- You are at repo root: `real-time-order-system/`.

## 1) Start/Reset Options

### Option A: Full reset (clean DB)
```bash
cd deploy
docker compose down -v
docker compose up -d
```

### Option B: Quick start (keep data)
```bash
cd deploy
docker compose up -d
```

## 2) Verify Services
```bash
cd deploy
docker compose ps
```

Health checks:
```bash
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health
```

Grafana + Prometheus:
```bash
curl -s http://localhost:3000/api/health
curl -s http://localhost:9090/-/ready
```

## 3) Get Dev Token
```bash
cd deploy
docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1
```

## 4) Seed Inventory (safe buffer)
```bash
TOKEN=$(docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1 | sed 's/.*Bearer //')
for sku in ABC-001 ABC-002 ABC-003 ABC-004 ABC-005 ABC-006 ABC-007 ABC-008 ABC-009 ABC-010; do
  curl -s -X PUT "http://localhost:8083/inventory/${sku}/adjust" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${TOKEN}" \
    -d '{"delta": 2000, "reason": "seed"}' > /dev/null
done
```

## 5) Seed Orders (optional for dashboards)
```bash
TOKEN=$(docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1 | sed 's/.*Bearer //')
for i in $(seq 1 50); do
  curl -s -X POST http://localhost:8081/orders \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${TOKEN}" \
    -d '{"customerId":"DEMO-INIT","amountCents":1999,"currency":"TRY","items":[{"sku":"ABC-001","qty":1}]}' \
    > /dev/null
done
curl -s "http://localhost:8084/reports/orders?period=DAILY&refresh=true" \
  -H "Authorization: Bearer ${TOKEN}" > /dev/null
```

## 6) Ensure No Queue Backlog
```bash
docker exec dev-rabbitmq rabbitmqctl list_queues -q name messages_ready messages_unacknowledged consumers
```

If any DLQ has messages and you want a clean demo:
```bash
docker exec dev-rabbitmq rabbitmqctl purge_queue -p / dev.reporting.order-created.dlq
docker exec dev-rabbitmq rabbitmqctl purge_queue -p / dev.inventory.order-created.dlq
```

## 7) Open UI Tabs
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- RabbitMQ UI: http://localhost:15672 (rtos/rtos)

## 8) Optional Live Load (30-60s, low rate)
Recommended for demo only:
```bash
TOKEN=$(docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1 | sed 's/.*Bearer //')
DEV_TOKEN="$TOKEN" SKU_POOL="ABC-001" ORDER_RATE=25 INVENTORY_RATE=10 REPORTING_RATE=5 DURATION=45s \
  k6 run docs/load/k6-runsmoke.js
```

## 9) Postman Note
Use `docs/postman/rtos.postman_collection.json` and `docs/postman/rtos-local.postman_environment.json`.  
The `jwt` value in the environment should be pasted **without** the `Bearer ` prefix.
