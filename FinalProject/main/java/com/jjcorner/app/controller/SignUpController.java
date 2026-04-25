package com.jjcorner.app.controller;

import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.nav.SceneManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Creates new employee profiles from the public sign-up screen.
 */
public final class SignUpController implements Initializable {
    @FXML private TextField employeeIdField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roleBox.setItems(FXCollections.observableArrayList(Role.WAITER, Role.BUSBOY, Role.COOK, Role.MANAGER));
        roleBox.getSelectionModel().select(Role.WAITER);
    }

    public void onBack(ActionEvent event) {
        SceneManager.goToWelcome();
    }

    public void onSignUp(ActionEvent event) {
        errorLabel.setText("");
        try {
            AppContext.auth().signUp(
                    employeeIdField.getText(),
                    usernameField.getText(),
                    passwordField.getText(),
                    roleBox.getValue(),
                    displayNameField.getText()
            );
            SceneManager.goToLogin();
        } catch (IllegalArgumentException ex) {
            errorLabel.setText("Error: " + ex.getMessage());
        }
    }
}
