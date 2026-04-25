package com.jjcorner.app.controller;

import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.nav.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    public void onBack(ActionEvent event) {
        SceneManager.goToWelcome();
    }

    public void onLogin(ActionEvent event) {
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();

        AppContext.auth().login(username, password)
                .ifPresentOrElse(emp -> {
                    AppContext.session().setDemoMode(false);
                    loginSuccess(emp);
                }, () -> errorLabel.setText("Error: Invalid username or password"));
    }

    public void onDemoLogin(ActionEvent event) {
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();

        AppContext.auth().login(username, password)
                .ifPresentOrElse(emp -> {
                    AppContext.session().setDemoMode(true);
                    loginSuccess(emp);
                }, () -> errorLabel.setText("Error: Invalid username or password"));
    }

    private void loginSuccess(Employee employee) {
        AppContext.session().setCurrentUser(employee);
        if (employee.role()== Role.WAITER) {
            AppContext.tables().ensureAssignmentsForCurrentUser();
        }
        SceneManager.goToRoleHome();
    }
}

