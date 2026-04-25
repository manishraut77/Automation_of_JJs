package com.jjcorner.app;

import com.jjcorner.app.service.AuthService;
import com.jjcorner.app.service.ClockService;
import com.jjcorner.app.service.MenuService;
import com.jjcorner.app.service.OrderService;
import com.jjcorner.app.service.SessionManager;
import com.jjcorner.app.service.TableService;

public final class AppContext {
    private static final SessionManager sessionManager = new SessionManager();
    private static final AuthService authService = new AuthService();
    private static final ClockService clockService = new ClockService(sessionManager);
    private static final TableService tableService = new TableService(sessionManager);
    private static final MenuService menuService = new MenuService();
    private static final OrderService orderService = new OrderService(sessionManager, tableService, menuService);

    private AppContext() {}

    public static SessionManager session() {
        return sessionManager;
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

    public static OrderService orders() {
        return orderService;
    }
}

