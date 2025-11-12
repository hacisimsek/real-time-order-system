# Manual Test Scenario: Happy Path Fulfilment

This scenario exercises the secured RTOS stack end to end, covering JWT-protected endpoints, order publishing, and downstream inventory processing.

## 1. Start Required Services
```bash
cd deploy
docker compose up -d postgres redis rabbitmq
# Build & start app containers (picks up latest jars)
docker compose up -d --build order-service inventory-service notification-service
```

## 2. Retrieve Developer Token
```bash
docker compose logs order-service | grep "DEV ADMIN TOKEN"
```
Copy the `Bearer …` token (valid for 1 hour). Use it as `<DEV_TOKEN>` below.

### Optional: Use the Postman Collection
- Import `docs/testing/postman/rtos.postman_collection.json` plus the `docs/testing/postman/rtos-local.postman_environment.json` environment.
- In Postman, select the **RTOS Local** environment, paste the raw token (without `Bearer `) into the `dev_token` variable, and run the requests in order (Health → Inventory → Orders → Reporting).
- The collection mirrors the curl steps below, so you can iterate quickly without retyping headers. The generated dev token now includes reporting roles, so the same token works for `/reports/**` calls.

## 3. Seed Inventory
```bash
curl -s -X PUT http://localhost:8083/inventory/ABC-001/adjust \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <DEV_TOKEN>' \
  -d '{"delta": 25, "reason": "manual seed"}'
```
Expected: `availableQty: 25`, `reservedQty: 0`.

## 4. Create an Order
```bash
curl -s -X POST http://localhost:8081/orders \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <DEV_TOKEN>' \
  -d '{
        "customerId":"C-5010",
        "amountCents":4999,
        "currency":"TRY",
        "items":[{"sku":"ABC-001","qty":3}]
      }'
```
Expected: 200 OK with an order payload (`id`, `status":"CREATED"`). Record the returned `id` as `<ORDER_ID>`.

## 5. Verify Reservation in Inventory
```bash
curl -s http://localhost:8083/inventory/ABC-001 \
  -H 'Authorization: Bearer <DEV_TOKEN>'
```
Expected: `availableQty` reduced by 3; `reservedQty` increased by 3.

## 6. Fulfil the Order
```bash
curl -s -X PATCH http://localhost:8081/orders/<ORDER_ID>/status \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <DEV_TOKEN>' \
  -d '{"status":"FULFILLED"}'
```
Expected: order response with `status":"FULFILLED"`.

## 7. Confirm Inventory Consumption
```bash
curl -s http://localhost:8083/inventory/ABC-001 \
  -H 'Authorization: Bearer <DEV_TOKEN>'
```
Expected: `reservedQty` returns to 0; `availableQty` reflects (initial stock − 3).

## 8. Verify Reporting Snapshots
```bash
# Refresh & list current snapshots (auto-refresh will also keep these warm)
curl -s "http://localhost:8084/reports/orders?period=DAILY&refresh=true" \
  -H 'Authorization: Bearer <DEV_TOKEN>' | jq

# Aggregate totals for the resolved window
curl -s http://localhost:8084/reports/orders/totals \
  -H 'Authorization: Bearer <DEV_TOKEN>' | jq

# Top customers leaderboard
curl -s "http://localhost:8084/reports/orders/top-customers?limit=5" \
  -H 'Authorization: Bearer <DEV_TOKEN>' | jq

# CSV export (saved to /tmp for convenience)
curl -s -o /tmp/order-reports.csv \
  -H 'Authorization: Bearer <DEV_TOKEN>' \
  http://localhost:8084/reports/orders/export
```
Expected:
- `/reports/orders` returns at least one snapshot that includes the order created above (look at `snapshotDate` / `totalOrders`).
- `/reports/orders/totals` and `/top-customers` aggregate the same numbers.
- The export response headers contain `Content-Disposition: attachment; filename="order-reports-...csv"` and `/tmp/order-reports.csv` opens as a valid CSV.

> Troubleshooting: Reporting now logs its JWT wiring (`SecurityDiagnosticsRunner`) at startup and dumps Spring Security filter debug output when `logging.level.org.springframework.security=DEBUG`. If a token is rejected, look for `FilterChainProxy` lines in `docker compose logs reporting-service --tail=200` to confirm which authority check failed.

## 9. Sanity-Check Actuator Endpoints
```bash
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health
```
Health endpoints remain public to simplify monitoring.

## 10. Teardown
```bash
cd deploy
docker compose down
```

> TIP: If you rerun the scenario, reseed inventory to a known number before creating new orders.
