# Hexagonal Orders — Ports & Adapters in Practice

A minimal, teaching-grade implementation of **Hexagonal Architecture** (a.k.a. Ports & Adapters)
using **Java 17**, **Maven**, **Spring Boot 3**, and **MySQL**.

The entire business core lives in a **standalone, pure-Java Maven library** with **zero framework
dependencies**. The Spring Boot app depends on that library like any other JAR and implements the
*adapters* (REST on the input side, MySQL on the output side).

---

## What Is Hexagonal Architecture?

Hexagonal Architecture (Alistair Cockburn) separates **business logic** from the **outside world**
(databases, web frameworks, messaging).

```
              ┌────────────────────────────────────────┐
              │            PRIMARY ADAPTERS             │
              │     REST API · CLI · MQ Consumers       │
              ├────────────────────────────────────────┤
              │              PRIMARY PORTS              │
              │   interfaces the app EXPOSES            │
              │                                         │
              │      ┌────────────────────────┐         │
              │      │       DOMAIN CORE       │         │
              │      │  models · services ·    │         │
              │      │  business rules         │         │
              │      │  (pure Java)            │         │
              │      └────────────────────────┘         │
              │                                         │
              │             SECONDARY PORTS             │
              │   interfaces the app REQUIRES           │
              ├────────────────────────────────────────┤
              │            SECONDARY ADAPTERS           │
              │    MySQL · PostgreSQL · Files · MQ      │
              └────────────────────────────────────────┘
```

Key ideas:

- **Domain is the center.** It knows nothing about HTTP, Spring, or databases.
- **Ports are interfaces.** The domain *needs* `OrderRepository` (secondary port) and *exposes*
  `OrderService` (primary port) — but never sees any implementation.
- **Adapters plug into ports.** Change the database by swapping one adapter class.
  The domain core is never touched.
- **The core is a library.** It is compiled into a plain JAR with no runtime dependencies.

---

## Project Structure

```
hexagonal-orders/
├── pom.xml                          ← Maven multi-module parent (aggregator)
│
├── core/                            ← PURE JAVA LIBRARY — the business heart
│   ├── pom.xml                      (only JUnit + AssertJ as test deps)
│   └── src/
│       ├── main/java/com/example/hex/domain/
│       │   ├── model/Order.java           ← entity + business rules
│       │   ├── port/OrderService.java     ← PRIMARY port (what app exposes)
│       │   ├── port/OrderRepository.java  ← SECONDARY port (what app needs)
│       │   └── service/OrderServiceImpl.java ← use cases / business workflow
│       └── test/java/com/example/hex/domain/
│           ├── port/InMemoryOrderRepository.java  ← FAKE adapter for tests
│           └── service/OrderServiceImplTest.java  ← domain tests, no Spring
│
└── app/                             ← SPRING BOOT APP — adapters & wiring
    ├── pom.xml                      (depends on `hexagonal-orders-core`)
    └── src/main/
        ├── java/com/example/hex/
        │   ├── HexagonalOrdersApplication.java   ← Spring Boot entry point
        │   ├── config/AppConfig.java             ← composition root (wires ports ⇄ adapters)
        │   └── adapter/
        │       ├── in/rest/                      ← PRIMARY adapter: REST controller + DTOs
        │       │   ├── OrderController.java
        │       │   └── dto/ (CreateOrderRequest, OrderResponse)
        │       └── out/mysql/                    ← SECONDARY adapter: JPA/MySQL
        │           ├── OrderJpaEntity.java       (DB representation, separate from domain)
        │           ├── SpringDataOrderJpaRepository.java
        │           └── OrderRepositoryJpaAdapter.java  (implements OrderRepository port)
        └── resources/application.properties      ← datasource config
```

> The `core` module produces `hexagonal-orders-core-1.0.0.jar` — a plain, dependency-free JAR.
> `jar tf` confirms it contains no Spring classes whatsoever.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven (multi-module) |
| Framework (app only) | Spring Boot 3.2, Spring Web, Spring Data JPA |
| Database | MySQL 8 (runtime) / H2 (tests) |
| Testing | JUnit 5, AssertJ |

---

## Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **MySQL 8** running on `localhost:3306` (see below), **or** Docker

### Quick MySQL with Docker

```bash
docker run -d --name hex-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=hex_orders \
  -p 3306:3306 \
  mysql:8.0
```

The app creates the database automatically if missing (`createDatabaseIfNotExist=true`).

---

## Running the Application

### 1. Build the core library + app

```bash
mvn clean install
```

This compiles the **core** (pure Java), runs its tests, installs the JAR to your local
`~/.m2` repository, then builds the **app** against it.

### 2. Start the app

```bash
cd app
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar app/target/hexagonal-orders-app-1.0.0.jar
```

---

## Database Configuration

Edit `app/src/main/resources/application.properties` to match your environment:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hex_orders?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

---

## REST API

Base URL: `http://localhost:8080/api/orders`

| Method | Endpoint            | Description                 |
|--------|---------------------|-----------------------------|
| POST   | `/api/orders`       | Create an order             |
| GET    | `/api/orders`       | List all orders             |
| GET    | `/api/orders/{id}`  | Get one order               |
| POST   | `/api/orders/{id}/confirm` | Confirm an order     |
| POST   | `/api/orders/{id}/ship`    | Ship an order        |
| POST   | `/api/orders/{id}/cancel`  | Cancel an order      |

### Example

```bash
# Create
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productName":"Laptop","quantity":2,"unitPrice":1500.00}'

# Response (201 Created)
{
  "id": "cebfc6b8-abcd-4f45-be21-5fcddab167ba",
  "productName": "Laptop",
  "quantity": 2,
  "unitPrice": 1500.00,
  "totalPrice": 3000.00,
  "status": "PENDING",
  "createdAt": "2026-09-08T14:39:52.151127701"
}

# Workflow
curl -X POST http://localhost:8080/api/orders/{id}/confirm
curl -X POST http://localhost:8080/api/orders/{id}/ship
```

---

## Business Rules (in the Domain Core)

These rules live **only** in `core/.../domain/model/Order.java` — no framework involved:

- `PENDING → CONFIRMED → SHIPPED → DELIVERED`
- Only `PENDING` orders can be **confirmed**
- Only `CONFIRMED` orders can be **shipped**
- `SHIPPED` / `DELIVERED` orders **cannot be cancelled**
- Quantity and unit price must be **positive**

---

## Testing Strategy

Because of hexagonal architecture, the domain is tested **without Spring, HTTP, or MySQL**:

```bash
mvn test
```

`core/src/test/.../InMemoryOrderRepository.java` is a *fake* secondary adapter (an in-memory map)
that implements the same `OrderRepository` port as the MySQL adapter. The same domain service
(`OrderServiceImpl`) is exercised against it:

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

The MySQL adapter can be tested the same way — swap the fake for JPA and the domain code stays
identical.

---

## Why This Architecture?

1. **The core is trivially testable** — domain tests run in milliseconds, no context to load.
2. **Adapters are swappable** — move from MySQL to PostgreSQL, or add MongoDB, by implementing
   the same port. The core JAR is untouched.
3. **The framework is optional** — the business logic is a plain Java library; you could drive it
   from a CLI, a batch job, or even a unit test with no Spring at all.
4. **Dependency inversion is enforced by Maven** — `app` depends on `core`, never the reverse.
   The core cannot accidentally import a Spring class because it doesn't have Spring on its classpath.
5. **Clear boundaries** — your team knows exactly where business logic goes (`core`) and where
   infrastructure lives (`app.adapter`).

---

## Try It Yourself

- Swap the MySQL adapter: implement `OrderRepository` backed by files or an in-memory store and
  register it in `AppConfig` — the domain never notices.
- Add a *second* primary adapter (e.g. a CLI) that calls `OrderService` without HTTP.
- Add a domain event (e.g. `OrderShippedEvent`) published from `OrderServiceImpl`.

---

## License

MIT