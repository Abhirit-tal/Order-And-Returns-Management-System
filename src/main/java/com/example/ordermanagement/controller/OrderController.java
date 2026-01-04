package com.example.ordermanagement.controller;

import com.example.ordermanagement.domain.Order;
import com.example.ordermanagement.domain.OrderStatus;
import com.example.ordermanagement.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> body) {
        String externalId = (String) body.getOrDefault("externalId", UUID.randomUUID().toString());
        String email = (String) body.getOrDefault("customerEmail", "customer@example.com");
        BigDecimal amount = new BigDecimal(body.getOrDefault("totalAmount", "0.0").toString());
        Order o = orderService.createOrder(externalId, email, amount);
        return ResponseEntity.ok(o);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Order> changeStatus(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        String target = body.get("status");
        OrderStatus status = OrderStatus.valueOf(target);
        Order o = orderService.changeOrderStatus(id, status, "api", "manual status change");
        return ResponseEntity.ok(o);
    }
}

