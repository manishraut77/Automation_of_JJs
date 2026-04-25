package com.jjcorner.app;

import com.jjcorner.app.nav.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the JJ's Corner desktop application.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        SceneManager.init(stage);
        SceneManager.goToWelcome();
        stage.setTitle("JJ's Corner Restaurant - Automation");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

