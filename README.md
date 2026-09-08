# Hexagonal Orders — Ports & Adapters in Practice

A minimal, teaching-grade implementation of **Hexagonal Architecture** (a.k.a. Ports & Adapters)
with a full stack: **Java 17**, **Maven**, **Spring Boot 3**, **MySQL**, and a responsive
**Angular 22** dashboard.

The entire business core lives in a **standalone, pure-Java Maven library** with **zero framework
dependencies**. The Spring Boot app depends on that library like any other JAR and implements the
*adapters* (REST on the input side, MySQL on the output side). The Angular frontend talks to the
REST API through a dev-server proxy.

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
        │   ├── config/WebConfig.java             ← CORS for the Angular dev server
        │   └── adapter/
        │       ├── in/rest/                      ← PRIMARY adapter: REST controller + DTOs
        │       │   ├── OrderController.java
        │       │   └── dto/ (CreateOrderRequest, OrderResponse)
        │       └── out/mysql/                    ← SECONDARY adapter: JPA/MySQL
        │           ├── OrderJpaEntity.java       (DB representation, separate from domain)
        │           ├── SpringDataOrderJpaRepository.java
        │           └── OrderRepositoryJpaAdapter.java  (implements OrderRepository port)
        └── resources/application.properties      ← datasource config (env vars)

frontend/                           ← ANGULAR 22 DASHBOARD (the "primary primary" adapter)
    ├── angular.json                 (proxies /api → localhost:8080 during `ng serve`)
    ├── proxy.conf.json
    └── src/
        ├── styles.css               ← design system: themes, tokens, primitives
        └── app/
            ├── services/order.service.ts  (fetch-based, signals + computed state)
            ├── services/toast.service.ts
            ├── components/order-form/     ← create orders
            ├── components/toast/          ← success/error notifications
            └── components/order-card/     ← status badges + confirm/ship/cancel
            & app.ts / app.html / app.css  ← shell: hero, stats, filters, grid

docker-compose.yml                    ← local MySQL 8 (see Prerequisites)
```

> The `core` module produces `hexagonal-orders-core-1.0.0.jar` — a plain, dependency-free JAR.
> `jar tf` confirms it contains no Spring classes whatsoever.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend language | Java 17 |
| Backend build | Maven (multi-module) |
| Framework (app only) | Spring Boot 3.2, Spring Web, Spring Data JPA |
| Database | MySQL 8 (runtime) / H2 (tests) |
| Backend testing | JUnit 5, AssertJ |
| Frontend | Angular 22 (Standalone components, signals) |
| Frontend styling | Hand-rolled design system (CSS custom properties, dark/light themes) |

---

## Prerequisites

- **Java 17+** and **Maven 3.9+** (backend)
- **Node.js 20+** and npm (frontend)
- **Docker** (with the Compose plugin) — used to start MySQL 8

### 1. Start MySQL with Docker Compose

From the project root:

```bash
docker compose up -d
```

This starts a `mysql:8.0` container (name `hex-mysql`) on `localhost:3306` and creates the
`hex_orders` database. Data is persisted in a named Docker volume, so it survives restarts.

To stop it:

```bash
docker compose down        # stops the container
docker compose down -v     # stops AND wipes the data volume
```

> If you previously started MySQL with `docker run` a container named `hex-mysql` may be
> occupying port 3306. Stop it first: `docker stop hex-mysql && docker rm hex-mysql`
> (or run `docker compose up -d` after removing it).

The app creates the database automatically if missing (`createDatabaseIfNotExist=true`).

---

## Running the Application

### 1. Build the core library + app

```bash
mvn clean install
```

This compiles the **core** (pure Java), runs its tests, installs the JAR to your local
`~/.m2` repository, then builds the **app** against it.

### 2. Start the app (Spring Boot REST API)

```bash
cd app
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar app/target/hexagonal-orders-app-1.0.0.jar
```

### 3. Start the frontend (Angular)

In a second terminal:

```bash
cd frontend
npm install          # first time only
ng serve
```

Open http://localhost:4200 — the dashboard loads. During `ng serve`, Angular proxies every
`/api` call to the Spring Boot API on `localhost:8080` (see `frontend/proxy.conf.json`), so no
CORS is involved in local development. The API is also reachable directly at
http://localhost:8080/api/orders.

> The API accepts cross-origin requests from `http://localhost:4200` too
> (see `app/.../config/WebConfig.java`), so a separately-hosted build of `frontend/dist` can
> talk to the API without a proxy.

---

## Database Configuration

The datasource is configured with **environment variables** that have local-friendly fallbacks,
so it works out of the box with the compose defaults:

```properties
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:hex_orders}?createDatabaseIfNotExist=true
spring.datasource.username=${MYSQL_USER:root}
spring.datasource.password=${MYSQL_PASSWORD:root}
spring.jpa.hibernate.ddl-auto=update
```

| Variable | Default | Purpose |
|---|---|---|
| `MYSQL_HOST` | `localhost` | Database host |
| `MYSQL_PORT` | `3306` | Database port |
| `MYSQL_DATABASE` | `hex_orders` | Database/schema name |
| `MYSQL_USER` | `root` | DB user |
| `MYSQL_PASSWORD` | `root` | DB password (matches `MYSQL_ROOT_PASSWORD` in `docker-compose.yml`) |

Override any of them when running the app, e.g.:

```bash
MYSQL_PASSWORD=mysecret mvn spring-boot:run
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

## Frontend at a Glance

The Angular dashboard is a *primary adapter* for the order API — a complete, self-contained UI
that stays decoupled from the backend contract beyond the REST DTOs:

- Glass-morphism cards and panels, gradient accents, and a **dark / light theme** (auto-detects
  your OS preference, toggle is persisted).
- Fully **responsive**: single-column on phones, form + orders grid on desktop.
- Live stats (total orders, active value, pending, shipped) computed with **signals**.
- Status filter chips with counts; contextual **Confirm / Ship / Cancel** actions per order
  (buttons appear only when the business rules allow them).
- Optimistic card animations, skeleton loading, toasts, and friendly empty / error states.

---

## Try It Yourself

- Swap the MySQL adapter: implement `OrderRepository` backed by files or an in-memory store and
  register it in `AppConfig` — the domain never notices.
- Add a *second* primary adapter (e.g. a CLI) that calls `OrderService` without HTTP.
- Add a domain event (e.g. `OrderShippedEvent`) published from `OrderServiceImpl`.
- Add a frontend "Deliver" action for shipped orders once the API supports it.

---

## License

MIT