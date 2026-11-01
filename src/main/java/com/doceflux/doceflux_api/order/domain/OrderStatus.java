package com.doceflux.doceflux_api.order.domain;

public enum OrderStatus {
    PENDING,
    IN_PRODUCTION,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == IN_PRODUCTION || next == CANCELLED;
            case IN_PRODUCTION -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
