package com.jjcorner.app.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class MenuItem {
    private final String id;
    private final String name;
    private final MenuCategory category;
    private final BigDecimal price;
    private final String description;

    public MenuItem(String id, String name, MenuCategory category, BigDecimal price) {
        this(id, name, category, price, "");
    }

    public MenuItem(String id, String name, MenuCategory category, BigDecimal price, String description) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.category = Objects.requireNonNull(category);
        this.price = Objects.requireNonNull(price);
        this.description = description == null ? "" : description;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public MenuCategory category() {
        return category;
    }

    public BigDecimal price() {
        return price;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }
}

