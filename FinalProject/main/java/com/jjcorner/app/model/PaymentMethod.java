package com.jjcorner.app.model;

/**
 * Supported payment and adjustment types for checkout and refunds.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    /** System use: zero remaining balance after closed-check refund. */
    ADJUSTMENT
}

