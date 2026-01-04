# Order And Returns Management System

This project is a Spring Boot backend (Java 17) that manages orders and returns with state machines and background job processing using RabbitMQ.

## Local integration test (requires Docker)

The integration tests use Testcontainers to start RabbitMQ and Postgres. If you want to run the integration test locally without Testcontainers, you can start RabbitMQ and Postgres using Docker Compose.

Example docker-compose snippet (save as `docker-compose.yml` in project root):

```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

Start the services:

```powershell
docker-compose up -d
```

Run the integration test (or the single integration class using Maven):

```powershell
mvn -Dtest=com.example.ordermanagement.integration.IntegrationInvoiceFlowTest test
```

Notes:
- The integration test will be skipped automatically if Docker is not available on the machine running the tests.
- The test creates PDF invoices in the temporary directory under `test-invoices` (configurable via `app.invoices.path` property).

If you prefer Testcontainers (recommended for CI), make sure Docker is running and simply run:

```powershell
mvn test
```

or run the single test class as shown above.


