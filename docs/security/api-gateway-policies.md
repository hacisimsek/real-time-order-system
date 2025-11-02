# API Gateway Security Checklist

The Real-Time Order System expects a zero-trust edge. When deploying behind an API gateway (e.g., Kong, Apigee, NGINX, AWS API Gateway), enforce the following controls:

- **TLS everywhere** – Terminate HTTPS at the gateway with strong ciphers (TLS 1.2+), then re-encrypt to upstream services. Automate certificate renewal (ACME/LetsEncrypt or ACM).
- **JWT verification at the edge** – Validate signature, issuer (`rtos`), and audience (`rtos-clients`). Reject expired tokens and propagate the validated claims to downstream services via trusted headers (e.g., `X-RTOS-Subject`, `X-RTOS-Roles`).
- **Rate limiting & spike arrest** – Apply per-client and global limits. Suggested defaults:
  - Order API: 120 requests/minute per client
  - Inventory API: 240 requests/minute per client
  - Burst tolerance of 2x sustained rate for ≤ 10s
- **Request validation** – Enforce JSON size limits (max 256 KB), block unknown HTTP methods, and ensure `Content-Type: application/json` for mutating requests.
- **Header sanitation** – Strip authentication headers from external callers and re-inject only after validation; drop hop-by-hop headers.
- **Threat protection** – Enable anomaly detection (SQLi/XSS signatures) and bot protection where available. Log denied requests with correlation IDs.
- **Observability** – Emit structured logs (method, path, status, client-id, duration) and export metrics for latency, error rate, throttle counts.
- **DoS resilience** – Configure circuit breakers and connection limits to protect upstream services from slowloris or connection floods.

Document these gateway policies with the platform team and store them alongside infrastructure-as-code to ensure reproducibility.
