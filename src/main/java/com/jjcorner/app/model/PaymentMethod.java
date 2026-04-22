package com.jjcorner.app.model;

public enum PaymentMethod {
    CASH,
    CARD,
    /** System use: zero remaining balance after closed-check refund. */
    ADJUSTMENT
}

