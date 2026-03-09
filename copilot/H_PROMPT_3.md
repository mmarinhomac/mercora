# H_PROMPT_3 — Payment Service: gRPC Removal + Kafka Event-Driven Architecture

## Context

The payment-service currently implements a synchronous gRPC server (`ProcessPayment` RPC) that creates a Payment
record with `PENDING` status and returns immediately. This must be replaced with a fully event-driven Kafka
architecture.

The service must:

1. **Remove all gRPC code and dependencies** — payment-service no longer exposes a gRPC endpoint.
2. **Consume `ORDER_CREATED` events** from the `order` Kafka topic, process the payment (approve or reject),
   and persist the result.
3. **Publish `PAYMENT_PROCESSED` or `PAYMENT_FAILED` events** to the `payment` Kafka topic with the payment
   outcome.
4. **Enforce idempotency** by checking whether a payment for the given `orderId` already exists in the database
   before processing.

Refer to `AGENTS.md` for the full architecture, event schemas, environment variables, and design patterns
expected in this codebase.

---

## Configuration & Environment Variables Policy

Environment variables defined in `docker-compose.yml` are automatically mapped by Spring Boot to their
corresponding properties (e.g., `SPRING_KAFKA_BOOTSTRAP_SERVERS` → `spring.kafka.bootstrap-servers`).
**Do not duplicate these in `application.yaml`.**

`application.yaml` must only contain:
- Application-level defaults that are not environment-specific (e.g., deserializer class names, group IDs,
  serialization formats)
- Configuration that has no corresponding environment variable in `docker-compose.yml`

When a step says "update `application.yaml`", add only the properties explicitly listed — nothing more.

---

## Dependency Order (Bottom-Up)

Files must be modified from the lowest dependency level to the highest. Each phase depends on all previous
phases being complete.

---

## Phase 1 — Build, Proto & Configuration (no project dependencies)

### Step 1 — UPDATE `pom.xml`

Remove all gRPC-related content and add Spring Kafka. Proto compilation is retained (for Kafka event
serialization) but simplified to message-only generation.

**Remove from `<properties>`:**
- `<grpc.version>1.75.0</grpc.version>`

**Remove the entire `<dependencyManagement>` block** (it only contains the `grpc-bom` import).

**Remove from `<dependencies>`:**
- `io.grpc:grpc-netty-shaded`
- `io.grpc:grpc-protobuf`
- `io.grpc:grpc-stub`
- `org.apache.tomcat:annotations-api`
- `net.devh:grpc-spring-boot-starter`

**Add to `<dependencies>`:**

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Update the `protobuf-maven-plugin` configuration** inside `<build><plugins>`: remove `<pluginId>`,
`<pluginArtifact>`, and the `compile-custom` goal. The proto files no longer contain gRPC service definitions,
so only the `compile` goal is needed. The `os-maven-plugin` extension must remain.

Replace the entire `protobuf-maven-plugin` block with:

```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:3.25.5:exe:${os.detected.classifier}</protocArtifact>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

`protobuf-java`, `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `postgresql`, and all test
dependencies remain unchanged.

---

### Step 2 — UPDATE `src/main/proto/payment_service.proto`

Remove the gRPC contract and unused messages. Add `correlationId` to the outgoing payment event.

- Remove the `service PaymentService` block entirely
- Remove the `PaymentRequest` message entirely
- Remove the `Money` message (no longer used in this proto after gRPC removal)
- Add `string correlation_id = 5;` to `PaymentResponse`

The final file must be exactly:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "payment";

package payment;

enum PaymentStatus {
  PENDING = 0;
  APPROVED = 1;
  REJECTED = 2;
}

message PaymentResponse {
  string payment_id = 1;
  string order_id = 2;
  PaymentStatus status = 3;
  string message = 4;
  string correlation_id = 5;
}
```

---

### Step 3 — CREATE `src/main/proto/order_service.proto`

Create the proto defining the `ORDER_CREATED` event that payment-service consumes from the `order` Kafka topic.
Field numbers must be compatible with the order-service's `order_service.proto`.

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "order.events";

package order.events;

message Money {
  int64 units = 1;
  int32 nanos = 2;
  string currency = 3;
}

message OrderEvent {
  string order_id = 1;
  Money amount = 2;
  string status = 3;
  string event_type = 4;
  string payment_method = 5;
  string reservation_id = 6;
  string correlation_id = 7;
}
```

> **Note:** Fields 1–6 mirror the order-service's `order_service.proto` as updated in H_PROMPT_1. Field 7
> (`correlation_id`) is required by payment-service for tracing propagation. For payment-service to receive a
> non-empty `correlationId`, the order-service must also set this field when publishing `ORDER_CREATED` events
> (a separate update to order-service's `KafkaProducer`, outside the scope of this prompt). Until that update
> is applied, `correlationId` will arrive as an empty string and must be handled gracefully.

---

### Step 4 — UPDATE `src/main/resources/application.yaml`

Remove the gRPC server configuration and add Kafka consumer and producer properties.

**Remove:**
```yaml
grpc:
  server:
    port: 9001
```

**Add under `spring`:**
```yaml
kafka:
  consumer:
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
    group-id: payment-service-group
    auto-offset-reset: earliest
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
```

Do not add `spring.kafka.bootstrap-servers` — Spring Boot maps it automatically from the
`SPRING_KAFKA_BOOTSTRAP_SERVERS` env var. `spring.application.name` and `server.port` remain unchanged.
No other changes to this file.

---

## Phase 2 — Domain Model (no project Java dependencies)

### Step 5 — UPDATE `src/main/java/com/mercora/paymentservice/model/Payment.java`

Add a `unique` constraint to the `orderId` column to enforce idempotency at the database level.

Change:
```java
@Column(nullable = false)
private String orderId;
```

To:
```java
@Column(nullable = false, unique = true)
private String orderId;
```

No other changes to this class.

---

## Phase 3 — Repository (depends on: Payment.java → Step 5)

### Step 6 — UPDATE `src/main/java/com/mercora/paymentservice/repository/PaymentRepository.java`

Add a derived query method used for the idempotency check before processing each incoming event.

Add:
```java
Optional<Payment> findByOrderId(String orderId);
```

Ensure `java.util.Optional` is imported. No other changes.

---

## Phase 4 — Mapper (depends on: order_service.proto → Step 3)

### Step 7 — UPDATE `src/main/java/com/mercora/paymentservice/mapper/MoneyMapper.java`

The mapper is now only used to parse the `amount` from incoming `OrderEvent` Kafka messages.
`order.events.Money` (from `order_service.proto`) replaces `payment.Money` as the input type.

- Replace the import `payment.Money` with `order.events.Money`
- Replace the `toBigDecimal(Money money)` method signature to accept `order.events.Money` — the conversion
  logic remains identical: `BigDecimal.valueOf(money.getUnits()).add(BigDecimal.valueOf(money.getNanos(), 9))`
- Remove the `toMoney(BigDecimal amount, String currency)` method — no longer needed, as outgoing
  `PaymentResponse` events do not carry a `Money` field

No other changes to this class.

---

## Phase 5 — Kafka Infrastructure (depends on: pom.xml → Step 1, application.yaml → Step 4)

### Step 8 — CREATE `src/main/java/com/mercora/paymentservice/kafka/KafkaConfig.java`

Create a Kafka configuration class following the same pattern as `order-service/kafka/KafkaConfig.java`.
Payment-service acts as both producer (publishes to `payment`) and consumer (subscribes to `order`), so both
factories must be defined.

It must:

- Be annotated with `@EnableKafka` and `@Configuration`
- Inject via `@Value`:
  - `${spring.kafka.bootstrap-servers}` — Kafka broker address
  - `${spring.kafka.producer.key-serializer}` — producer key serializer class
  - `${spring.kafka.producer.value-serializer}` — producer value serializer class
  - `${spring.kafka.consumer.key-deserializer}` — consumer key deserializer class
  - `${spring.kafka.consumer.value-deserializer}` — consumer value deserializer class
  - `${spring.kafka.consumer.group-id}` — consumer group ID
- Define a `ProducerFactory<String, byte[]> producerFactory()` bean configured with bootstrap servers,
  key serializer, and value serializer
- Define a `KafkaTemplate<String, byte[]> kafkaTemplate(ProducerFactory<String, byte[]>)` bean
- Define a `ConsumerFactory<String, byte[]> consumerFactory()` bean configured with bootstrap servers,
  key deserializer, value deserializer, and group ID
- Define a `ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(ConsumerFactory<String, byte[]>)` bean — required for `@KafkaListener` to work with `byte[]` payloads

---

## Phase 6 — Kafka Producer (depends on: KafkaConfig → Step 8, payment_service.proto → Step 2)

### Step 9 — CREATE `src/main/java/com/mercora/paymentservice/kafka/KafkaProducer.java`

Create a Spring `@Service` that publishes payment result events to the `payment` Kafka topic.

It must:

- Inject `KafkaTemplate<String, byte[]>` via constructor
- Expose a method `publishResult(Payment payment, String correlationId)` that:
  - Determines the human-readable event label from `payment.getStatus()`:
    - `APPROVED` → `message = "PAYMENT_PROCESSED"`
    - `REJECTED` → `message = "PAYMENT_FAILED"`
  - Builds a `PaymentResponse` protobuf message:
    - `payment_id` = `payment.getId().toString()`
    - `order_id` = `payment.getOrderId()`
    - `status` = `PaymentStatus` enum value matching `payment.getStatus()` (e.g. `PaymentStatus.APPROVED`)
    - `message` = event label string (above)
    - `correlation_id` = `correlationId`
  - Serializes with `paymentResponse.toByteArray()`
  - Sends to topic `"payment"` via `kafkaTemplate.send("payment", bytes)`
  - Logs the published event including `orderId`, `status`, and `correlationId`
  - Wraps the send call in a try/catch; on exception, logs the error with the failed event details

---

## Phase 7 — Kafka Consumer (depends on: KafkaConfig → Step 8, KafkaProducer → Step 9, PaymentRepository → Step 6, MoneyMapper → Step 7, order_service.proto → Step 3)

### Step 10 — CREATE `src/main/java/com/mercora/paymentservice/kafka/KafkaConsumer.java`

Create a Spring `@Service` Kafka consumer that drives the full payment lifecycle from event receipt to result
publication.

It must:

- Inject `PaymentRepository` and `KafkaProducer` via constructor
- Define a method annotated with `@KafkaListener(topics = "order", groupId = "payment-service-group")`
  that receives `byte[]`
- Inside the listener method, execute the following steps in order:
  1. Deserialize: `OrderEvent event = OrderEvent.parseFrom(payload)`
  2. Extract `orderId = event.getOrderId()` and `correlationId = event.getCorrelationId()`
  3. Log the received event with `orderId` and `correlationId`
  4. **Idempotency check**: call `paymentRepository.findByOrderId(orderId)`:
     - If a `Payment` record is found, log `"Payment already processed for orderId={}, status={}, skipping"`
       using the stored payment's status, then return immediately without reprocessing
  5. Convert amount: `BigDecimal amount = MoneyMapper.toBigDecimal(event.getAmount())`
  6. Build a new `Payment` entity:
     - `orderId` = `event.getOrderId()`
     - `amount` = converted `BigDecimal`
     - `currency` = `event.getAmount().getCurrency()`
     - `paymentMethod` = `event.getPaymentMethod()`
     - `status` = `PaymentStatus.PENDING`
  7. **Approval logic**: use `amount.compareTo(new BigDecimal("10.00")) == 0` to check for the rejection
     threshold — if true, set `status = PaymentStatus.REJECTED`; otherwise set `status = PaymentStatus.APPROVED`.
     Use `compareTo` (not `equals`) to handle BigDecimal scale differences correctly.
  8. Save: `Payment savedPayment = paymentRepository.save(payment)`
  9. Publish: `kafkaProducer.publishResult(savedPayment, correlationId)`
  10. Log the outcome with `orderId`, final `status`, and `correlationId`
- Wrap the entire method body in a `try/catch (InvalidProtocolBufferException e)` for deserialization errors:
  log the error and return. Wrap the remaining logic in a `try/catch (Exception e)`: log the error with
  `orderId` if already extracted

---

## Phase 8 — Cleanup (depends on: all previous phases complete)

### Step 11 — DELETE `src/main/java/com/mercora/paymentservice/grpc/PaymentGrpcService.java`

Remove this file entirely. All gRPC-related code has been replaced by the Kafka-based event-driven flow.
No other file in the updated codebase references this class.

---

## Summary of Changes

| Step | Action | File                                                          |
|------|--------|---------------------------------------------------------------|
| 1    | UPDATE | `pom.xml`                                                     |
| 2    | UPDATE | `src/main/proto/payment_service.proto`                        |
| 3    | CREATE | `src/main/proto/order_service.proto`                          |
| 4    | UPDATE | `src/main/resources/application.yaml`                         |
| 5    | UPDATE | `src/main/java/.../model/Payment.java`                        |
| 6    | UPDATE | `src/main/java/.../repository/PaymentRepository.java`         |
| 7    | UPDATE | `src/main/java/.../mapper/MoneyMapper.java`                   |
| 8    | CREATE | `src/main/java/.../kafka/KafkaConfig.java`                    |
| 9    | CREATE | `src/main/java/.../kafka/KafkaProducer.java`                  |
| 10   | CREATE | `src/main/java/.../kafka/KafkaConsumer.java`                  |
| 11   | DELETE | `src/main/java/.../grpc/PaymentGrpcService.java`              |
