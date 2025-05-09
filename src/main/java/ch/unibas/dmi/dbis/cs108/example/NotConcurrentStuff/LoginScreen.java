package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.Cube.CubeDrawer;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GameUIComponents;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LoginScreen zeigt ein Textfeld zur Eingabe des Benutzernamens
 * und startet nach Login Client, GameContext und GUI.
 */
public class LoginScreen extends VBox {

    private final TextField usernameField = new TextField();
    private final Button loginButton = new Button("Login");

    /**
     * @param primaryStage  Die Hauptbühne für den Wechsel zur Spielszene
     * @param prefilledName Ein vorgegebener Username oder Leerstring
     */
    public LoginScreen(Stage primaryStage, String prefilledName) {
        setSpacing(10);
        setPadding(new Insets(20));

        CubeDrawer drawer = new CubeDrawer();
        Canvas cubeCanvas = GameUIComponents.createCubeCanvas(drawer);
        VBox.setVgrow(cubeCanvas, Priority.ALWAYS);
        getChildren().add(cubeCanvas);

        // Entscheide, ob übergebener Name oder Nickname
        String baseName = (prefilledName == null || prefilledName.isBlank())
                ? Nickname_Generator.generateNickname()
                : prefilledName;

        usernameField.setPromptText("Enter your username");
        usernameField.setText(baseName);

        getChildren().addAll(new Label("Login"), usernameField, loginButton);

        loginButton.setOnAction(e -> {
            // Lese und valide eingetippten Namen
            String input = usernameField.getText().trim();
            String desired = input.isEmpty() ? baseName : input;

            // Erzeuge GameContext VOR der Duplikat-Prüfung, damit gameContext nicht null ist
            GameContext context = new GameContext();
            ThinkOutsideTheRoom.gameContext = context;

            // Prüfe auf Duplikate über GameContext.getGameObjects()
            String finalName = resolveDuplicate(desired);

            // Setze Client-Username
            Client client = Client.getInstance();
            client.setUsername(finalName);

            // Starte Netzwerk-Client
            new Thread(client::run).start();

            // Wechsle zur Spiel-GUI
            Platform.runLater(() -> {
                context.start();
                Platform.runLater(context::startGameLoop);

                // Aufbau der Spielszene
                CentralGraphicalUnit cgu = CentralGraphicalUnit.getInstance();
                Scene gameScene = new Scene(cgu.getMainContainer(), 800, 800);
                primaryStage.setScene(gameScene);
                primaryStage.setTitle("Game: " + finalName);
                cgu.getMainContainer().requestFocus();
            });
        });
    }

    /**
     * Hängt eine laufende Nummer im Format ### an, falls der Name bereits existiert.
     * Nutzt GameContext.getGameObjects()!
     */
    private String resolveDuplicate(String baseName) {
        // Hole aktuelle Namen aus dem GameContext (jetzt garantiert nicht null)
        List<String> existingNames = ThinkOutsideTheRoom.gameContext.getGameObjects().stream()
                .map(GameObject::getName)
                .collect(Collectors.toList());

        if (!existingNames.contains(baseName)) {
            return baseName;
        }
        // iteriere Suffix von 001 bis ...
        for (int i = 1; i < 1000; i++) {
            String candidate = String.format("%s%03d", baseName, i);
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }
        // Fallback, falls alle belegt sind
        return baseName + System.currentTimeMillis();
    }
}
