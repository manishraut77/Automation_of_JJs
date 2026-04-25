package com.jjcorner.app.model;

/**
 * Employee profile for users with administrative and override permissions.
 */
public final class Manager extends Employee {
    public Manager(String employeeId, String username, String password, String displayName) {
        super(employeeId, username, password, Role.MANAGER, displayName);
    }
}
