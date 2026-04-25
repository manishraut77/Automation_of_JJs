package com.jjcorner.app.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Payment {
    private final BigDecimal amount;
    private final PaymentMethod method;
    private final Instant at;

    public Payment(BigDecimal amount, PaymentMethod method) {
        this(amount, method, Instant.now());
    }

    public Payment(BigDecimal amount, PaymentMethod method, Instant at) {
        this.amount = Objects.requireNonNull(amount);
        this.method = Objects.requireNonNull(method);
        this.at = Objects.requireNonNull(at);
    }

    public BigDecimal amount() {
        return amount;
    }

    public PaymentMethod method() {
        return method;
    }

    public Instant at() {
        return at;
    }
}

