package com.jjcorner.app;

import com.jjcorner.app.service.ActivityService;
import com.jjcorner.app.service.AuthService;
import com.jjcorner.app.service.ClockService;
import com.jjcorner.app.service.InventoryService;
import com.jjcorner.app.service.MenuService;
import com.jjcorner.app.service.OrderService;
import com.jjcorner.app.service.SessionManager;
import com.jjcorner.app.service.TableService;

/**
 * Simple application-wide service registry used by the JavaFX controllers.
 */
public final class AppContext {
    private static final SessionManager sessionManager = new SessionManager();
    private static final ActivityService activityService = new ActivityService();
    private static final AuthService authService = new AuthService();
    private static final ClockService clockService = new ClockService(sessionManager, activityService);
    private static final TableService tableService = new TableService(sessionManager, activityService);
    private static final MenuService menuService = new MenuService();
    private static final InventoryService inventoryService = new InventoryService(menuService);
    private static final OrderService orderService = new OrderService(sessionManager, tableService, menuService, inventoryService, activityService);

    private AppContext() {}

    public static SessionManager session() {
        return sessionManager;
    }

    public static ActivityService activity() {
        return activityService;
    }

    public static AuthService auth() {
        return authService;
    }

    public static ClockService clock() {
        return clockService;
    }

    public static TableService tables() {
        return tableService;
    }

    public static MenuService menu() {
        return menuService;
    }

    public static InventoryService inventory() {
        return inventoryService;
    }

    public static OrderService orders() {
        return orderService;
    }
}

