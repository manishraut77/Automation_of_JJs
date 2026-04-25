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

/**
 * Authenticates users and routes them to the screen for their role.
 */
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
                    try {
                        AppContext.session().setDemoMode(false);
                        loginSuccess(emp);
                    } catch (RuntimeException ex) {
                        AppContext.session().clear();
                        errorLabel.setText("Error: Could not open " + emp.role() + " screen.");
                    }
                }, () -> errorLabel.setText("Error: Invalid username or password"));
    }

    public void onDemoLogin(ActionEvent event) {
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();

        AppContext.auth().login(username, password)
                .ifPresentOrElse(emp -> {
                    try {
                        AppContext.session().setDemoMode(true);
                        loginSuccess(emp);
                    } catch (RuntimeException ex) {
                        AppContext.session().clear();
                        errorLabel.setText("Error: Could not open " + emp.role() + " screen.");
                    }
                }, () -> errorLabel.setText("Error: Invalid username or password"));
    }

    private void loginSuccess(Employee employee) {
        AppContext.session().setCurrentUser(employee);
        AppContext.activity().record(employee, "Logged in" + (AppContext.session().isDemoMode() ? " in demo mode" : ""));
        if (employee.role()== Role.WAITER) {
            AppContext.tables().ensureAssignmentsForCurrentUser();
        }
        SceneManager.goToRoleHome();
    }
}
