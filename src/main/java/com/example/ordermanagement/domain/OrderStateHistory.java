package com.example.ordermanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_state_history")
public class OrderStateHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private OrderStatus fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state")
    private OrderStatus toState;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public OrderStateHistory() {
        // JPA
    }

    public OrderStateHistory(UUID id, Order order, OrderStatus fromState, OrderStatus toState, String changedBy, String reason) {
        this.id = id;
        this.order = order;
        this.fromState = fromState;
        this.toState = toState;
        this.changedBy = changedBy;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    // getters/setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderStatus getFromState() {
        return fromState;
    }

    public void setFromState(OrderStatus fromState) {
        this.fromState = fromState;
    }

    public OrderStatus getToState() {
        return toState;
    }

    public void setToState(OrderStatus toState) {
        this.toState = toState;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

