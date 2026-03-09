# Mercora – Living Architecture Document

## 1. Architecture Overview

Mercora is an event-driven e-commerce platform built on microservices.

**Core components:**

- **API Gateway** – single entrypoint, authentication validation
- **Auth Service** – login and token validation
- **Order Service** – order creation and management
- **Payment Service** – payment processing
- **Inventory Service** – stock reservation and confirmation
- **Kafka** – async inter-service communication

**Request flow:**

```
Client → Gateway → Order Service
                        │
                        ├─ gRPC ──→ Inventory Service (reserve stock → reservationId)
                        │
                        ↓ persists order with reservationId
                        │
                        ↓ Kafka: ORDER_CREATED
                        │
                   Payment Service (consumes ORDER_CREATED)
                        │
                        ↓ Kafka: PAYMENT_PROCESSED / PAYMENT_FAILED
                        │
                   Inventory Service (confirms or releases reservation)
```

**Communication:**

- External: REST (via Gateway)
- Internal async: Kafka
- Internal sync: gRPC (`order-service` → `inventory-service`)
- Persistence: PostgreSQL (one database per service, inventory-service is stateless)

---

## 2. Technology Stack

| Layer          | Technologies                                                 |
|----------------|--------------------------------------------------------------|
| Backend        | Java 21, Spring Boot, Spring Web, Spring Kafka, gRPC, Maven  |
| Infrastructure | Docker, Kafka, PostgreSQL, LocalStack                        |
| Architecture   | Event-driven, Microservices, API Gateway, Stateless services |
| Observability  | Structured JSON logs, Correlation ID                         |

---

## 3. Environment Variables

Variables are sourced from `docker-compose.yml`.

### Auth Service

| Variable                        | Value                                                    |
|---------------------------------|----------------------------------------------------------|
| `JWT_SECRET`                    | `8hf9KAJEeATIvtf5E2wzeZsoM9zaMr3O1aQTbCPngV6`            |
| `SPRING_DATASOURCE_URL`         | `jdbc:postgresql://auth-service-db:5432/auth-service-db` |
| `SPRING_DATASOURCE_USERNAME`    | `admin`                                                  |
| `SPRING_DATASOURCE_PASSWORD`    | `adminpass`                                              |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update`                                                 |
| `SPRING_SQL_INIT_MODE`          | `always`                                                 |

### Order Service

| Variable                         | Value                                                      |
|----------------------------------|------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`          | `jdbc:postgresql://order-service-db:5432/order-service-db` |
| `SPRING_DATASOURCE_USERNAME`     | `admin`                                                    |
| `SPRING_DATASOURCE_PASSWORD`     | `adminpass`                                                |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`  | `update`                                                   |
| `SPRING_SQL_INIT_MODE`           | `always`                                                   |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092`                                               |
| `INVENTORY_SERVICE_ADDRESS`      | `inventory-service`                                        |
| `INVENTORY_SERVICE_GRPC_PORT`    | `9001`                                                     |

### Payment Service

| Variable                         | Value                                                          |
|----------------------------------|----------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`          | `jdbc:postgresql://payment-service-db:5432/payment-service-db` |
| `SPRING_DATASOURCE_USERNAME`     | `admin`                                                        |
| `SPRING_DATASOURCE_PASSWORD`     | `adminpass`                                                    |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`  | `update`                                                       |
| `SPRING_SQL_INIT_MODE`           | `always`                                                       |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092`                                                   |

### Inventory Service

| Variable                         | Value        |
|----------------------------------|--------------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` |

### Gateway

| Variable           | Value                      |
|--------------------|----------------------------|
| `AUTH_SERVICE_URL` | `http://auth-service:4004` |

---

## 4. Directory Structure

```
mercora/
├── backend/
│   ├── gateway/
│   ├── auth-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── inventory-service/
│   ├── integration-tests/
│   ├── api-requests/
│   └── grpc-requests/
├── infrastructure/       # LocalStack / AWS CDK
└── docker-compose.yml
```

---

## 5. Services

### Auth Service

Handles authentication.

**Endpoints:**

- `POST /auth/login`
- `POST /auth/validate`

**Model: `User`**

| Field      | Type     | Constraints        |
|------------|----------|--------------------|
| `id`       | `UUID`   | PK, auto-generated |
| `email`    | `String` | unique, not null   |
| `password` | `String` | not null           |
| `role`     | `String` | not null           |

---

### Order Service

Handles order lifecycle.

**Endpoints:**

- `POST /orders`
- `GET /orders`

**gRPC:** calls `inventory-service` to reserve stock before persisting the order. Receives `reservationId` in
response.

**Model: `Order`**

| Field           | Type            | Constraints                    |
|-----------------|-----------------|--------------------------------|
| `id`            | `UUID`          | PK, auto-generated             |
| `customerId`    | `String`        | not null                       |
| `totalAmount`   | `BigDecimal`    | not null, precision 19 scale 4 |
| `currency`      | `String`        | not null, 3 chars              |
| `paymentMethod` | `String`        | not null                       |
| `status`        | `OrderStatus`   | not null                       |
| `paymentId`     | `UUID`          | nullable                       |
| `reservationId` | `UUID`          | nullable, set after gRPC call  |
| `createdAt`     | `LocalDateTime` | not null, immutable            |

**Enum: `OrderStatus`** — `CREATED`, `PAYMENT_PENDING`, `PAID`, `FAILED`

**Produces:** `ORDER_CREATED` (topic: `order`, Protobuf `byte[]`)

**Event: `ORDER_CREATED`**

| Field           | Type                                              | Description                       |
|-----------------|---------------------------------------------------|-----------------------------------|
| `orderId`       | `string`                                          | Order identifier                  |
| `amount`        | `Money` (`units`: int64, `nanos`: int32, `currency`: string) | Order total          |
| `paymentMethod` | `string`                                          | Payment method                    |
| `correlationId` | `string`                                          | Distributed tracing identifier    |

**Consumes:** `PAYMENT_PROCESSED`, `PAYMENT_FAILED` (topic: `payment`, Protobuf `byte[]`)

---

### Payment Service

Handles payment processing. Communicates with order-service via Kafka only — no gRPC.

**Consumer group:** `payment-service-group`

**Consumes:** `ORDER_CREATED` (topic: `order`, Protobuf `byte[]`)

**Event: `ORDER_CREATED`** (minimal fields consumed)

| Field           | Type                                                          | Description                    |
|-----------------|---------------------------------------------------------------|--------------------------------|
| `orderId`       | `string`                                                      | Order identifier               |
| `amount`        | `Money` (`units`: int64, `nanos`: int32, `currency`: string)  | Order total                    |
| `paymentMethod` | `string`                                                      | Payment method                 |
| `correlationId` | `string`                                                      | Distributed tracing identifier |

**Produces:** `PAYMENT_PROCESSED` or `PAYMENT_FAILED` (topic: `payment`, Protobuf `byte[]`)

**Event: `PAYMENT_PROCESSED` / `PAYMENT_FAILED`** (minimal fields produced)

| Field           | Type                                 | Description                    |
|-----------------|--------------------------------------|--------------------------------|
| `paymentId`     | `string`                             | Payment identifier             |
| `orderId`       | `string`                             | Related order identifier       |
| `status`        | `PaymentStatus` (APPROVED/REJECTED)  | Payment result                 |
| `correlationId` | `string`                             | Distributed tracing identifier |

**Processing logic:**

- On receiving `ORDER_CREATED`, the service creates a `Payment` record with status `PENDING`.
- If `amount == 10.00`, the payment is `REJECTED` and a `PAYMENT_FAILED` event is published.
- Otherwise, the payment is `APPROVED` and a `PAYMENT_PROCESSED` event is published.
- `correlationId` is read from the incoming event body and propagated to the outgoing event body.

**Idempotency strategy:**

- The `orderId` is used as the unique identifier for each payment request.
- Before processing, the service checks whether a `Payment` record for that `orderId` already exists in the database.
- If it does, processing is skipped and the previous result is logged.

**Model: `Payment`**

| Field           | Type            | Constraints                    |
|-----------------|-----------------|--------------------------------|
| `id`            | `UUID`          | PK, auto-generated             |
| `orderId`       | `String`        | not null, unique               |
| `amount`        | `BigDecimal`    | not null, precision 19 scale 4 |
| `currency`      | `String`        | not null, 3 chars              |
| `paymentMethod` | `String`        | not null                       |
| `status`        | `PaymentStatus` | not null                       |
| `createdAt`     | `LocalDateTime` | not null, immutable            |

**Enum: `PaymentStatus`** — `PENDING`, `APPROVED`, `REJECTED`

---

### Inventory Service

Handles stock reservation and confirmation. Stateless — no database.

**gRPC server:** exposes a `reserve` RPC called by `order-service` during order creation. Returns `reservationId`.

**Consumes:** `PAYMENT_PROCESSED` (topic: `payment`) → confirm reservation, update stock
**Consumes:** `PAYMENT_FAILED` (topic: `payment`) → release reservation

---

## 6. Common Hurdles

### Kafka consumer not receiving events

**Symptom:** Consumer is connected but no messages arrive.

**Cause:** `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` set to a value incompatible with a single-node cluster.

**Fix:** Set `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`

---

## 7. Design Patterns

- Event-Driven Architecture
- API Gateway
- Saga (choreography)
- Retry Pattern
- Idempotency
- Database per Service
- Correlation ID
- Producer/Consumer
- DTO Pattern
- Repository Pattern

---

## 8. Weekly Pipeline

| Day       | Activity                              |
|-----------|---------------------------------------|
| Monday    | Feature planning, architecture review |
| Tuesday   | Feature implementation                |
| Wednesday | Integration tests                     |
| Thursday  | Refactoring, performance checks       |
| Friday    | Documentation update, demo            |

---

## 9. Post-Implementation Checklist

Before merging:

- [ ] All tests passing
- [ ] Kafka events documented
- [ ] Logs include `correlationId`
- [ ] Endpoints documented
- [ ] Environment variables documented
- [ ] Retry logic validated
- [ ] Idempotency verified
- [ ] Docker build works
- [ ] Local environment reproducible

