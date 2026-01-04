package com.example.ordermanagement.domain;

public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    IN_TRANSIT,
    RECEIVED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(ReturnStatus target) {
        if (this == target) return true;
        return switch (this) {
            case REQUESTED -> target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED -> target == IN_TRANSIT || target == CANCELLED;
            case IN_TRANSIT -> target == RECEIVED || target == CANCELLED;
            case RECEIVED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED -> false;
            case REJECTED -> false;
            case CANCELLED -> false;
        };
    }
}

