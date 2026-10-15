# Day 04 Review — RelayOps Foundation

**Date:** 2026-10-15  
**Scope:** Reliability core (destinations, signed ingest, idempotency, async worker, retries, CI)

## Acceptance checklist

| Criterion | Status | Notes |
| --- | --- | --- |
| Independent repository | Done | `/Users/admin/SideProject/relayops` |
| API contract + Flyway migration | Done | `V1__init.sql` |
| Authenticated destination registration | Done | Bearer API key + `/v1/destinations` |
| Signed webhook ingestion | Done | `X-RelayOps-Signature` HMAC-SHA256 |
| Event persistence + attempts | Done | `inbound_events`, `delivery_attempts` |
| Idempotency keys | Done | Unique `(api_client_id, idempotency_key)` |
| Async worker + Redis queue | Done | After-commit enqueue |
| Retry/backoff + timeouts | Done | Configurable policy |
| Integration test harness | Done | Idempotency + delivery + bad signature |
| CI workflow | Done | `.github/workflows/ci.yml` with Postgres/Redis services |
| Thin Next.js shell | Done | `web/` |
| Deploy / demo video | Out of scope | Day 05 |

## Verification

```bash
cd api && ./mvnw test
# unit + context + delivery integration — passed locally (2026-10-15)
```

## Follow-ups (Day 05)

- Operations UI for event/attempt inspection and replay
- DLQ / terminal failure console
- Deployment + demo recording + case study
