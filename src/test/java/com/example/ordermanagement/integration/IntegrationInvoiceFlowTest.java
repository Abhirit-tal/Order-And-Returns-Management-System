package com.example.ordermanagement.integration;

import com.example.ordermanagement.domain.JobLog;
import com.example.ordermanagement.domain.JobStatus;
import com.example.ordermanagement.domain.Order;
import com.example.ordermanagement.domain.OrderStatus;
import com.example.ordermanagement.repository.JobLogRepository;
import com.example.ordermanagement.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Testcontainers
public class IntegrationInvoiceFlowTest {

    @Container
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("orderdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbit.getAmqpPort());
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // use a temp dir for invoices
        registry.add("app.invoices.path", () -> System.getProperty("java.io.tmpdir") + File.separator + "test-invoices");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private JobLogRepository jobLogRepository;

    @Test
    @Timeout(value = 120)
    public void invoiceJobIsProcessedAndPdfCreated() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available; skipping integration test");

        // ensure invoices directory is clean
        String invoicesPath = System.getProperty("java.io.tmpdir") + File.separator + "test-invoices";
        File invoicesDir = new File(invoicesPath);
        if (invoicesDir.exists()) {
            File[] files = invoicesDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    // best-effort deletion
                    try { f.delete(); } catch (Exception ignored) {}
                }
            }
        } else {
            invoicesDir.mkdirs();
        }

        // create order
        Order order = orderService.createOrder("int-ext-1", "int@example.com", java.math.BigDecimal.valueOf(123.45));

        // progress to shipped (will enqueue invoice job)
        orderService.changeOrderStatus(order.getId(), OrderStatus.PAID, "test", "pay");
        orderService.changeOrderStatus(order.getId(), OrderStatus.PROCESSING_IN_WAREHOUSE, "test", "proc");
        orderService.changeOrderStatus(order.getId(), OrderStatus.SHIPPED, "test", "ship");

        // poll JobLog repository for invoice job related to this order
        UUID orderId = order.getId();
        JobLog job = waitForJobForOrder(orderId, Duration.ofSeconds(60));
        Assertions.assertNotNull(job, "Job should be created for the order");

        // wait until job becomes SUCCESS
        boolean success = waitForJobStatus(job.getId(), JobStatus.SUCCESS, Duration.ofSeconds(60));
        Assertions.assertTrue(success, "Invoice job should complete successfully");

        // check that a pdf file exists in invoices path
        File dir = new File(invoicesPath);
        Assertions.assertTrue(dir.exists() && dir.isDirectory(), "Invoices directory should exist");

        File[] pdfs = dir.listFiles((d, name) -> name.endsWith(".pdf") && name.contains(orderId.toString()));
        Assertions.assertNotNull(pdfs);
        Assertions.assertTrue(pdfs.length >= 1, "Expected at least one PDF invoice for the order");
    }

    private JobLog waitForJobForOrder(UUID orderId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            List<JobLog> all = jobLogRepository.findAll();
            Optional<JobLog> oj = all.stream().filter(j -> orderId.equals(j.getRelatedOrderId())).findFirst();
            if (oj.isPresent()) return oj.get();
            Thread.sleep(500);
        }
        return null;
    }

    private boolean waitForJobStatus(UUID jobId, JobStatus target, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Optional<JobLog> oj = jobLogRepository.findById(jobId);
            if (oj.isPresent() && oj.get().getStatus() == target) return true;
            Thread.sleep(500);
        }
        return false;
    }
}
