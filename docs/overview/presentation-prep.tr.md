# Sunum Oncesi Hazirlik Notlari (RTOS)

Bu checklist, sunumdan once sistemi hazirlamak icin kullanilir. Paylasilabilir ve birebir uygulanabilir formatta yazilmistir.

## 0) On Kosullar
- Docker Desktop calisiyor olmali.
- Repo kok dizinindesin: `real-time-order-system/`.

## 1) Baslatma/Sifirlama Secenekleri

### Secenek A: Tam sifirlama (temiz DB)
```bash
cd deploy
docker compose down -v
docker compose up -d
```

### Secenek B: Hizli baslat (veriyi koru)
```bash
cd deploy
docker compose up -d
```

## 2) Servisleri Dogrula
```bash
cd deploy
docker compose ps
```

Saglik kontrolleri:
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

## 3) Dev Token Al
```bash
cd deploy
docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1
```

## 4) Envanter Seed (guvenli buffer)
```bash
TOKEN=$(docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1 | sed 's/.*Bearer //')
for sku in ABC-001 ABC-002 ABC-003 ABC-004 ABC-005 ABC-006 ABC-007 ABC-008 ABC-009 ABC-010; do
  curl -s -X PUT "http://localhost:8083/inventory/${sku}/adjust" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${TOKEN}" \
    -d '{"delta": 2000, "reason": "seed"}' > /dev/null
done
```

## 5) Order Seed (dashboard icin opsiyonel)
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

## 6) Queue Backlog Kontrolu
```bash
docker exec dev-rabbitmq rabbitmqctl list_queues -q name messages_ready messages_unacknowledged consumers
```

Temiz bir demo icin DLQ'lar bosaltilmak istenirse:
```bash
docker exec dev-rabbitmq rabbitmqctl purge_queue -p / dev.reporting.order-created.dlq
docker exec dev-rabbitmq rabbitmqctl purge_queue -p / dev.inventory.order-created.dlq
```

## 7) UI Tablarini Ac
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- RabbitMQ UI: http://localhost:15672 (rtos/rtos)

## 8) Opsiyonel Canli Yuk (30-60 sn, dusuk rate)
Demo icin onerilen:
```bash
TOKEN=$(docker compose logs order-service | grep "DEV ADMIN TOKEN" | tail -n 1 | sed 's/.*Bearer //')
DEV_TOKEN="$TOKEN" SKU_POOL="ABC-001" ORDER_RATE=25 INVENTORY_RATE=10 REPORTING_RATE=5 DURATION=45s \
  k6 run docs/load/k6-runsmoke.js
```

## 9) Postman Notu
`docs/postman/rtos.postman_collection.json` ve `docs/postman/rtos-local.postman_environment.json` kullanilir.  
Environment icindeki `jwt` degeri **Bearer olmadan** yapistirilir.
