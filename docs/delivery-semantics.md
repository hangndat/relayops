# Delivery Semantics

## Contract

- **At-least-once** delivery to destination URLs.
- Duplicate ingest requests with the same idempotency key return the existing event.
- Downstream handlers should be idempotent.

## States

| Status | Meaning |
| --- | --- |
| `RECEIVED` | Accepted (transient; immediately becomes queued) |
| `QUEUED` | Waiting for worker / next attempt time |
| `DELIVERING` | HTTP attempt in flight |
| `SUCCEEDED` | Destination returned 2xx |
| `FAILED_RETRYABLE` | Transient failure; next attempt scheduled |
| `FAILED_TERMINAL` | Exhausted `max-attempts` |

## Retry policy

- Exponential backoff from `initial-backoff-ms`, capped by `max-backoff-ms`.
- HTTP timeout bounded by `http-timeout-ms`.
- Non-2xx and transport errors are retryable until the attempt budget is spent.
