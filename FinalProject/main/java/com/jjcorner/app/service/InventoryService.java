package com.jjcorner.app.service;

import com.jjcorner.app.model.MenuItem;
import com.jjcorner.app.persist.InventoryLedger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks ingredient-level inventory and estimates ingredient use from sold menu items.
 */
public final class InventoryService {
    private static final int DEFAULT_STOCK = 100;
    private final Map<String, Integer> stockByIngredient;

    public InventoryService(MenuService menu) {
        this.stockByIngredient = InventoryLedger.load();
        ensureKnownIngredients();
    }

    public int stockFor(MenuItem item) {
        if (item == null) return 0;
        return ingredientsFor(item).stream().mapToInt(i -> stockForIngredient(i.name())).min().orElse(0);
    }

    public void setStock(MenuItem item, int quantity) {
        if (item == null) {
            throw new IllegalArgumentException("Select a menu item.");
        }
        for (IngredientUse ingredient : ingredientsFor(item)) {
            stockByIngredient.put(ingredient.name(), Math.max(0, quantity));
        }
        persist();
    }

    public void adjustStock(MenuItem item, int delta) {
        if (item == null) return;
        for (IngredientUse ingredient : ingredientsFor(item)) {
            adjustIngredientStock(ingredient.name(), delta);
        }
    }

    public void removeItem(String menuItemId) {
    }

    public List<String> allIngredients() {
        ensureKnownIngredients();
        return stockByIngredient.keySet().stream().sorted().toList();
    }

    public int stockForIngredient(String ingredient) {
        if (ingredient == null || ingredient.isBlank()) return 0;
        return stockByIngredient.computeIfAbsent(ingredient, x -> DEFAULT_STOCK);
    }

    public void setIngredientStock(String ingredient, int quantity) {
        if (ingredient == null || ingredient.isBlank()) {
            throw new IllegalArgumentException("Ingredient is required.");
        }
        stockByIngredient.put(ingredient.trim(), Math.max(0, quantity));
        persist();
    }

    public void adjustIngredientStock(String ingredient, int delta) {
        if (ingredient == null || ingredient.isBlank()) return;
        setIngredientStock(ingredient, stockForIngredient(ingredient) + delta);
    }

    public void consumeForMenuItem(MenuItem item, int quantity) {
        if (item == null || quantity <= 0) return;
        for (IngredientUse ingredient : ingredientsFor(item)) {
            int units = Math.max(1, ingredient.unitsPerMenuItem()) * quantity;
            adjustIngredientStock(ingredient.name(), -units);
        }
    }

    public List<IngredientUse> ingredientsFor(MenuItem item) {
        if (item == null) return List.of();
        String id = item.id().toUpperCase();
        String name = item.name().toLowerCase();
        List<IngredientUse> out = new ArrayList<>();
        if (name.contains("chicken")) out.add(new IngredientUse("Chicken", "portions", 1));
        if (name.contains("pork")) out.add(new IngredientUse("Pork", "portions", 1));
        if (name.contains("shrimp")) out.add(new IngredientUse("Shrimp", "portions", 1));
        if (name.contains("catfish")) out.add(new IngredientUse("Catfish", "portions", 1));
        if (name.contains("grouper")) out.add(new IngredientUse("Grouper", "portions", 1));
        if (name.contains("tuna")) out.add(new IngredientUse("Tuna", "portions", 1));
        if (name.contains("steak") || name.contains("strip")) out.add(new IngredientUse("Steak", "portions", 1));
        if (name.contains("burger")) out.add(new IngredientUse("Burger patties", "each", 1));
        if (name.contains("boca")) out.add(new IngredientUse("Vegan patties", "each", 1));
        if (name.contains("salad") || name.contains("lettuce") || id.startsWith("SAL")) out.add(new IngredientUse("Mixed greens", "servings", 1));
        if (name.contains("fries") || name.contains("potato")) out.add(new IngredientUse("Potatoes", "servings", 1));
        if (name.contains("grits")) out.add(new IngredientUse("Grits", "servings", 1));
        if (name.contains("mac") || name.contains("cheese") || name.contains("nachos")) out.add(new IngredientUse("Cheese", "servings", 1));
        if (name.contains("nachos")) out.add(new IngredientUse("Tortilla chips", "servings", 1));
        if (name.contains("sliders") || name.contains("sandwich") || name.contains("club") || name.contains("sub") || name.contains("philly")) {
            out.add(new IngredientUse("Bread and buns", "servings", 1));
        }
        if (name.contains("tea")) out.add(new IngredientUse("Tea concentrate", "servings", 1));
        if (name.contains("coke") || name.contains("sprite") || name.contains("lemonade") || name.contains("juice")) {
            out.add(new IngredientUse("Beverage syrup", "servings", 1));
        }
        if (name.contains("water")) out.add(new IngredientUse("Bottled water", "bottles", 1));
        if (out.isEmpty()) out.add(new IngredientUse("General pantry", "servings", 1));
        return out.stream()
                .collect(LinkedHashMap<String, IngredientUse>::new, (m, i) -> m.putIfAbsent(i.name(), i), Map::putAll)
                .values().stream().toList();
    }

    public Map<String, Integer> ingredientUsageFromTickets(List<com.jjcorner.app.model.Ticket> tickets) {
        Map<String, Integer> usage = new HashMap<>();
        for (var ticket : tickets) {
            for (var line : ticket.items()) {
                for (IngredientUse ingredient : ingredientsFor(line.menuItem())) {
                    usage.merge(ingredient.name(), ingredient.unitsPerMenuItem() * line.quantity(), Integer::sum);
                }
            }
        }
        return usage.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
    }

    public void ensureKnownIngredients() {
        boolean changed = false;
        for (String ingredient : defaultIngredients()) {
            if (!stockByIngredient.containsKey(ingredient)) {
                stockByIngredient.put(ingredient, DEFAULT_STOCK);
                changed = true;
            }
        }
        if (changed) {
            persist();
        }
    }

    private List<String> defaultIngredients() {
        return List.of(
                "Beverage syrup", "Bottled water", "Bread and buns", "Burger patties", "Catfish",
                "Cheese", "Chicken", "Grits", "Grouper", "Mixed greens", "Pork", "Potatoes",
                "Shrimp", "Steak", "Tea concentrate", "Tortilla chips", "Tuna", "Vegan patties",
                "General pantry"
        ).stream().sorted(Comparator.naturalOrder()).toList();
    }

    private void persist() {
        InventoryLedger.save(new HashMap<>(stockByIngredient));
    }

    public record IngredientUse(String name, String unit, int unitsPerMenuItem) {
    }
}
