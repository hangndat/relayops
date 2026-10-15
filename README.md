# RelayOps

Webhook and API delivery reliability hub. Day 04 delivers the foundation: authenticated destination registration, signed event ingestion, idempotency, Redis-backed async delivery with retry/backoff, and integration tests.

## Stack

- Java 21 / Spring Boot 4 / Flyway / PostgreSQL / Redis
- Next.js operator console (thin Day 04 shell)
- GitHub Actions CI

## Delivery semantics

RelayOps provides **at-least-once** delivery:

1. Accept and persist the inbound event (unique per API client + idempotency key).
2. Enqueue after commit.
3. Deliver with HMAC signature headers and a bounded HTTP timeout.
4. Retry with exponential backoff until `max-attempts`, then mark `FAILED_TERMINAL`.

Exactly-once delivery is **not** claimed.

## Quick start

### Prerequisites

- Java 21, Maven Wrapper (`api/mvnw`)
- PostgreSQL 16 (`createdb relayops`)
- Redis (`redis-server`)
- Node.js 22+ (for `web/`)

### API

```bash
cd api
export RELAYOPS_DB_URL=jdbc:postgresql://127.0.0.1:5432/relayops
export RELAYOPS_DB_USER=$USER
export RELAYOPS_BOOTSTRAP_API_KEY=dev-bootstrap-key-change-me
./mvnw spring-boot:run
```

### Bootstrap + happy path

```bash
# 1) Create an API client
curl -s localhost:8080/v1/bootstrap \
  -H 'content-type: application/json' \
  -d '{"name":"demo","bootstrapKey":"dev-bootstrap-key-change-me"}'

# 2) Register a destination (use returned apiKey as Bearer)
# 3) POST /v1/events with X-RelayOps-Signature: sha256=<hmac of payload JSON>
# 4) GET /v1/events/{id} to inspect status + attempts
```

### Web console

```bash
cd web
cp .env.example .env.local
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

### Tests

```bash
cd api
./mvnw test
```

## Documentation

- [Architecture](docs/architecture.md)
- [Delivery semantics](docs/delivery-semantics.md)
- [Day 04 review](docs/day-04-review.md)

## Status

Day 04 foundation complete locally. Day 05 will add operations UI (history/replay/DLQ views), richer auth, deploy, and demo recording.
