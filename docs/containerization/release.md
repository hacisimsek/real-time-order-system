# Container Release & Deployment Playbook

This guide explains how to build, tag, push, and roll back the service images
without touching the upcoming Kubernetes work. It complements `baseline.md`.

## 1. Pre-flight Checklist

- Docker logged in to the target registry (e.g., `docker login ghcr.io/<org>`).
- `.env` contains the desired `SPRING_PROFILES_ACTIVE`, secrets, and port mapping.
- Prometheus/Grafana stack running locally (`docker compose up -d prometheus grafana`)
  for quick smoke validation after redeploy.

## 2. Build & Tag

```bash
cd deploy
export IMAGE_TAG=dev-$(git rev-parse --short HEAD)
docker compose build order-service notification-service inventory-service reporting-service

# Tag for registry (example: ghcr.io/hacisimsek/rtos-*)
for svc in order notification inventory reporting; do
  docker tag rtos/${svc}-service:dev ghcr.io/hacisimsek/${svc}-service:${IMAGE_TAG}
done
```

## 3. Push

```bash
for svc in order notification inventory reporting; do
  docker push ghcr.io/hacisimsek/${svc}-service:${IMAGE_TAG}
done
```

Record the exact tag in the release notes.

## 4. Deploy / Roll Forward (Compose)

```bash
cd deploy
IMAGE_TAG=dev-abcdef docker compose up -d order-service notification-service inventory-service reporting-service
docker compose ps
```

To switch tags without rebuilding:

```bash
docker compose pull
docker compose up -d --no-deps order-service
```

## 5. Rollback

1. Identify the previous known-good tag (`git log --oneline -n 5` or release notes).
2. Re-tag locally:
   ```bash
   docker pull ghcr.io/hacisimsek/order-service:<good-tag>
   docker tag ghcr.io/hacisimsek/order-service:<good-tag> ghcr.io/hacisimsek/order-service:${IMAGE_TAG}
   ```
3. `docker compose up -d --no-deps order-service` to redeploy the rolled-back image.

## 6. Post-deploy Validation

- `docker compose ps` should show health `healthy`.
- Hit each actuator health endpoint (`curl -s localhost:808x/actuator/health`).
- Check Grafana dashboard (reporting overview) and Prometheus alert page.
- Tail logs: `docker compose logs -f reporting-service`.

## 7. Artifact Retention

Store pushed image tags + git SHAs in release notes (e.g., `docs/releases/<date>.md`) once you introduce the formal release process.
