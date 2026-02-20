# Mercora

<p>
  <img alt="mercora" src="/presentation/mercora_logo.png" width="150"/>
</p>

A scalable, event-driven e-commerce platform built with Java Spring Boot microservices. Mercora demonstrates modern
distributed system design patterns including microservices architecture, event-driven communication, and
containerized deployment.

## Project Overview

Mercora is an enterprise-grade e-commerce backend system that manages the complete order fulfillment lifecycle. The
platform is designed with scalability, resilience, and maintainability in mind, showcasing industry best practices
for building distributed systems.

**Key Features:**

- Complete order management system with real-time inventory tracking
- Secure payment processing with multiple gateway integrations
- Event-driven architecture for loose coupling and asynchronous workflows
- Service-to-service communication via REST APIs and gRPC
- Cloud-ready containerized deployment
- Horizontal scalability through microservices isolation

## Architecture

### System Design

Mercora follows a **microservices architecture** with the following principles:

- **Database per Service**: Each microservice maintains its own dedicated database, ensuring data isolation and
  independent scalability
- **API Gateway**: Single entry point for all client requests, handling routing, load balancing, and cross-cutting
  concerns
- **Event-Driven Communication**: Apache Kafka enables asynchronous, decoupled service interactions
- **Synchronous RPC**: gRPC used for critical inter-service communication requiring immediate responses
- **Container Orchestration**: Docker-based deployment with cloud-ready architecture patterns

### Architecture Diagram

![Diagram](/presentation/diagram-architecture.png)

## Technology Stack

### Core Framework

- **Java 21** - Programming language
- **Spring Boot 4.0** - Microservice framework
- **Spring Cloud** - Distributed system tools

### Communication

- **REST APIs** - Client-facing and service-to-service communication
- **gRPC** - High-performance synchronous service calls
- **Apache Kafka** - Event streaming and asynchronous messaging

### Data Management

- **PostgreSQL** - Relational database for each service
- **Spring Data JPA** - ORM and data access

### Infrastructure & Deployment

- **Docker** - Containerization
- **Docker Compose** - Local orchestration
- **AWS-Inspired Design** - ECS-compatible, RDS-compatible, MSK-compatible architecture

### Development & Testing

- **JUnit 5** - Unit testing
- **Testcontainers** - Integration testing with containers
- **Spring Test** - Framework testing utilities
- **Mockito** - Mocking library

### Monitoring & Logging

- **Spring Boot Actuator** - Health checks and metrics
- **SLF4J + Logback** - Structured logging
- **Micrometer** - Application metrics

## Microservices Overview

### Order Service

**Responsibility:** Manages the complete order lifecycle from creation to fulfillment.

**Key Responsibilities:**

- Accept and validate customer orders
- Orchestrate payment and inventory workflows
- Maintain order state and history
- Emit domain events (order.created, order.confirmed, order.shipped)
- Expose REST API for order management

**Technology:** Spring Boot, PostgreSQL, gRPC Client, Kafka Producer/Consumer

---

### Payment Service

**Responsibility:** Handles all payment processing and financial transactions.

**Key Responsibilities:**

- Process payment transactions with multiple gateway integrations
- Manage payment states and reconciliation
- Implement retry logic and idempotency
- Emit payment events (payment.initiated, payment.completed, payment.failed)
- Consume order events for payment triggering

**Technology:** Spring Boot, PostgreSQL, gRPC Server, Kafka Producer/Consumer

---

### Inventory Service

**Responsibility:** Manages product catalog and stock levels.

**Key Responsibilities:**

- Track inventory levels in real-time
- Reserve stock for pending orders
- Release reservations on order cancellation
- Update stock on successful fulfillment
- Emit inventory events (stock.reserved, stock.released, stock.updated)

**Technology:** Spring Boot, PostgreSQL, REST API, Kafka Producer/Consumer

## Communication Patterns

### 1. REST APIs

Used for client-facing operations and external service communication.

**Example:** Creating an order via API Gateway

```
POST /api/orders
{
  "customerId": "123",
  "items": [{"productId": "456", "quantity": 2}]
}
```

**Services Exposing REST:**

- Order Service: `/api/orders`, `/api/orders/{id}`
- Inventory Service: `/api/inventory`, `/api/inventory/{productId}`
- Payment Service: Health checks, metrics endpoints

---

### 2. gRPC Communication

Used for synchronous, high-performance inter-service communication between Order and Payment services.

**Example:** Order Service calls Payment Service

```
Order Service → Payment Service (gRPC)
"Process payment for order #12345"
↓
Payment Service processes and returns result immediately
```

**Advantages:**

- Low latency and high throughput
- Binary protocol for efficiency
- Strongly typed contracts

---

### 3. Apache Kafka Events

Used for event-driven, asynchronous communication to ensure loose coupling.

**Event Topics:**

- `order.created` - Triggered when a new order is placed
- `order.confirmed` - Triggered after payment is successful
- `payment.initiated` - Triggered when payment processing begins
- `payment.completed` - Triggered after successful payment
- `payment.failed` - Triggered on payment failure
- `inventory.reserved` - Triggered when inventory is reserved
- `inventory.released` - Triggered when reservation is cancelled
- `inventory.updated` - Triggered when stock levels change

**Event Flow Example:**

```
1. Order Service publishes: order.created
2. Inventory Service consumes → reserves stock
3. Payment Service consumes → initiates payment
4. Payment Service publishes: payment.completed
5. Order Service consumes → updates order status
6. Kafka ensures durability and replay capability
```

## Prerequisites

- **Java 21** - [Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **Maven 4.0** - [Download](https://maven.apache.org/download.cgi)
- **Git** - Version control

## Running Locally

### Option 1: Docker Compose (Recommended)

**Step 1:** Clone the repository

```bash
git clone https://github.com/yourusername/mercora.git
cd mercora
```

**Step 2:** Start all services

```bash
docker-compose up -d
```

This will start:

- 3 microservices (Order, Payment, Inventory)
- 3 PostgreSQL databases
- Apache Kafka
- API Gateway (if configured)

**Step 3:** Verify services are running

```bash
docker-compose ps
```

**Step 4:** Check service health

```bash
curl http://localhost:8080/actuator/health  # Order Service
curl http://localhost:8081/actuator/health  # Payment Service
curl http://localhost:8082/actuator/health  # Inventory Service
```

---

### Option 2: Local Development

**Step 1:** Clone and navigate to repository

```bash
git clone https://github.com/yourusername/mercora.git
cd mercora
```

**Step 2:** Start infrastructure (Kafka, PostgreSQL)

```bash
docker-compose up -d kafka postgres-order postgres-payment postgres-inventory
```

**Step 3:** Build all services

```bash
mvn clean install -DskipTests
```

**Step 4:** Run each service in separate terminals

```bash
# Terminal 1: Order Service
cd order-service && mvn spring-boot:run

# Terminal 2: Payment Service
cd payment-service && mvn spring-boot:run

# Terminal 3: Inventory Service
cd inventory-service && mvn spring-boot:run
```

**Step 5:** Test the system

```bash
# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "cust123", "items": [{"productId": "prod456", "quantity": 2}]}'

# Check order status
curl http://localhost:8080/api/orders/{orderId}

# Check inventory
curl http://localhost:8082/api/inventory/prod456
```

### Docker Compose Configuration

The `docker-compose.yml` file includes:

- PostgreSQL containers for each service
- Kafka and Zookeeper for event streaming
- Service environment configurations
- Network isolation and service discovery

```yaml
version: '3.8'
services:
# Services defined here...
# See docker-compose.yml for full configuration
```

## Project Structure

```
mercora/
├── backend/
    ├── api-gateway/
    ├── order-service/
    │   ├── src/main/java/com/mercora/order/
    │   ├── src/test/java/com/mercora/order/
    │   └── pom.xml
    ├── payment-service/
    │   ├── src/main/java/com/mercora/payment/
    │   ├── src/test/java/com/mercora/payment/
    │   └── pom.xml
    ├── inventory-service/
    │   ├── src/main/java/com/mercora/inventory/
    │   ├── src/test/java/com/mercora/inventory/
    │   └── pom.xml
├── docker-compose.yml
├── README.md
```

## Learning Goals

This project was developed to demonstrate and master:

1. **Microservices Architecture** - Breaking monolithic applications into independent, deployable services
2. **Event-Driven Design** - Building loosely coupled systems using event streams (Kafka)
3. **Inter-Service Communication** - Implementing both synchronous (REST, gRPC) and asynchronous patterns
4. **Database per Service Pattern** - Managing data isolation and consistency across services
5. **Distributed Transactions** - Handling eventual consistency and saga patterns
6. **gRPC Protocol** - Building high-performance RPC communication with Protocol Buffers
7. **Spring Boot & Spring Cloud** - Enterprise Java development with modern frameworks
8. **Container Orchestration** - Docker and container-based deployment
9. **Cloud Architecture** - AWS-inspired design patterns (ECS, RDS, MSK)
10. **Resilience Patterns** - Circuit breakers, retries, and timeouts
11. **Observability** - Logging, metrics, and health checks in distributed systems

**Built with ☕ and ❤️ using Java, Spring Boot, and modern distributed system patterns.**
