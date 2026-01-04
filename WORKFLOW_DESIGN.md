# Workflow Design — Order And Returns Management System

Purpose
- Document the Order and Return state machines, where background jobs are queued, the database schema (tables, relationships, indexes), and message flow (RabbitMQ). This is an engineering-facing reference to implement, operate, and test the core workflows.

Assumptions
- Target RDBMS: PostgreSQL (DDL examples use PostgreSQL types such as UUID and timestamptz). If you prefer MySQL or another DB, I can provide an alternate DDL.
- Message broker: RabbitMQ (existing project contains RabbitConfig and listeners).
- IDs: Use UUIDs for primary business entities (`orders`, `return_requests`).

Checklist (what this doc contains)
- Order state machine (ASCII diagram + notes)
- Return state machine (ASCII diagram + notes)
- Where background jobs are scheduled
- Database schema (tables, columns, FKs, indexes, example DDL)
- How state history is recorded and example queries
- Message flow (queues, DTOs, listeners, retry/DLQ notes)
- Concurrency, idempotency, and operational recommendations
- Appendix: sample enums and JSON payloads


1) Order State Machine

ASCII diagram (allowed transitions)

PENDING_PAYMENT
   |  pay
   v
PAID ----------------------------------------------+
   |                                                |
   | process -> PROCESSING_IN_WAREHOUSE             | cancel (allowed only if not processed)
   v                                                |
PROCESSING_IN_WAREHOUSE -> ship -> SHIPPED -> deliver -> DELIVERED

Allowed cancellations:
- From PENDING_PAYMENT -> CANCELLED
- From PAID -> CANCELLED only if the order hasn't moved to PROCESSING_IN_WAREHOUSE

Notes:
- Transitions must be validated in `OrderService`. Each validated transition creates an `order_state_history` row.
- When an order transitions to `SHIPPED`, an Invoice background job is queued (see Message Flow section).

2) Return State Machine

Precondition: a Return can only be created for an order whose current status is `DELIVERED`.

ASCII diagram (allowed transitions)

REQUESTED
  | manager approves -> APPROVED
  | manager rejects  -> REJECTED
  v
APPROVED -> customer ships -> IN_TRANSIT -> warehouse receives -> RECEIVED -> refund -> COMPLETED

Notes:
- If a return is `REJECTED`, the lifecycle ends; optionally notify customer.
- When a return transitions to `COMPLETED`, a Refund background job is queued.
- All transitions must be logged in `return_state_history`.

3) Background jobs (where and when)
- Invoice Generation: queued when Order -> SHIPPED. Job payload includes order id, customer email, items, shipment details.
- Refund Processing: queued when ReturnRequest -> COMPLETED. Job payload includes return id, order id, refund amount, payment reference.
- Jobs are published to RabbitMQ via `JobPublisherService` (or RabbitTemplate) and consumed by `InvoiceListener` and `RefundListener` that call `PdfService` and `RefundClient` respectively.

4) Database Schema

Overview of main tables
- orders
- order_state_history
- return_requests
- return_state_history
- job_log

Primary keys use UUIDs for business entities. `job_log` may use UUID or serial.

Example PostgreSQL DDL (copy/paste-ready)

-- orders
CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  external_id TEXT,
  customer_id UUID,
  total_amount NUMERIC(12,2) NOT NULL,
  status TEXT NOT NULL,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now(),
  version BIGINT DEFAULT 0,
  -- additional fields (address, payment ref, items serialized or in child table)
  CONSTRAINT orders_status_check CHECK (status IN ('PENDING_PAYMENT','PAID','PROCESSING_IN_WAREHOUSE','SHIPPED','DELIVERED','CANCELLED'))
);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_customer ON orders(customer_id);

-- order_state_history
CREATE TABLE order_state_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  previous_status TEXT,
  new_status TEXT NOT NULL,
  actor TEXT, -- who caused the change (system, user id, admin)
  reason TEXT,
  created_at timestamptz DEFAULT now()
);
CREATE INDEX idx_osh_order_id ON order_state_history(order_id);
CREATE INDEX idx_osh_created_at ON order_state_history(created_at);

-- return_requests
CREATE TABLE return_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  requested_by UUID,
  requested_at timestamptz DEFAULT now(),
  status TEXT NOT NULL,
  refund_amount NUMERIC(12,2),
  reason TEXT,
  processed_by TEXT,
  processed_at timestamptz,
  version BIGINT DEFAULT 0,
  CONSTRAINT rr_status_check CHECK (status IN ('REQUESTED','APPROVED','REJECTED','IN_TRANSIT','RECEIVED','COMPLETED'))
);
CREATE INDEX idx_rr_order_id ON return_requests(order_id);
CREATE INDEX idx_rr_status ON return_requests(status);

-- return_state_history
CREATE TABLE return_state_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  return_id UUID NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
  previous_status TEXT,
  new_status TEXT NOT NULL,
  actor TEXT,
  reason TEXT,
  created_at timestamptz DEFAULT now()
);
CREATE INDEX idx_rsh_return_id ON return_state_history(return_id);
CREATE INDEX idx_rsh_created_at ON return_state_history(created_at);

-- job_log
CREATE TABLE job_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_type TEXT NOT NULL, -- INVOICE, REFUND
  reference_id UUID, -- order_id or return_id depending on job_type
  payload jsonb,
  status TEXT NOT NULL, -- QUEUED, PROCESSING, SUCCESS, FAILED
  attempt INT DEFAULT 0,
  last_error TEXT,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);
CREATE INDEX idx_joblog_type ON job_log(job_type);
CREATE INDEX idx_joblog_ref ON job_log(reference_id);

Notes on schema and mapping
- Use `version` (optimistic locking) on `orders` and `return_requests` to prevent lost updates (maps to `@Version` in JPA).
- Status fields are stored as TEXT for readability and simple migrations; alternatively use smallint or enum types.
- Keep `order_state_history` and `return_state_history` as append-only audit tables; don't update or delete rows except in rare archival tasks.
- Use `ON DELETE CASCADE` on history tables referencing parent to avoid orphaned history for removed test data; consider soft deletes in production.

5) How state history is recorded
- Implementation pattern:
  - All status changes are done inside a transactional service method.
  - After verifying transition rules, update the entity status, persist it, and insert a new history row to `order_state_history` / `return_state_history` with previous/new statuses, actor, reason, and timestamp.
  - Persisting both entity update and history row in the same transaction ensures consistency.

Example pseudocode (Java/Spring/JPA):

@Transactional
public void shipOrder(UUID orderId, String actor) {
  Order order = orderRepository.findById(orderId).orElseThrow(...);
  validateTransition(order.getStatus(), SHIPPED);
  String previous = order.getStatus().name();
  order.setStatus(SHIPPED);
  orderRepository.save(order);
  orderStateHistoryRepository.save(new OrderStateHistory(orderId, previous, SHIPPED.name(), actor, null));
  // queue invoice job after commit (see message flow and optional TransactionSynchronization)
}

Important: queueing background jobs (publishing messages) should happen after the DB transaction commits to avoid jobs running for transactions that later rollback. Use `TransactionSynchronizationManager.registerSynchronization` or an outbox pattern.

6) Message Flow (RabbitMQ)

Textual diagram

[OrderService] --(on SHIPPED)-> Rabbit Exchange `order.jobs` (routingKey: `invoice`) -> Queue `invoice.queue` -> `InvoiceListener` -> calls `PdfService` -> writes file / sends email -> updates `job_log` to SUCCESS

[ReturnService] --(on COMPLETED)-> Rabbit Exchange `order.jobs` (routingKey: `refund`) -> Queue `refund.queue` -> `RefundListener` -> calls `RefundClient` (mock or real payment gateway) -> updates `job_log`

Message contract (DTOs)
- InvoiceRequestDto (json)
  - orderId: UUID
  - customerEmail: string
  - items: [{ sku, name, qty, price }]
  - shippedAt: timestamp
- RefundRequestDto (json)
  - returnId: UUID
  - orderId: UUID
  - amount: numeric
  - paymentReference: string
  - requestedBy: string

Listener responsibilities
- Validate incoming message
- Check idempotency (e.g., `job_log` unique job id or reference+type+attempt) before executing
- Update `job_log` status to PROCESSING at start, SUCCESS on success and FAILED with last_error on exception
- Implement limited retries and then route failing messages to a Dead Letter Queue (DLQ)

Retry / Dead-letter notes
- Use RabbitMQ retry and DLQ: messages that fail after N attempts go to `invoice.dlq` or `refund.dlq`
- Also record attempts in `job_log.attempt` so operators can inspect failed jobs and requeue manually if needed

7) Concurrency, idempotency, and constraints

Concurrency
- Use optimistic locking (`@Version`) on `orders` and `return_requests` to prevent lost updates when multiple services or users attempt state transitions concurrently.
- For high-concurrency operations (e.g., payments), consider SELECT ... FOR UPDATE when needed.

Idempotency
- Jobs must be idempotent: listeners should check `job_log` for an existing SUCCESS for the given job key before re-processing.
- Use a unique job id in the message (UUID) and store it in `job_log.payload` and as a logical key to prevent duplicates.

Constraints and validations
- Enforce allowed transitions in the application layer; optionally add DB CHECK constraints to prevent invalid `status` values.
- Index the FK columns and `status` columns for query performance.
- Use NOT NULL where applicable and validate amounts > 0.

8) Example SQL queries

Order timeline (latest first):

SELECT osh.created_at, osh.actor, osh.previous_status, osh.new_status, osh.reason
FROM order_state_history osh
WHERE osh.order_id = 'ORDER_UUID_HERE'
ORDER BY osh.created_at DESC;

Fetch returns for an order with their latest status:

SELECT rr.id, rr.status, rr.requested_at, rr.refund_amount
FROM return_requests rr
WHERE rr.order_id = 'ORDER_UUID_HERE'
ORDER BY rr.requested_at DESC;

Return timeline for a specific return:

SELECT rsh.created_at, rsh.actor, rsh.previous_status, rsh.new_status, rsh.reason
FROM return_state_history rsh
WHERE rsh.return_id = 'RETURN_UUID_HERE'
ORDER BY rsh.created_at ASC;

Find orders waiting for invoice generation (example):

SELECT id, status, updated_at
FROM orders
WHERE status = 'SHIPPED'
AND updated_at > now() - interval '24 hours';

9) Appendix

Enums (examples)

OrderStatus {
  PENDING_PAYMENT,
  PAID,
  PROCESSING_IN_WAREHOUSE,
  SHIPPED,
  DELIVERED,
  CANCELLED
}

ReturnStatus {
  REQUESTED,
  APPROVED,
  REJECTED,
  IN_TRANSIT,
  RECEIVED,
  COMPLETED
}

Sample JSON payloads

InvoiceRequestDto example

{
  "jobId": "11111111-1111-1111-1111-111111111111",
  "orderId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "customerEmail": "buyer@example.com",
  "items": [ {"sku":"SKU-1","name":"Handmade Vase","qty":1,"price":199.99} ],
  "shippedAt": "2026-01-04T12:00:00Z"
}

RefundRequestDto example

{
  "jobId": "22222222-2222-2222-2222-222222222222",
  "returnId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "orderId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "amount": 199.99,
  "paymentReference": "PAY-REF-12345",
  "requestedBy": "support@articurated.com"
}

Operational recommendations (short)
- Publish jobs only after the DB transaction commits. Use TransactionSynchronization or an outbox table + poller.
- Ensure listeners are idempotent and mark work in `job_log` atomically.
- Use DLQs and provide operational runbooks for retrying failed refunds/invoices.
- Add integration tests (Testcontainers) to verify end-to-end: order->ship->invoice job->PdfService and return->completed->refund job->RefundClient.

If you'd like, I will:
- Add an ASCII state diagram PNG or Mermaid diagram file.
- Create a small SQL migration (Flyway) script containing the above DDL.
- Add example listeners or a sample `outbox` table and a poller.


