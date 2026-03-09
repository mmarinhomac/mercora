# Mercora

<p>
  <img alt="mercora" src="/presentation/mercora_logo.png" width="150"/>
</p>

Mercora is an event-driven e-commerce backend built with Java and Spring Boot microservices.
It combines synchronous gRPC calls and asynchronous Kafka events to handle order, payment, and inventory workflows.

## Architecture

<img alt="archtecture" src="/presentation/arch_diagram.png" width="100%"/>

### Core services

- **Gateway**: single public entrypoint and auth validation
- **Auth Service**: login and token validation
- **Order Service**: creates/manages orders
- **Payment Service**: processes payments from order events
- **Inventory Service**: reserves/confirms/releases stock (stateless)
- **Kafka**: async communication backbone

### Main flow

```text
Client -> Gateway -> Order Service
                    |- gRPC -> Inventory Service (reserve stock -> reservationId)
                    |- persist order + reservationId
                    |- Kafka: ORDER_CREATED
                    v
               Payment Service (consumes ORDER_CREATED)
                    |- Kafka: PAYMENT_PROCESSED / PAYMENT_FAILED
                    v
               Inventory Service confirms or releases reservation
```

### Communication model

- **External:** REST via Gateway
- **Internal sync:** gRPC (`order-service` -> `inventory-service`)
- **Internal async:** Kafka (`order` and `payment` topics)
- **Persistence:** PostgreSQL per stateful service (Inventory is stateless)

## Service summary

| Service   | API / Contract                              | Role                                                                                        |
|-----------|---------------------------------------------|---------------------------------------------------------------------------------------------|
| Gateway   | REST                                        | Routes client requests and validates auth with auth-service                                 |
| Auth      | `POST /auth/login`, `POST /auth/validate`   | Authentication and token validation                                                         |
| Order     | `POST /orders`, `GET /orders` + gRPC client | Creates orders, reserves stock, publishes `ORDER_CREATED`                                   |
| Payment   | Kafka consumer/producer                     | Consumes `ORDER_CREATED`, stores payment, publishes `PAYMENT_PROCESSED` or `PAYMENT_FAILED` |
| Inventory | gRPC server + Kafka consumer                | Handles reservation lifecycle based on payment result                                       |

## Tech stack

- Java 21
- Spring Boot
- PostgreSQL
- Apache Kafka
- gRPC
- Docker / Docker Compose

## Run locally (Docker Compose)

```bash
# For the first time (build images and start containers):
docker compose up --build -d
# For a clean start (rebuild images and recreate containers):
docker compose up --build --force-recreate -d
```

Gateway is exposed at `http://localhost:4000`.

## API Documentation

Each service exposes its OpenAPI spec as a static file under `src/main/resources/static/`.
Once the stack is running, the specs are accessible at:

| Service           | OpenAPI Spec URL                                     |
|-------------------|------------------------------------------------------|
| Auth Service      | http://localhost:4004/openapi-auth-service.yaml      |
| Order Service     | http://localhost:4001/openapi-order-service.yaml     |
| Payment Service   | http://localhost:4002/openapi-payment-service.yaml   |
| Inventory Service | http://localhost:4003/openapi-inventory-service.yaml |

The Order Service also serves an interactive UI (powered by [Scalar](https://scalar.com)) at:

```
http://localhost:4001/api-docs-order-service
```

> Payment Service and Inventory Service have no REST endpoints. Their OpenAPI files document
> Kafka event schemas and gRPC contracts respectively.

<img alt="archtecture" src="/presentation/docs_scalar.png" width="100%"/>

## Testing status

Current tested scope in this project is **integration tests only**, under:

- `backend/integration-tests`
    - `AuthIntegrationTest`
    - `OrderIntegrationTest`

Run them with:

```bash
cd backend/integration-tests
mvn test
```

## Observability status

Current observability is **logging only**.

- Structured application logs
- Correlation ID propagation in event flows
- No metrics/tracing stack documented as part of the current implementation

## Infrastructure as Code (IaC) / AWS-ready

`infrastructure/` is part of the project and is a strong AWS-ready foundation:

- AWS CDK (Java) stack source: `infrastructure/src/main/java/com/mercora/stack/LocalStack.java`
- Synthesized CloudFormation template: `infrastructure/cdk.out/localstack.template.json`
- Deployment helper for LocalStack via CloudFormation API: `infrastructure/localstack-deploy.sh`

The stack models AWS-native components such as ECS/Fargate, RDS, MSK, and ALB, making the architecture directly
transferable to AWS environments.

## Repository structure

```text
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
├── infrastructure/
├── presentation/
├── docker-compose.yml
└── README.md
```

## Common local hurdle

For single-node Kafka in local environments, keep:

```text
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
```
