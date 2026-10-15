# RelayOps Architecture (Day 04)

## Components

| Component | Role |
| --- | --- |
| Spring Boot API | Auth, destination registry, ingestion, attempt inspection |
| PostgreSQL | Durable events, destinations, delivery attempts |
| Redis list queue | Async handoff from accept path to delivery worker |
| Scheduled worker | Poll queue, HTTP deliver, retry/backoff, terminal failure |
| Next.js web | Thin operator shell (health + docs links) |

## Request path

```text
Client --Bearer API key--> /v1/events
  validate destination ownership
  verify HMAC signature over payload JSON
  upsert by (api_client_id, idempotency_key)
  enqueue after commit --> Redis
Worker --> HTTP POST destination with X-RelayOps-Signature
  record delivery_attempts
  SUCCEEDED | FAILED_RETRYABLE | FAILED_TERMINAL
```

## Security notes

- API keys stored as SHA-256 hashes only.
- Destination signing secrets are stored server-side for outbound HMAC; treat as sensitive.
- Bootstrap key is a temporary onboarding gate for Day 04 demos; replace with stronger provisioning later.
