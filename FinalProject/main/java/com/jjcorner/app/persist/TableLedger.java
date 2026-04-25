package com.jjcorner.app.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jjcorner.app.model.RestaurantTable;
import com.jjcorner.app.model.TableStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists table layout, status, seat counts, waiter assignments, and joined tables.
 */
public final class TableLedger {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private TableLedger() {}

    private static Path ledgerPath() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".jjcorner-pos").resolve("tables.json");
    }

    public static List<RestaurantTable> load() {
        Path p = ledgerPath();
        if (!Files.exists(p)) return List.of();
        try {
            RootDto root = MAPPER.readValue(p.toFile(), RootDto.class);
            if (root == null || root.tables == null) return List.of();
            List<RestaurantTable> out = new ArrayList<>();
            for (TableDto dto : root.tables) {
                if (dto == null || dto.id == null) continue;
                RestaurantTable table = new RestaurantTable(dto.id);
                table.setStatus(dto.status == null ? TableStatus.OPEN : dto.status);
                table.setAssignedWaiterId(dto.assignedWaiterId);
                table.setGuestCount(dto.guestCount);
                table.setSeatCount(dto.seatCount <= 0 ? 4 : dto.seatCount);
                table.setJoinedTableIds(dto.joinedTableIds);
                out.add(table);
            }
            return out;
        } catch (IOException ex) {
            return List.of();
        }
    }

    public static void save(List<RestaurantTable> tables) {
        Path p = ledgerPath();
        try {
            Files.createDirectories(p.getParent());
            RootDto root = new RootDto();
            root.schema = 1;
            root.tables = new ArrayList<>();
            for (RestaurantTable table : tables) {
                TableDto dto = new TableDto();
                dto.id = table.id();
                dto.status = table.status();
                dto.assignedWaiterId = table.assignedWaiterId();
                dto.guestCount = table.guestCount();
                dto.seatCount = table.seatCount();
                dto.joinedTableIds = table.joinedTableIds();
                root.tables.add(dto);
            }
            MAPPER.writeValue(p.toFile(), root);
        } catch (IOException ignored) {
        }
    }

    public static final class RootDto {
        public int schema;
        public List<TableDto> tables;
    }

    public static final class TableDto {
        public String id;
        public TableStatus status;
        public String assignedWaiterId;
        public int guestCount;
        public int seatCount;
        public String joinedTableIds;
    }
}
