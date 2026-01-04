package com.example.ordermanagement.service;

import com.example.ordermanagement.domain.*;
import com.example.ordermanagement.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderService orderService;

    public ReturnService(ReturnRequestRepository returnRequestRepository, OrderService orderService) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderService = orderService;
    }

    @Transactional
    public ReturnRequest createReturn(UUID orderId, String reason) {
        Optional<Order> orderOpt = orderService.findById(orderId);
        if (orderOpt.isEmpty()) throw new IllegalArgumentException("Order not found: " + orderId);
        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Return can only be initiated for delivered orders");
        }
        ReturnRequest rr = new ReturnRequest(UUID.randomUUID(), order, reason, ReturnStatus.REQUESTED);
        returnRequestRepository.save(rr);
        return rr;
    }

    @Transactional
    public ReturnRequest changeReturnStatus(UUID returnId, ReturnStatus target, String actor, String reason) {
        ReturnRequest rr = returnRequestRepository.findById(returnId).orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));
        ReturnStatus from = rr.getStatus();
        if (!from.canTransitionTo(target)) throw new IllegalStateException("Invalid transition from " + from + " to " + target);
        rr.setStatus(target);
        returnRequestRepository.save(rr);
        return rr;
    }
}

