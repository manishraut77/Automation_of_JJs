package com.jjcorner.app.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Persists ingredient and supply stock counts for manager inventory.
 */
public final class InventoryLedger {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private InventoryLedger() {}

    private static Path ledgerPath() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".jjcorner-pos").resolve("inventory.json");
    }

    public static Map<String, Integer> load() {
        Path p = ledgerPath();
        if (!Files.exists(p)) {
            return new HashMap<>();
        }
        try {
            RootDto root = MAPPER.readValue(p.toFile(), RootDto.class);
            if (root == null) return new HashMap<>();
            if (root.stockByIngredient != null) return new HashMap<>(root.stockByIngredient);
            if (root.stockByMenuId != null) return new HashMap<>(root.stockByMenuId);
            return new HashMap<>();
        } catch (IOException ex) {
            return new HashMap<>();
        }
    }

    public static void save(Map<String, Integer> stockByIngredient) {
        Objects.requireNonNull(stockByIngredient);
        Path p = ledgerPath();
        try {
            Files.createDirectories(p.getParent());
            RootDto root = new RootDto();
            root.schema = 1;
            root.stockByIngredient = new HashMap<>(stockByIngredient);
            MAPPER.writeValue(p.toFile(), root);
        } catch (IOException ignored) {
        }
    }

    public static final class RootDto {
        public int schema;
        public Map<String, Integer> stockByIngredient;
        /** Backward-compatible field name from the earlier menu-item inventory prototype. */
        public Map<String, Integer> stockByMenuId;
    }
}
