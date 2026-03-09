# H_PROMPT_2 — Inventory Service: gRPC Server + Payment Kafka Consumer

## Context

The inventory-service requires two architectural changes:

1. **Add gRPC Server (`reserve` RPC)**: The order-service calls inventory-service synchronously via gRPC to
   reserve stock during order creation. Inventory must expose a `reserve` RPC that generates a `reservationId`,
   stores the reservation in-memory, and returns the `reservationId` to the caller.

2. **Replace Order Kafka Consumer with Payment Kafka Consumer**: The current consumer listens to the `order` topic
   and does nothing useful. It must be replaced with a consumer on the `payment` topic that confirms or releases
   reservations based on `APPROVED` / `REJECTED` payment outcomes.

The service remains **stateless** — no database. Reservations are tracked in an in-memory store (e.g.,
`ConcurrentHashMap`).

Refer to `AGENTS.md` for the full architecture, request flow, environment variables, and design patterns expected
in this codebase.

---

## Configuration & Environment Variables Policy

Environment variables defined in `docker-compose.yml` are automatically mapped by Spring Boot to their
corresponding properties (e.g., `SPRING_KAFKA_BOOTSTRAP_SERVERS` → `spring.kafka.bootstrap-servers`).
**Do not duplicate these in `application.yaml`.**

`application.yaml` must only contain:
- Application-level defaults that are not environment-specific (e.g., deserializer class names, group IDs)
- Configuration that has no corresponding environment variable in `docker-compose.yml`

When a step says "update `application.yaml`", add only the properties explicitly listed — nothing more.

---

## Dependency Order (Bottom-Up)

Files must be modified from the lowest dependency level to the highest. Each phase depends on all previous phases
being complete.

---

## Phase 1 — Build & Configuration (no project dependencies)

### Step 1 — UPDATE `pom.xml`

Add the gRPC server runtime dependencies required to expose a gRPC server from Spring Boot.

Add the following dependencies inside `<dependencies>`:

- `net.devh:grpc-server-spring-boot-starter` (version `3.1.0.RELEASE`) — integrates a gRPC server lifecycle into
  Spring Boot, enables `@GrpcService` annotation
- `io.grpc:grpc-stub` (version `1.68.1`) — required for generated stub base classes
- `io.grpc:grpc-protobuf` (version `1.68.1`) — bridges protobuf messages with gRPC transport

The `protobuf-maven-plugin` is already configured with `grpc-java` code generation. No changes needed to the
build plugin section.

---

### Step 2 — DELETE `src/main/proto/order_service.proto`

Remove this file entirely. The inventory-service will no longer consume from the `order` topic, so the
`OrderEvent` and `Money` message definitions it provides are no longer needed in this service.

---

### Step 3 — CREATE `src/main/proto/inventory_service.proto`

Create the proto file defining the gRPC contract that order-service (client) calls against inventory-service
(server).

It must define:

- Package: `inventory`
- Options: `java_package = "inventory"`, `java_multiple_files = true`
- A `Money` message with: `units` (int64, field 1), `nanos` (int32, field 2), `currency` (string, field 3)
- A `ReserveRequest` message with: `order_id` (string, field 1), `amount` (Money, field 2)
- A `ReserveResponse` message with: `reservation_id` (string, field 1)
- A service `InventoryService` with a single RPC: `Reserve(ReserveRequest) returns (ReserveResponse)`

This proto must match the `inventory_service.proto` already created on the order-service side (from H_PROMPT_1,
Step 2).

---

### Step 4 — UPDATE `src/main/resources/application.yaml`

Add only the gRPC server port, which has no corresponding env var in `docker-compose.yml` for this service.
The Kafka bootstrap-servers are already provided by `SPRING_KAFKA_BOOTSTRAP_SERVERS` (Docker env var) and must
**not** be added here.

Add:

```yaml
grpc:
  server:
    port: 9001
```

The existing Kafka consumer configuration (`key-deserializer`, `value-deserializer`) and `server.port: 4003`
remain unchanged. No other changes to this file.

---

## Phase 2 — In-Memory Reservation Store (no project Java dependencies)

### Step 5 — CREATE `src/main/java/com/mercora/inventoryservice/reservation/ReservationStore.java`

Create a Spring `@Component` that acts as the in-memory reservation registry.

It must:

- Internally use a `ConcurrentHashMap<String, String>` where key = `reservationId` (UUID string), value =
  `orderId` (string)
- Expose a method `String reserve(String orderId)` that:
    - Generates a new `UUID` as the `reservationId`
    - Stores the mapping `reservationId → orderId`
    - Returns the `reservationId` as a string
    - Logs the reservation creation including `orderId` and `reservationId`
- Expose a method `void confirm(String reservationId)` that:
    - Removes the entry from the map
    - Logs confirmation including `reservationId`
    - If the `reservationId` is not found, logs a warning and returns without throwing
- Expose a method `void release(String reservationId)` that:
    - Removes the entry from the map
    - Logs release including `reservationId`
    - If the `reservationId` is not found, logs a warning and returns without throwing

---

## Phase 3 — gRPC Server (depends on: inventory_service.proto → Step 3, ReservationStore → Step 5)

### Step 6 — CREATE `src/main/java/com/mercora/inventoryservice/grpc/InventoryGrpcService.java`

Create the gRPC server implementation annotated with `@GrpcService` (from `net.devh` starter).

It must:

- Extend `InventoryServiceGrpc.InventoryServiceImplBase` (generated from `inventory_service.proto`)
- Inject `ReservationStore` via constructor
- Override the `reserve(ReserveRequest request, StreamObserver<ReserveResponse> responseObserver)` method:
    - Extract `orderId` from `request.getOrderId()`
    - Call `reservationStore.reserve(orderId)` to obtain the `reservationId`
    - Build a `ReserveResponse` with the returned `reservationId`
    - Call `responseObserver.onNext(response)` then `responseObserver.onCompleted()`
    - Log the incoming reserve call (orderId) and the issued reservationId
    - Wrap in try/catch: on any exception, call `responseObserver.onError(Status.INTERNAL.withDescription(...).asRuntimeException())`

---

## Phase 4 — Kafka Consumer (depends on: payment_service.proto → exists, ReservationStore → Step 5)

### Step 7 — UPDATE `src/main/java/com/mercora/inventoryservice/kafka/KafkaConsumer.java`

Replace the entire existing implementation. The new consumer listens to the `payment` topic and drives reservation
lifecycle based on payment outcomes.

It must:

- Inject `ReservationStore` via constructor
- Replace the `@KafkaListener` topic from `"order"` to `"payment"`, keeping the same `groupId`
  (`"inventory-service-group"`)
- Deserialize the incoming `byte[]` payload as a `PaymentResponse` protobuf message (generated from
  `payment_service.proto`, class `payment.PaymentResponse`)
- Extract `paymentId`, `orderId`, and `status` from the deserialized event
- If `status == PaymentStatus.APPROVED`: call `reservationStore.confirm(reservationId)` — note: the
  `PaymentResponse` does not carry a `reservationId` directly; use `orderId` as the lookup key if needed, or
  document this limitation and confirm by `orderId` by iterating the store to find the matching `reservationId`
- If `status == PaymentStatus.REJECTED`: call `reservationStore.release(reservationId)` using the same lookup
  strategy
- Log the received event including `orderId`, `paymentId`, and `status`
- On `InvalidProtocolBufferException`: log the error and return without rethrowing

> **Note on reservationId lookup**: `PaymentResponse` contains `order_id` but not `reservation_id`. Since
> `ReservationStore` maps `reservationId → orderId`, a reverse lookup is needed (find key by value). Either add a
> reverse map to `ReservationStore` or add a `findByOrderId(String orderId): Optional<String>` method that scans
> the map. Whichever approach is chosen, keep it consistent with the `ReservationStore` implementation in Step 5.

---

## Summary of Changes

| Step | Action | File                                                              |
|------|--------|-------------------------------------------------------------------|
| 1    | UPDATE | `pom.xml`                                                         |
| 2    | DELETE | `src/main/proto/order_service.proto`                              |
| 3    | CREATE | `src/main/proto/inventory_service.proto`                          |
| 4    | UPDATE | `src/main/resources/application.yaml`                             |
| 5    | CREATE | `src/main/java/.../reservation/ReservationStore.java`             |
| 6    | CREATE | `src/main/java/.../grpc/InventoryGrpcService.java`                |
| 7    | UPDATE | `src/main/java/.../kafka/KafkaConsumer.java`                      |
