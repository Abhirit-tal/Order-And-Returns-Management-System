package com.example.ordermanagement.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING_IN_WAREHOUSE,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) return true;
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELLED;
            case PAID -> target == PROCESSING_IN_WAREHOUSE || target == CANCELLED;
            case PROCESSING_IN_WAREHOUSE -> target == SHIPPED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED -> false;
            case CANCELLED -> false;
        };
    }
}

