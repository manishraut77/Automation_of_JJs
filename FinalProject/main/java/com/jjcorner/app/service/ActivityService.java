package com.jjcorner.app.service;

import com.jjcorner.app.model.Employee;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/**
 * Appends human-readable employee actions to a local activity log.
 */
public final class ActivityService {
    private static Path logPath() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".jjcorner-pos").resolve("activity.log");
    }

    public void record(Employee employee, String action) {
        if (employee == null || action == null || action.isBlank()) {
            return;
        }
        String line = Instant.now() + " | " + employee.employeeId() + " | " + employee.username()
                + " | " + employee.role() + " | " + action.strip() + System.lineSeparator();
        try {
            Path p = logPath();
            Files.createDirectories(p.getParent());
            Files.writeString(p, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    public List<String> recent(int limit) {
        List<String> lines = all();
        int from = Math.max(0, lines.size() - Math.max(1, limit));
        return lines.subList(from, lines.size()).reversed();
    }

    public List<String> all() {
        Path p = logPath();
        if (!Files.exists(p)) {
            return List.of();
        }
        try {
            return Files.readAllLines(p);
        } catch (IOException ex) {
            return List.of();
        }
    }
}
