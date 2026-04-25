package com.jjcorner.app.model;

/**
 * Lifecycle states for a ticket from draft through payment.
 */
public enum OrderStatus {
    DRAFT,
    PENDING,
    IN_PROGRESS,
    READY,
    DELIVERED,
    PAID
}

