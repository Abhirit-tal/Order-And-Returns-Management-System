package com.example.ordermanagement.service;

import com.example.ordermanagement.domain.Order;
import com.example.ordermanagement.domain.OrderStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest(properties = "spring.profiles.active=local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private JobPublisherService jobPublisherService; // mock to prevent RabbitMQ calls in tests

    @Test
    public void testCreateAndTransitionToPaid() {
        Order o = orderService.createOrder("ext-1", "a@b.com", new BigDecimal("100.00"));
        Assertions.assertEquals(OrderStatus.PENDING_PAYMENT, o.getStatus());

        Order updated = orderService.changeOrderStatus(o.getId(), OrderStatus.PAID, "test", "payment received");
        Assertions.assertEquals(OrderStatus.PAID, updated.getStatus());
    }

    @Test
    public void testInvalidTransitionThrows() {
        Order o = orderService.createOrder("ext-2", "c@d.com", new BigDecimal("50.00"));
        // move to delivered first (simulate processed pipeline)
        orderService.changeOrderStatus(o.getId(), OrderStatus.PAID, "test", "payment");
        orderService.changeOrderStatus(o.getId(), OrderStatus.PROCESSING_IN_WAREHOUSE, "test", "proc");
        orderService.changeOrderStatus(o.getId(), OrderStatus.SHIPPED, "test", "ship");
        orderService.changeOrderStatus(o.getId(), OrderStatus.DELIVERED, "test", "deliver");

        Assertions.assertThrows(IllegalStateException.class, () -> {
            orderService.changeOrderStatus(o.getId(), OrderStatus.SHIPPED, "test", "invalid");
        });
    }
}
