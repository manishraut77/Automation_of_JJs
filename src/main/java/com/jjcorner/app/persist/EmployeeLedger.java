package com.jjcorner.app.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.Role;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EmployeeLedger {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private EmployeeLedger() {}

    private static Path ledgerPath() {
        String home = System.getProperty("user.home");
        Path dir = Path.of(home, ".jjcorner-pos");
        return dir.resolve("employees.json");
    }

    public static List<Employee> load() {
        Path p = ledgerPath();
        if (!Files.exists(p)) {
            return List.of();
        }
        try {
            RootDto root = MAPPER.readValue(p.toFile(), RootDto.class);
            if (root == null || root.employees == null) return List.of();
            List<Employee> out = new ArrayList<>();
            for (EmpDto e : root.employees) {
                if (e == null) continue;
                if (e.employeeId == null || e.username == null || e.password == null || e.role == null) continue;
                String display = e.displayName == null ? "" : e.displayName;
                out.add(new Employee(e.employeeId, e.username, e.password, e.role, display));
            }
            return out;
        } catch (IOException ex) {
            return List.of();
        }
    }

    public static void save(List<Employee> employees) {
        Objects.requireNonNull(employees);
        Path p = ledgerPath();
        try {
            Files.createDirectories(p.getParent());
            RootDto root = new RootDto();
            root.schema = 1;
            root.employees = new ArrayList<>();
            for (Employee e : employees) {
                if (e == null) continue;
                EmpDto dto = new EmpDto();
                dto.employeeId = e.employeeId();
                dto.username = e.username();
                dto.password = e.password();
                dto.role = e.role();
                dto.displayName = e.displayName();
                root.employees.add(dto);
            }
            MAPPER.writeValue(p.toFile(), root);
        } catch (IOException ignored) {
        }
    }

    public static final class RootDto {
        public int schema;
        public List<EmpDto> employees;
    }

    public static final class EmpDto {
        public String employeeId;
        public String username;
        public String password;
        public Role role;
        public String displayName;
    }
}

