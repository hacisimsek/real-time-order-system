Real-Time Order System (RTOS)

Mikroservis tabanlı Gerçek Zamanlı Sipariş ve Envanter Yönetimi örneği.
Servisler: Order, Inventory, Notification.
Mesajlaşma: RabbitMQ. Veri tabanı: PostgreSQL. Gözlemlenebilirlik: Micrometer → Prometheus → Grafana.

Hedef: İlk 2 haftada MVP akışı (Order → Event → Inventory/Notification), Docker ile lokal çalıştırma, temel metrikler, DLQ/Retry ve Outbox ile güvenilir yayın.

İçindekiler

Mimari

Dizin Yapısı

Ön Koşullar

Hızlı Başlangıç (Docker Compose)

Servisler & Portlar

API Uçları (cURL Örnekleri)

Mesajlaşma Topolojisi

Outbox (Exactly-once Publish)

Observability (Prometheus & Grafana)

DLQ Yönetimi & Replay

Konfigürasyon / Ortam Değişkenleri

Build/Run (Opsiyonel: Makefile)

Troubleshooting

Lisans

Mimari

Order Service (8081): Sipariş CRUD + durum değişimi. Event’ler Outbox tablosuna yazılır, arka planda RabbitMQ’ya yayınlanır.

Inventory Service (8083): order.created / order.status-changed event’lerini tüketir. Stok reserve/release yapar, DLQ/Retry destekli.

Notification Service (8082): Order event’lerini tüketir (mock e-posta/SMS).

Infra: PostgreSQL, RabbitMQ (management UI), Redis (ileride rate-limit veya cache için), Prometheus, Grafana.

Akış (MVP):

POST /orders → Order Created (DB) → Outbox → RabbitMQ (order.events)
→ Notification (consume & log)
→ Inventory (reserve/release)

Dizin Yapısı
real-time-order-system/
├─ backend/
│  ├─ order-service/
│  │  ├─ src/...
│  │  ├─ pom.xml
│  │  ├─ Dockerfile
│  │  └─ .dockerignore
│  ├─ inventory-service/
│  │  ├─ src/...
│  │  ├─ pom.xml
│  │  ├─ Dockerfile
│  │  └─ .dockerignore
│  └─ notification-service/
│     ├─ src/...
│     ├─ pom.xml
│     ├─ Dockerfile
│     └─ .dockerignore
├─ deploy/
│  ├─ docker-compose.yml
│  └─ observability/
│     ├─ prometheus.yml
│     ├─ grafana-datasource.yml
│     ├─ grafana-dashboards.yml
│     └─ rtos-dashboard.json
└─ README.md

Ön Koşullar

Java 17 (Oracle/OpenJDK)

Maven ≥ 3.9.9

Docker & Docker Compose v2

(Opsiyonel) jq (CLI JSON güzelleştirici)

Doğrulama:

java -version
mvn -v
docker -v && docker compose version

Hızlı Başlangıç (Docker Compose)

İlk çalıştırmada sadece infra + servisler yeterli.

cd deploy
docker compose up -d postgres redis rabbitmq
docker compose build order-service notification-service inventory-service
docker compose up -d order-service notification-service inventory-service


Sağlık kontrolleri:

curl -s localhost:8081/actuator/health
curl -s localhost:8082/actuator/health
curl -s localhost:8083/actuator/health


RabbitMQ UI: http://localhost:15672
(guest/guest)
Prometheus: http://localhost:9090/targets

Grafana: http://localhost:3000
(admin/admin)

Compose version: uyarısı görürsen dosyadaki version: satırını sil.

Servisler & Portlar
Servis	Port	Açıklama
Order	8081	Sipariş API + Outbox
Notification	8082	Event consumer (mock)
Inventory	8083	Stok görüntüleme/rezervasyon
PostgreSQL	5432	appdb (user/pass: app)
RabbitMQ	5672 / 15672	AMQP / Management UI
Prometheus	9090	Metrics
Grafana	3000	Dashboard
API Uçları (cURL Örnekleri)
Inventory seed (başlangıç stoğu)
curl -s -X PUT localhost:8083/inventory/ABC-001/adjust \
-H 'Content-Type: application/json' -d '{ "delta": 20, "reason": "seed" }' | jq
curl -s -X PUT localhost:8083/inventory/XYZ-123/adjust \
-H 'Content-Type: application/json' -d '{ "delta": 10, "reason": "seed" }' | jq

Sipariş oluştur
curl -s localhost:8081/orders -H 'Content-Type: application/json' -d '{
"customerId":"C-9001","amountCents":1999,"currency":"TRY",
"items":[{"sku":"ABC-001","qty":1}]
}' | jq

Sipariş durumu değiştir (CONFIRMED / FULFILLED / CANCELED)
curl -s -X PATCH localhost:8081/orders/64/status \
-H 'Content-Type: application/json' -d '{ "status": "FULFILLED" }' | jq

Stok görüntüle
curl -s localhost:8083/inventory/ABC-001 | jq


Postman collection & environment dosyaları (opsiyonel) deploy/ ya da doc/ altında tutulabilir.

Mesajlaşma Topolojisi

Exchange: order.events (topic)

Routing keys:

order.created.v1

order.status-changed.v1

Queue’lar:

Inventory: dev.inventory.order-created, dev.inventory.order-status-changed

Notification: dev.notifications.order-created, dev.notifications.order-status-changed

Retry / DLQ:

<queue>.retry (TTL → exchange’e geri döner)

<queue>.dlq (dead-letter queue)

Outbox (Exactly-once Publish)

Order Service event’leri önce outbox_events tablosuna yazar, arka plandaki publisher batch olarak RabbitMQ’ya gönderir.
Kısa şema:

CREATE TABLE outbox_events(
id bigserial primary key,
aggregate_id bigint not null,
type varchar(64) not null,
payload jsonb not null,
status varchar(16) not null default 'PENDING',
attempts int not null default 0,
created_at timestamptz not null default now(),
last_error text
);
CREATE UNIQUE INDEX ux_outbox_agg_type ON outbox_events(aggregate_id, type);


Test:

# (opsiyonel) RabbitMQ'yu kapat → outbox PENDING kalır
cd deploy && docker compose stop rabbitmq

curl -s localhost:8081/orders -H 'Content-Type: application/json' -d '{
"customerId":"C-OBX","amountCents":2500,"currency":"TRY",
"items":[{"sku":"ABC-001","qty":1}]
}' | jq

docker exec -it dev-postgres psql -U app -d appdb -c \
"SELECT status, count(*) FROM outbox_events GROUP BY status;"

# RabbitMQ'yu aç → publisher PUBLISHED yapar
docker compose start rabbitmq
sleep 3
docker exec -it dev-postgres psql -U app -d appdb -c \
"SELECT status, count(*) FROM outbox_events GROUP BY status;"

Observability (Prometheus & Grafana)

Micrometer metrikleri:

HTTP: http_server_requests_seconds_*

JVM: jvm_*

İş metrikleri:

Order publish: order_events_published_total, order_events_publish_failed_total

Inventory consumer: inventory_messages_processed_total, ..._retried_total, ..._dlq_total

Prometheus & Grafana Compose ile açılır (opsiyonel).

Dashboard: deploy/observability/rtos-dashboard.json
Başlıca paneller:

Publish OK/FAIL (Order)

Inventory Processed/Retry/DLQ

HTTP p95 (Inventory/Notification)

JVM memory & threads

HTTP p95 panellerinde actuator istekleri filtrelidir: uri!~"/actuator/.*"

DLQ Yönetimi & Replay

Hatalı SKU gibi durumlarda mesajlar DLQ’ya düşer. Inventory servisinde ops endpoint ile DLQ → retry taşıma yapılabilir:

# Hatalı siparişler gönder
for i in {1..3}; do
curl -s localhost:8081/orders -H 'Content-Type: application/json' -d "{
\"customerId\":\"C-ERR\",\"amountCents\":1999,\"currency\":\"TRY\",
\"items\":[{\"sku\":\"NO-SUCH-$i\",\"qty\":1}]
}" >/dev/null
done

# DLQ sayısı kontrol
docker exec -it dev-rabbitmq rabbitmqadmin list queues name messages

# Replay (DLQ → retry)
curl -s -X POST "http://localhost:8083/ops/replay/dev.inventory.order-created.dlq?max=100" | jq


Retry kuyruklarında TTL süresi dolunca mesajlar tekrar işlenmek üzere ana exchange’e döner.

Konfigürasyon / Ortam Değişkenleri

Tüm servisler application.yml içinde environment override destekler:

server:
port: ${PORT:808x}

spring:
datasource:
url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:appdb}
username: ${DB_USER:app}
password: ${DB_PASS:app}
rabbitmq:
host: ${RABBIT_HOST:localhost}
port: ${RABBIT_PORT:5672}
username: ${RABBIT_USER:guest}
password: ${RABBIT_PASS:guest}

management:
endpoints.web.exposure.include: "health,info,prometheus"


Flyway (Inventory): İlk kurulumda boş olmayan şemada tablo yok hatası için:

spring:
flyway:
baseline-on-migrate: true
baseline-version: 0
table: flyway_schema_history_inventory

Build/Run (Opsiyonel: Makefile)

İstersen köke aşağıdaki Makefile ile kısayol komutları ekleyebilirsin:

SHELL := /bin/sh
.RECIPEPREFIX := >
DC := $(shell command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1 && echo "docker compose" || echo "docker-compose")

.PHONY: infra up rebuild down ps logs tail

infra:
> cd deploy && $(DC) up -d postgres redis rabbitmq

up:
> cd deploy && $(DC) up -d order-service notification-service inventory-service

rebuild:
> cd deploy && $(DC) build order-service notification-service inventory-service

down:
> cd deploy && $(DC) down

ps:
> cd deploy && $(DC) ps

logs:
> cd deploy && $(DC) logs --no-color --timestamps --tail=200

tail:
> cd deploy && $(DC) logs -f


Kullanım:

make infra
make rebuild
make up


Make istemezsen doğrudan docker compose ... komutlarını kullanman yeterli.

Troubleshooting

version is obsolete (Compose uyarısı)
deploy/docker-compose.yml içindeki version: satırını kaldır.

Servis UP ama DB hatası
depends_on: { condition: service_healthy } var; yine de ilk seferde postgres hazır olmadan bağlanmaya çalıştıysan tüm servisleri restart et:

cd deploy && docker compose up -d --force-recreate order-service inventory-service notification-service


Flyway: “Found non-empty schema but no schema history table”
Inventory’de baseline-on-migrate: true kullan (yukarıdaki örnek).

Grafana’da “No data”
PromQL’lere or vector(0) ekledik. Yine boşsa ilgili servise gerçek trafik üret (ör. /orders, /inventory/...).

RabbitMQ DLQ temizleme

docker exec -it dev-rabbitmq rabbitmqadmin purge queue name=dev.inventory.order-created.dlq

Lisans

Eğitim ve demo amaçlı örnek proje. Kurumsal kullanımlar için güvenlik, kimlik doğrulama, şema yönetimi ve HA/DR gereksinimleri ayrıca ele alınmalıdır.


Not: README’deki cURL’ler lokal Docker Compose kurulumunu hedefler. Kubernetes/CI/CD, Outbox temizliği (arşivleme), Gateway & Rate limiting gibi ileri adımları bir sonraki sprintte ekleyebiliriz.