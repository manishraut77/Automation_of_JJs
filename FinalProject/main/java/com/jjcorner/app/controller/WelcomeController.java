package com.jjcorner.app.controller;

import com.jjcorner.app.nav.SceneManager;
import javafx.event.ActionEvent;

/**
 * Handles navigation from the welcome screen to login or sign-up.
 */
public final class WelcomeController {
    public void onLogin(ActionEvent event) {
        SceneManager.goToLogin();
    }

    public void onSignUp(ActionEvent event) {
        SceneManager.goToSignUp();
    }
}

