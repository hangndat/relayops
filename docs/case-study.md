# RelayOps Technical Case Study

**Product:** Webhook delivery reliability hub  
**Stack:** Spring Boot 4, PostgreSQL, Redis, Next.js  
**Sprint:** Days 04–05

## Problem

Accepting a webhook is easy; proving what happened next is hard. Teams need durable acceptance, signed delivery, retries with a budget, and an operator path when destinations fail.

## Architecture decisions

1. **Accept path ≠ delivery path** — Persist first, enqueue after commit, deliver asynchronously.
2. **At-least-once contract** — Documented explicitly; handlers must be idempotent.
3. **HMAC over payload JSON** — `X-RelayOps-Signature: sha256=…` for ingest and outbound delivery.
4. **DLQ = `FAILED_TERMINAL`** — Exhausted retry budget; operators replay after fixing the destination.
5. **API-key auth for Day 04/05** — Bootstrap key issues hashed client keys; stronger IdP deferred.

## What shipped

| Area | Evidence |
| --- | --- |
| Destination registry + secret rotate | `/v1/destinations`, `/rotate-secret` |
| Signed ingest + idempotency | `/v1/events` + unique `(client, idempotency_key)` |
| Async worker + backoff | Redis queue + scheduled `DeliveryWorker` |
| Ops UI | `/events`, `/dlq`, `/settings` |
| Failure/replay tests | `FailureReplayIntegrationTest` |
| CI | `.github/workflows/ci.yml` |

## Limitations

- Demo video not recorded in this session.
- API deployment is reproducible via Postgres/Redis + jar/`spring-boot:run`; hosted API URL may lag the console deploy.
- Signing secrets are stored server-side (needed for outbound HMAC); treat DB access as sensitive.
- Browser console stores API keys in `localStorage` for operator demos — not a production SSO design.

## Trade-offs

- Redis list queue is simple and observable locally; a broker with delayed jobs would refine backoff scheduling.
- Replay keeps historical attempt rows and continues attempt numbering to preserve audit uniqueness.
