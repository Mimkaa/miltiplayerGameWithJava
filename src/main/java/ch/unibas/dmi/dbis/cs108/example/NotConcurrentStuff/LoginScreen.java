package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;


public class LoginScreen extends VBox {

    private final TextField usernameField = new TextField();
    private final Button loginButton = new Button("Login");


    public LoginScreen(Stage primaryStage, String prefilledName) {
        setSpacing(10);
        setPadding(new Insets(20));

        String baseName = (prefilledName == null || prefilledName.isBlank())
                ? Nickname_Generator.generateNickname()
                : prefilledName;

        usernameField.setPromptText("Enter your username");
        usernameField.setText(baseName);

        getChildren().addAll(new Label("Login"), usernameField, loginButton);

        loginButton.setOnAction(e -> {

            String input = usernameField.getText().trim();
            String desired = input.isEmpty() ? baseName : input;


            String finalName = resolveDuplicate(desired);


            Client client = Client.getInstance();
            client.setUsername(finalName);


            new Thread(client::run).start();


            GameContext context = new GameContext();
            ThinkOutsideTheRoom.gameContext = context;


            Platform.runLater(() -> {
                context.start();
                Platform.runLater(context::startGameLoop);


                CentralGraphicalUnit cgu = CentralGraphicalUnit.getInstance();
                Scene gameScene = new Scene(cgu.getMainContainer(), 800, 800);
                primaryStage.setScene(gameScene);
                primaryStage.setTitle("Game: " + finalName);
                cgu.getMainContainer().requestFocus();
            });
        });
    }

    private String resolveDuplicate(String baseName) {
        List<String> existingNames = ThinkOutsideTheRoom.gameContext.getPlayerNames();

        if (!existingNames.contains(baseName)) {
            return baseName;
        }
        for (int i = 1; i < 1000; i++) {
            String candidate = String.format("%s%03d", baseName, i);
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }

        return baseName + System.currentTimeMillis();
    }
}
