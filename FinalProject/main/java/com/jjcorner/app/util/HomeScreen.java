package com.jjcorner.app.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Shared logged-in home screen with restaurant hours and contact information.
 */
public final class HomeScreen {
    private HomeScreen() {}

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("JJ's Corner - Home");

        ImageView logo = new ImageView();
        var stream = HomeScreen.class.getResourceAsStream("/com/jjcorner/view/jj-logo.png");
        if (stream != null) {
            logo.setImage(new Image(stream));
        }
        logo.setFitWidth(120);
        logo.setFitHeight(120);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        Label title = new Label("JJ's Corner Restaurant");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label hoursTitle = sectionTitle("Hours of Operation");
        Label hours = body("""
                Monday: 11:00 AM - 9:30 PM
                Tuesday: 11:00 AM - 9:30 PM
                Wednesday: 11:30 AM - 9:30 PM
                Thursday: 11:00 AM - 9:30 PM
                Friday: 11:00 AM - 9:30 PM
                Saturday: 11:00 AM - 9:30 PM
                Sunday: Closed
                """);

        Label contactTitle = sectionTitle("Contact Information");
        Label contact = body("""
                Website: jscorner.com
                Phone: (470) 555-1212
                Address: 680 Arntson Dr., Marietta, GA 30060
                Payments accepted: Cash and credit card
                """);

        Label menuTitle = sectionTitle("Menu Overview");
        Label menu = body("""
                Appetizers, Salads, Entrees, Sides, Sandwiches, Burgers, and Beverages.
                Entrees are served with two sides. Sides can also be ordered separately for $2.50.
                All beverages are $2.00.
                """);

        Label noteTitle = sectionTitle("Service Notes");
        Label note = body("""
                Standard tables hold 1-4 customers.
                Joined tables share one check and use the combined seat count.
                Waiters mark ready tables occupied and paid tables dirty.
                Busboys primarily mark dirty tables ready.
                Refunds require manager approval.
                Inventory is checked weekly.
                """);

        Label staffTitle = sectionTitle("Staffing Reference");
        Label staff = body("""
                Planned staffing: 5 waiters, 2 busboys, 5 chefs, and 1 manager.
                The system tracks hours worked, not employee schedules.
                Employee IDs are six alphanumeric characters.
                """);

        Button close = new Button("Close");
        close.setDefaultButton(true);
        close.setOnAction(e -> stage.close());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12,
                logo, title,
                hoursTitle, hours,
                contactTitle, contact,
                menuTitle, menu,
                noteTitle, note,
                staffTitle, staff,
                actions);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(22));

        Scene scene = new Scene(root, 640, 760);
        var css = HomeScreen.class.getResource("/com/jjcorner/view/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return label;
    }

    private static Label body(String text) {
        Label label = new Label(text.strip());
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 14px;");
        return label;
    }
}
