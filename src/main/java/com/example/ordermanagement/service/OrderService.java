package com.example.ordermanagement.service;

import com.example.ordermanagement.domain.*;
import com.example.ordermanagement.repository.OrderRepository;
import com.example.ordermanagement.repository.OrderStateHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStateHistoryRepository historyRepository;
    private final JobPublisherService jobPublisherService;

    public OrderService(OrderRepository orderRepository,
                        OrderStateHistoryRepository historyRepository,
                        JobPublisherService jobPublisherService) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.jobPublisherService = jobPublisherService;
    }

    @Transactional
    public Order createOrder(String externalId, String customerEmail, BigDecimal totalAmount) {
        Order order = new Order(UUID.randomUUID(), externalId, customerEmail, totalAmount, OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);
        // initial history entry
        OrderStateHistory h = new OrderStateHistory(UUID.randomUUID(), order, null, OrderStatus.PENDING_PAYMENT, "system", "order created");
        historyRepository.save(h);
        return order;
    }

    @Transactional
    public Order changeOrderStatus(UUID orderId, OrderStatus target, String actor, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderStatus from = order.getStatus();
        if (from == null) {
            from = OrderStatus.PENDING_PAYMENT;
        }
        if (!from.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid transition from " + from + " to " + target);
        }
        // create history
        OrderStateHistory history = new OrderStateHistory(UUID.randomUUID(), order, from, target, actor, reason);
        historyRepository.save(history);
        order.setStatus(target);
        orderRepository.save(order);

        // enqueue invoice generation when shipped
        if (target == OrderStatus.SHIPPED) {
            jobPublisherService.publishInvoiceJob(order);
        }

        return order;
    }

    public Optional<Order> findById(UUID id) {
        return orderRepository.findById(id);
    }
}

