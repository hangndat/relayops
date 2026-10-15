# Day 05 Review — RelayOps Operations + Release

**Date:** 2026-10-15

## Acceptance checklist

| Criterion | Status | Notes |
| --- | --- | --- |
| Delivery history + attempt inspection | Done | `/v1/events`, `/v1/events/{id}`, ops UI |
| Replay with authorization | Done | `/v1/events/{id}/replay` (API key scoped) |
| Terminal failure / DLQ | Done | `/v1/dlq` + `/dlq` UI |
| Webhook secret handling | Done | create + `/rotate-secret` |
| Logs/metrics | Done | structured delivery logs + actuator |
| Failure-path + duplicate tests | Done | replay/DLQ + idempotent ingest |
| Ops UI | Done | Next.js console |
| Case study + operating docs | Done | `docs/case-study.md`, README |
| Hosted API deploy | Partial | reproducible local/docker; console can deploy independently |
| Demo video | Outstanding | |

## Verification

```bash
cd api && ./mvnw test   # 6 tests, 0 failures
cd web && npm run lint && npm run build
```
