package com.jjcorner.app.nav;

import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.util.AlertHelper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Centralized navigation helper for switching between FXML screens.
 */
public final class SceneManager {
    private static Stage stage;

    private SceneManager() {}

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void goToWelcome() {
        setRoot(load("/com/jjcorner/view/Welcome.fxml"), 1100, 700);
    }

    public static void goToLogin() {
        setRoot(load("/com/jjcorner/view/Login.fxml"), 1100, 700);
    }

    public static void goToSignUp() {
        setRoot(load("/com/jjcorner/view/SignUp.fxml"), 1100, 780);
    }

    public static void goToRoleHome() {
        Role role = AppContext.session().requireRole();
        if (role == Role.WAITER) {
            setRoot(load("/com/jjcorner/view/waiter/WaiterHome.fxml"), 1280, 800);
        } else if (role == Role.BUSBOY) {
            setRoot(load("/com/jjcorner/view/busboy/BusboyHome.fxml"), 1280, 800);
        } else if (role==Role.MANAGER) {
            setRoot(load("/com/jjcorner/view/manager/ManagerHome.fxml"),1280,800);
        } else {
            AlertHelper.error("Unsupported role", "This build only includes Waiter and Busboy screens.");
            goToWelcome();
        }
    }

    public static void logout() {
        if (AppContext.session().isClockedIn()) {
            AlertHelper.error("Clock out required", "Error: Please clock out before logging out.");
            return;
        }
        if (AppContext.session().isDemoMode()) {
            AppContext.orders().resetDemoForCurrentUser();
            AppContext.tables().resetDemoForCurrentUser();
        }
        AppContext.session().clear();
        goToWelcome();
    }

    public static void switchUser() {
        if (AppContext.session().isDemoMode()) {
            AppContext.orders().resetDemoForCurrentUser();
            AppContext.tables().resetDemoForCurrentUser();
        }
        AppContext.session().lock();
        goToLogin();
    }

    private static void setRoot(Parent root, int width, int height) {
        Objects.requireNonNull(stage, "SceneManager not initialized");
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, width, height);
            scene.getStylesheets().add(Objects.requireNonNull(SceneManager.class.getResource("/com/jjcorner/view/styles.css")).toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        stage.sizeToScene();
    }

    private static Parent load(String resourcePath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(resourcePath));
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + resourcePath, e);
        }
    }
}

