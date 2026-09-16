# Toll-Free Number Provisioning API

A Java 17 / Spring Boot service for managing telecom customers and toll-free number provisioning. It exposes a REST API, enforces the number lifecycle, records audit actions, publishes JSON Kafka events, caches status reads in Redis, and sends email notifications via a Kafka consumer.

## Architecture

```text
Client -> JWT Security -> REST Controllers -> Services -> PostgreSQL
                                      |              -> Redis (number-status cache)
                                      +--------------> Kafka (events / DLT)
                                                            |
                                                 ProvisioningNotificationListener
                                                            |
                                                       Mailhog (SMTP)
```

## Stack

Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, Kafka, Redis, Spring Mail, Mailhog, Spring Security/JWT, springdoc OpenAPI, Maven, JUnit 5/Mockito, Docker Compose.

## Domain design

| Entity | Responsibility / key constraints |
|---|---|
| `Customer` | Customer record; unique email |
| `TelecomNumber` | Number primary key, status, optional assigned customer, optimistic version |
| `ProvisioningRequest` | Idempotency record; unique `(customer_id, number)` |
| `AuditLog` | Immutable status-change audit trail |

Indexes exist for customer email, number status/customer, and audit number. Provisioning and lifecycle writes lock the number row to prevent concurrent double allocation.

## Number lifecycle

Valid transitions:

```text
AVAILABLE ──► RESERVED ──► ACTIVE ──► SUSPENDED
                 │             ▲           │
                 │             └───────────┘
                 │
                 └──► AVAILABLE   (cancel reservation)

ACTIVE ──► RELEASED
```

| From | To | How |
|---|---|---|
| `AVAILABLE` | `RESERVED` | `POST /api/v1/provisioning` |
| `RESERVED` | `AVAILABLE` | `POST /api/v1/numbers/{number}/actions/AVAILABLE` |
| `RESERVED` | `ACTIVE` | `POST /api/v1/numbers/{number}/actions/ACTIVE` |
| `ACTIVE` | `SUSPENDED` | `POST /api/v1/numbers/{number}/actions/SUSPENDED` |
| `SUSPENDED` | `ACTIVE` | `POST /api/v1/numbers/{number}/actions/ACTIVE` |
| `ACTIVE` | `RELEASED` | `POST /api/v1/numbers/{number}/actions/RELEASED` |

`POST /api/v1/provisioning` reserves an available number for a customer and is idempotent for the `(customerId, number)` pair. The first request returns `201`; a repeat returns the stored request with `idempotent: true`.

When a number transitions back to `AVAILABLE` or is `RELEASED` the customer link is cleared (`customerId` becomes `null`).

## API

Swagger UI is available at `/swagger-ui/index.html`.

```bash
# Create a customer
curl -X POST localhost:8080/api/v1/customers -H 'Authorization: Bearer <jwt>' -H 'Content-Type: application/json' -d '{"name":"Acme","email":"acme@example.com"}'

# Import a number (ADMIN or OPERATOR)
curl -X POST localhost:8080/api/v1/numbers -H 'Authorization: Bearer <jwt>' -H 'Content-Type: application/json' -d '{"number":"8001234567"}'

# Provision it (triggers email notification)
curl -X POST localhost:8080/api/v1/provisioning -H 'Authorization: Bearer <jwt>' -H 'Content-Type: application/json' -d '{"customerId":"<uuid>","number":"8001234567"}'

# Activate a reserved number
curl -X POST localhost:8080/api/v1/numbers/8001234567/actions/ACTIVE -H 'Authorization: Bearer <jwt>'
```

All API routes require a JWT except Swagger and health. The JWT subject is the principal and its `role` claim must be `ADMIN`, `OPERATOR`, or `READ_ONLY`. `READ_ONLY` can retrieve and list; status-changing APIs require ADMIN/OPERATOR. Set a strong 32-byte-plus `JWT_SECRET` outside development.

### Generating a JWT for local testing

Use [jwt.io](https://jwt.io) with algorithm `HS256`, secret `change-this-to-a-32-byte-minimum-secret-key`, and payload:

```json
{ "sub": "me", "role": "ADMIN" }
```

## Kafka and failure strategy

Events are JSON records on `tollfree.number.events`: `NumberReserved`, `NumberActivated`, `NumberSuspended`, `NumberReleased`, and `ProvisioningFailed`. Consumer error handling retries three times with a one-second fixed backoff, then routes the failed record to `tollfree.number.events.DLT`. A provisioning validation/service failure emits `ProvisioningFailed` before returning a normal API error. In a larger deployment, use an outbox table for atomic DB/event delivery.

## Email notifications

A Kafka consumer (`ProvisioningNotificationListener`) listens on `tollfree.number.events` and sends an email to the customer whenever a `NumberReserved` event is received.

In local development, emails are captured by **Mailhog** — a fake SMTP server that never delivers to a real inbox. View intercepted emails at:

```
http://localhost:8025
```

To switch to real SMTP (e.g. Gmail) in production, override these environment variables:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_FROM=you@gmail.com
SPRING_MAIL_USERNAME=you@gmail.com
SPRING_MAIL_PASSWORD=<app-password>
```

And update `application.yml` to enable `starttls` and `auth`.

## Run locally

Prerequisites: Java 17, Maven 3.9+, and Docker Desktop.

```bash
mvn clean package
docker compose up --build
```

The application uses PostgreSQL at `postgres:5432`, Kafka at `kafka:9092`, Redis at `redis:6379`, and Mailhog at `mailhog:1025` in Compose. The database schema is initialized from `src/main/resources/schema.sql`.

| Service | Local URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Mailhog inbox | http://localhost:8025 |

## Test

```bash
mvn test
mvn verify
```

JaCoCo generates its report at `target/site/jacoco/index.html`. The existing unit tests cover reservation/event publication, missing/invalid lifecycle states, provisioning failure notification, and provisioning persistence. Add Testcontainers-based HTTP/Kafka/Redis integration tests in CI for an enforceable 80% coverage gate.

## Design notes

Controllers only handle HTTP/DTO concerns; services own business and transaction boundaries; repositories own persistence; messaging and security are isolated configuration/components. DTOs prevent JPA entities from leaking into the API. Status reads are cached for ten minutes and all status mutations evict the number key. `NumberView` implements `Serializable` to support Redis JDK serialization.
