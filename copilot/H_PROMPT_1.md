# H_PROMPT_1 — Order Service Refactor: Inventory gRPC + Payment Kafka Consumer

## Context

The order-service requires two architectural changes:

1. **Replace Payment gRPC with Inventory gRPC**: Instead of synchronously calling payment-service via gRPC to
   initiate payment, order-service must call inventory-service via gRPC to reserve stock and receive a
   `reservationId`. Payment is triggered asynchronously by payment-service after it consumes the `ORDER_CREATED`
   Kafka event.

2. **Consume Payment Kafka Events**: Order-service must listen to the `payment` Kafka topic and update the order
   status to `PAID` (on `PAYMENT_PROCESSED`) or `FAILED` (on `PAYMENT_FAILED`).

Refer to `AGENTS.md` for the full architecture, request flow, environment variables, and design patterns expected
in this codebase.

---

## Configuration & Environment Variables Policy

Environment variables defined in `docker-compose.yml` are automatically mapped by Spring Boot to their
corresponding properties (e.g., `SPRING_KAFKA_BOOTSTRAP_SERVERS` → `spring.kafka.bootstrap-servers`,
`INVENTORY_SERVICE_ADDRESS` → `inventory.service.address`). **Do not duplicate these in `application.yaml`.**

`application.yaml` must only contain:
- Application-level defaults that are not environment-specific (e.g., deserializer class names, group IDs,
  serialization formats)
- Configuration that has no corresponding environment variable in `docker-compose.yml`

When a step says "update `application.yaml`", add only the properties explicitly listed — nothing more.

---

## Dependency Order (Bottom-Up)

Files must be modified from the lowest dependency level to the highest. Each phase depends on all previous phases
being complete.

---

## Phase 1 — Proto Files (no project dependencies)

### Step 1 — DELETE `src/main/proto/payment_service.proto`

Remove this file entirely. The order-service will no longer call payment-service via gRPC. Deleting this proto
removes the generated `PaymentService` stub classes that `PaymentServiceGrpcClient` currently depends on.

---

### Step 2 — CREATE `src/main/proto/inventory_service.proto`

Create a new proto file defining the gRPC contract between order-service (client) and inventory-service (server).

It must define:

- A `ReserveRequest` message containing: `order_id` (string) and `amount` (Money message with `units`, `nanos`,
  `currency`)
- A `ReserveResponse` message containing: `reservation_id` (string)
- An `InventoryService` service with a single `Reserve` RPC: `Reserve(ReserveRequest) returns (ReserveResponse)`

Use `option java_package = "inventory"` and `option java_multiple_files = true`. Package should be `inventory`.

The `Money` message in this proto should follow the same structure as in `order_service.proto`: `units` (int64),
`nanos` (int32), `currency` (string).

---

### Step 3 — UPDATE `src/main/proto/order_service.proto`

The `OrderEvent` Kafka message must carry the fields that downstream services (payment-service) need to process the
order.

Add to the `OrderEvent` message:

- `string payment_method = 5;`
- `string reservation_id = 6;`

The existing fields (`order_id`, `amount`, `status`, `event_type`) must remain unchanged with their current field
numbers.

---

## Phase 2 — Domain Model (no project Java dependencies)

### Step 4 — UPDATE `src/main/java/com/mercora/orderservice/model/Order.java`

Add the `reservationId` field to the `Order` JPA entity.

- Type: `UUID`
- Nullable (no `@Column(nullable = false)`)
- Should follow the same pattern as the existing `paymentId` field

---

## Phase 3 — DTO Layer (no project Java dependencies)

### Step 5 — UPDATE `src/main/java/com/mercora/orderservice/dto/OrderResponseDTO.java`

Add `reservationId` (String, nullable) to the response DTO following the same pattern as `paymentId`.

---

## Phase 4 — gRPC Client (depends on: inventory_service.proto → Step 2)

### Step 6 — DELETE `src/main/java/com/mercora/orderservice/grpc/PaymentServiceGrpcClient.java`

Remove this file entirely. It references the now-deleted `payment_service.proto` generated stubs and implements
logic that no longer belongs in order-service.

---

### Step 7 — CREATE `src/main/java/com/mercora/orderservice/grpc/InventoryServiceGrpcClient.java`

Create a new Spring `@Component` gRPC client that calls inventory-service.

It must:

- Read host from `${inventory.service.address:localhost}` and port from `${inventory.service.grpc.port:9001}`
- Build a `ManagedChannel` using plaintext (no TLS), matching the pattern from the deleted
  `PaymentServiceGrpcClient`
- Expose a method `reserveStock(String orderId, BigDecimal amount, String currency): String` that:
    - Converts `BigDecimal` to `units` and `nanos` for the `Money` message
    - Builds and sends a `ReserveRequest` via the blocking stub
    - Returns the `reservationId` string from `ReserveResponse`
    - Logs the request (orderId) and the received `reservationId`

---

## Phase 5 — Mapper (depends on: Order.java → Step 4, OrderResponseDTO.java → Step 5)

### Step 8 — UPDATE `src/main/java/com/mercora/orderservice/mapper/OrderMapper.java`

Update both mapper methods:

- `toDTO(Order order)`: map `order.getReservationId()` to `reservationId` in `OrderResponseDTO` (null-safe, same
  pattern as `paymentId`)
- `toModel(OrderRequestDTO dto)`: no change needed here since `reservationId` is set after the gRPC call, not from
  the request DTO

---

## Phase 6 — Kafka Infrastructure (depends on: order_service.proto → Step 3, Order.java → Step 4)

### Step 9 — UPDATE `src/main/resources/application.yaml`

Add only the Kafka consumer properties that are **not** supplied by environment variables. The bootstrap servers
are already provided by `SPRING_KAFKA_BOOTSTRAP_SERVERS` (Docker env var) and must **not** be added here.

Add under `spring.kafka`:

```yaml
consumer:
  key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
  value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
  group-id: order-service-group
  auto-offset-reset: earliest
```

Do not add `spring.kafka.bootstrap-servers` — Spring maps it automatically from the `SPRING_KAFKA_BOOTSTRAP_SERVERS`
env var. Do not add `inventory.service.address` or `inventory.service.grpc.port` — those are injected via
`INVENTORY_SERVICE_ADDRESS` and `INVENTORY_SERVICE_GRPC_PORT` env vars respectively. No other changes to this
file.

---

### Step 10 — UPDATE `src/main/java/com/mercora/orderservice/kafka/KafkaConfig.java`

Add a Kafka consumer factory alongside the existing producer factory.

Add:

- `ConsumerFactory<String, byte[]> consumerFactory()` bean — reads `spring.kafka.bootstrap-servers`,
  `spring.kafka.consumer.key-deserializer`, `spring.kafka.consumer.value-deserializer`, and
  `spring.kafka.consumer.group-id`
- `ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(ConsumerFactory)bean `
  required for `@KafkaListener` to work with byte[] payloads

---

### Step 11 — UPDATE `src/main/java/com/mercora/orderservice/kafka/KafkaProducer.java`

Update `sendEvent(Order order)` to include the two new fields in the `OrderEvent` protobuf message:

- Set `payment_method` from `order.getPaymentMethod()`
- Set `reservation_id` from `order.getReservationId()` (convert UUID to string, null-safe)

All other fields and behavior remain unchanged.

---

## Phase 7 — Kafka Consumer (depends on: KafkaConfig → Step 10, Order.java → Step 4, OrderRepository)

### Step 12 — CREATE `src/main/java/com/mercora/orderservice/kafka/KafkaConsumer.java`

Create a new Spring `@Component` Kafka consumer that listens to the `payment` topic.

It must:

- Use `@KafkaListener(topics = "payment")` on a method that receives `byte[]`
- Deserialize the payload using a `PaymentEvent` protobuf message (note: the payment-service publishes to the
  `payment` topic using protobuf — the proto definition for that event should be referenced from the
  payment-service proto or a shared contract; check `backend/payment-service/src/main/proto/` for the event message
  definition)
- Extract `orderId` and `eventType` (or status) from the event
- Load the order from `OrderRepository` by `orderId`
- If `eventType` is `PAYMENT_PROCESSED`: set order status to `PAID`
- If `eventType` is `PAYMENT_FAILED`: set order status to `FAILED`
- Save the updated order via `OrderRepository`
- Log the transition including `orderId` and new status (include `correlationId` if available per `AGENTS.md`
  logging standards)
- Handle the case where the order is not found (log a warning, do not throw)

---

## Phase 8 — Service Layer (depends on: InventoryServiceGrpcClient → Step 7, KafkaProducer → Step 11, OrderMapper → Step 8, OrderRepository)

### Step 13 — UPDATE `src/main/java/com/mercora/orderservice/service/OrderService.java`

Replace the payment gRPC flow with the inventory gRPC flow, and remove the direct payment initiation logic.

Update `createOrder(OrderRequestDTO request)` to:

1. Map request DTO to Order entity (status = `CREATED`) — unchanged
2. Call `inventoryServiceGrpcClient.reserveStock(orderId, amount, currency)` to get `reservationId`
3. Set `order.setReservationId(UUID.fromString(reservationId))`
4. Set `order.setStatus(OrderStatus.PAYMENT_PENDING)`
5. Persist the order to the database with `orderRepository.save(order)`
6. Publish the `ORDER_CREATED` event to Kafka via `kafkaProducer.sendEvent(savedOrder)`
7. Return `OrderMapper.toDTO(savedOrder)`

Remove all references to `PaymentServiceGrpcClient`. Remove the `paymentId` assignment during creation (it is now
set by the Kafka consumer when payment completes). Replace the `@Autowired` / constructor injection of
`PaymentServiceGrpcClient` with `InventoryServiceGrpcClient`.

The `getOrders()` method remains unchanged.

---

## Summary of Changes

| Step | Action | File                                                     |
|------|--------|----------------------------------------------------------|
| 1    | DELETE | `src/main/proto/payment_service.proto`                   |
| 2    | CREATE | `src/main/proto/inventory_service.proto`                 |
| 3    | UPDATE | `src/main/proto/order_service.proto`                     |
| 4    | UPDATE | `src/main/java/.../model/Order.java`                     |
| 5    | UPDATE | `src/main/java/.../dto/OrderResponseDTO.java`            |
| 6    | DELETE | `src/main/java/.../grpc/PaymentServiceGrpcClient.java`   |
| 7    | CREATE | `src/main/java/.../grpc/InventoryServiceGrpcClient.java` |
| 8    | UPDATE | `src/main/java/.../mapper/OrderMapper.java`              |
| 9    | UPDATE | `src/main/resources/application.yaml`                    |
| 10   | UPDATE | `src/main/java/.../kafka/KafkaConfig.java`               |
| 11   | UPDATE | `src/main/java/.../kafka/KafkaProducer.java`             |
| 12   | CREATE | `src/main/java/.../kafka/KafkaConsumer.java`             |
| 13   | UPDATE | `src/main/java/.../service/OrderService.java`            |
