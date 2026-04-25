package com.jjcorner.app.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jjcorner.app.model.MenuCategory;
import com.jjcorner.app.model.MenuItem;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persists manager-edited menu items to local JSON storage.
 */
public final class MenuLedger {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private MenuLedger() {}

    private static Path ledgerPath() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".jjcorner-pos").resolve("menu.json");
    }

    public static List<MenuItem> load() {
        Path p = ledgerPath();
        if (!Files.exists(p)) {
            return List.of();
        }
        try {
            RootDto root = MAPPER.readValue(p.toFile(), RootDto.class);
            if (root == null || root.items == null) return List.of();
            List<MenuItem> out = new ArrayList<>();
            for (MenuDto i : root.items) {
                if (i == null || i.id == null || i.name == null || i.category == null || i.price == null) continue;
                out.add(new MenuItem(i.id, i.name, i.category, i.price, i.description));
            }
            return out;
        } catch (IOException ex) {
            return List.of();
        }
    }

    public static void save(List<MenuItem> items) {
        Objects.requireNonNull(items);
        Path p = ledgerPath();
        try {
            Files.createDirectories(p.getParent());
            RootDto root = new RootDto();
            root.schema = 1;
            root.items = new ArrayList<>();
            for (MenuItem item : items) {
                if (item == null) continue;
                MenuDto dto = new MenuDto();
                dto.id = item.id();
                dto.name = item.name();
                dto.category = item.category();
                dto.price = item.price();
                dto.description = item.description();
                root.items.add(dto);
            }
            MAPPER.writeValue(p.toFile(), root);
        } catch (IOException ignored) {
        }
    }

    public static final class RootDto {
        public int schema;
        public List<MenuDto> items;
    }

    public static final class MenuDto {
        public String id;
        public String name;
        public MenuCategory category;
        public BigDecimal price;
        public String description;
    }
}
