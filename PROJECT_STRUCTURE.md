# Project Structure — Order And Returns Management System

This document explains the layout of the repository and the purpose of each top-level folder and important modules. It helps new developers quickly locate business logic (order/return state machines), background job processing, third-party integration code, and tests.

Repository root
- `pom.xml` — Maven build file (project coordinates, Java 17, dependencies, plugins).
- `README.md` — High-level project instructions, how to run tests, docker-compose examples.
- `docker-compose.yml` (optional) — Compose file used for local integration testing (RabbitMQ, Postgres).
- `PROJECT_STRUCTURE.md` — (this file) description of the project layout.
- `chat.md` — conversational log (if present).

Target and build artifacts
- `target/` — Maven build output (do not edit). Contains compiled classes, packaged JAR, test reports.

Source code — core application
- `src/main/java/com/example/ordermanagement/` — main application package.
  - `Application.java` — Spring Boot application entry point.

  - `config/` — Spring configuration classes.
    - `RabbitConfig` — RabbitMQ exchanges/queues/bindings and RabbitTemplate setup used to publish/consume background job messages.
    - Other configs (data source, object mapping, feature flags) as needed.

  - `controller/` — HTTP REST controllers.
    - `OrderController` — endpoints for creating orders, making payments, checking status, cancelling where allowed.
    - `ReturnController` — endpoints for initiating returns and updating return approvals/shipments.

  - `domain/` — domain objects and enums that represent core business models.
    - `Order` — core order entity with fields such as id, items, amount, current state.
    - `OrderStatus` / `OrderStateHistory` — enums and history records for order lifecycle and a table/entity to record changes for audit.
    - `ReturnRequest` — return entity tied to an order.
    - `ReturnStatus` / `ReturnStateHistory` — enums and history records for the return lifecycle.
    - `JobLog`, `JobType`, `JobStatus` — job/audit records for background processing attempts and results.

  - `repository/` — Spring Data JPA repositories.
    - `OrderRepository`, `ReturnRequestRepository`, `OrderStateHistoryRepository`, `ReturnStateHistoryRepository`, `JobLogRepository` — persistence interfaces for domain entities.

  - `service/` — business logic and orchestration.
    - `OrderService` — implements order lifecycle transitions, enforces state machine rules (PENDING_PAYMENT -> PAID -> PROCESSING_IN_WAREHOUSE -> SHIPPED -> DELIVERED), and allowed cancels. It records every state change into `OrderStateHistory` for auditing.
    - `ReturnService` (or included in `OrderService`) — enforces return rules: only allow REQUESTED if order is DELIVERED, progression through REQUESTED -> APPROVED/REJECTED -> IN_TRANSIT -> RECEIVED -> COMPLETED, and recording state changes to `ReturnStateHistory`.
    - `JobPublisherService` — publishes background job messages to RabbitMQ (or enqueues to internal job processor), used to trigger PDF generation and refund processing.
    - `PdfService` — generates PDF invoices for shipped orders; executed asynchronously.
    - `RefundClient` / `MockRefundClient` — integration client used to call a mock payment gateway for refunds. In tests/mocks a `MockRefundClient` simulates external responses and failures.

  - `listener/` — message listeners / background job consumers.
    - `InvoiceListener` — consumes invoice generation messages and calls `PdfService` to create/store or email the invoice.
    - `RefundListener` — consumes refund processing messages and calls `RefundClient` to execute refunds; logs results to `JobLog`.

  - `messaging/` — DTOs and message contract classes used on the message bus.
    - `dto/` — message payload classes (InvoiceRequestDto, RefundRequestDto, etc.).

  - `domain/` (continued) — state history logging
    - The code records every state transition for both orders and returns in history tables. Use these tables for auditing and for reconstructing timeline of a request.

Source code — tests
- `src/test/java/com/example/ordermanagement/` — unit and integration tests.
  - `service/OrderServiceTest` — unit tests for order logic and state transitions.
  - `integration/IntegrationInvoiceFlowTest` — integration test that can start RabbitMQ and Postgres using Testcontainers or rely on local Docker compose. This verifies that when an order moves to SHIPPED, the invoice job is published and the PDF is created.

Resources and configuration
- `src/main/resources/application.yml` — default Spring Boot properties; contains datasource, rabbit properties, and app-specific properties like `app.invoices.path`.
- `src/main/resources/application-local.yml` — overrides for local development (ports, in-memory DB settings, etc.).

How the main features map to code (quick reference)
- Order state machine
  - Implemented as an enum `OrderStatus` and enforced in `OrderService`. Methods like `payOrder(...)`, `cancelOrder(...)`, `markProcessed(...)`, `shipOrder(...)`, `markDelivered(...)` will validate current state and apply allowed transitions. Each transition writes a `OrderStateHistory` record.

- Return state machine
  - Implemented as `ReturnStatus` and enforced in `ReturnService` (or `OrderService`). A return can only be created if the linked order is DELIVERED. Manager actions approve/reject; accepted flows continue through shipping and warehouse receipt to COMPLETED. Each change writes a `ReturnStateHistory` row.

- Background processing & RabbitMQ
  - When order transitions to SHIPPED: `OrderService` enqueues an Invoice job via `JobPublisherService` that sends an `InvoiceRequestDto` to an exchange/queue. `InvoiceListener` consumes it and invokes `PdfService` asynchronously to create a simple PDF and simulate an email (or store to disk at `app.invoices.path`).
  - When return becomes COMPLETED: `ReturnService` enqueues a Refund job (`RefundRequestDto`). `RefundListener` invokes `RefundClient` to call the mock payment gateway. Results are logged to `JobLog` and the job may retry/fail based on `RefundClient` exceptions.

Operational notes
- Database migrations: this project can be extended to use Flyway/Liquibase; currently JPA DDL auto-generation may be in use (check `application.yml`).
- Local integration: use the provided `docker-compose.yml` or Testcontainers via Maven to spin up RabbitMQ and Postgres for integration tests. See `README.md` for example commands.

Where to change behavior
- To add a new order transition: update `OrderStatus`, add transition enforcement in `OrderService`, and add a corresponding `OrderStateHistory` entry.
- To change background job routing: update `RabbitConfig` to change exchanges/queues and update `JobPublisherService` and listener bindings accordingly.
- To swap the mock refund gateway for a real client: replace `MockRefundClient` with an implementation of `RefundClient` that performs HTTP calls. Add robust error handling and retries.

Developer tips
- Use service-level unit tests for state transitions and history recording (happy + failure paths).
- Keep listeners thin: parse the message, validate payload, call a service that performs the real business work. This keeps message handling idempotent and testable.
- Always record state changes with actor and timestamp for auditing.

Next steps and suggestions
- Add integration tests for refund flow (COMPLETED -> refund job -> RefundClient behavior).
- Add metrics and health checks for the RabbitMQ listener consumers.
- Add retry handling and dead-letter queues for failed background jobs.

If you want, I can also:
- generate a `diagram.png` or an ASCII state-transition diagram of the order and return flows, or
- add a small CONTRIBUTING.md snippet that explains how to run tests and add new state transitions.


