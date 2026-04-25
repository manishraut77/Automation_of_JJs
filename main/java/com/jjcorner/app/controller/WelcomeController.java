package com.jjcorner.app.controller;

import com.jjcorner.app.nav.SceneManager;
import javafx.event.ActionEvent;

public final class WelcomeController {
    public void onLogin(ActionEvent event) {
        SceneManager.goToLogin();
    }

    public void onSignUp(ActionEvent event) {
        SceneManager.goToSignUp();
    }
}

