package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class LoginScreen extends VBox {

    private final TextField usernameField = new TextField();
    private final Button loginButton = new Button("Login");

    public LoginScreen(String suggestedName, Consumer<String> onLogin) {
        setSpacing(10);
        setPadding(new Insets(20));

        usernameField.setPromptText("Enter your username");
        if (suggestedName != null && !suggestedName.isBlank()) {
            usernameField.setText(suggestedName);
        }

        getChildren().addAll(new Label("Login"), usernameField, loginButton);

        loginButton.setOnAction(e -> {
            String enteredName = usernameField.getText().trim();
            if (!enteredName.isEmpty()) {
                onLogin.accept(enteredName);
            } else {
                // fallback: use system name or generated name
                String fallback = suggestedName != null ? suggestedName : System.getProperty("user.name");
                onLogin.accept(fallback);
            }
        });
    }
}
