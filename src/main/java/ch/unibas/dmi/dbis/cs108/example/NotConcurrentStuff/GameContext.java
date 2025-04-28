package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.UIManager;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GameUIComponents;
import ch.unibas.dmi.dbis.cs108.example.highscore.LevelTimer;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Node;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;

/**
 * The {@code GameContext} class manages the game session, user interaction, and game updates.
 * It is responsible for handling incoming messages, updating game state, drawing the game, and managing game sessions.
 */
@Getter
public class GameContext {

    // Singleton instance.
    @Getter
    private static GameContext instance;

    private final GameSessionManager gameSessionManager;
    private MessageHogger testHogger;

    // Make the current game ID and the selected game object's ID static.
    private static final AtomicReference<String> currentGameId = new AtomicReference<>();
    private static final AtomicReference<String> selectedGameObjectId = new AtomicReference<>();

    // Create a UIManager instance.
    private final UIManager uiManager = new UIManager();

    // Add a field to store the last frame time (in milliseconds).
    private long lastFrameTime = 0;

    // Adjustable FPS fields.
    private int targetFPS = 20;
    private long frameIntervalMs = 1000L / targetFPS; // (For 60 fps, roughly 16 ms per frame)

    private Set<KeyCode> prevPressedKeys = new HashSet<>();

    /**
     * Constructs a new {@code GameContext} and initializes the game session manager,
     * UI manager, and message handler (MessageHogger).
     */
    public GameContext() {
        instance = this;
        this.gameSessionManager = new GameSessionManager();

        instance = this;

        // Initialize the custom MessageHogger.
        testHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message receivedMessage) {
                String type   = receivedMessage.getMessageType();
                String option = receivedMessage.getOption();   // e.g. "REQUEST", "RESPONSE", "GAME"
                String uuid   = receivedMessage.getUUID();

                // Handle specific message types here...
                // This section processes various message types like "REGISTER", "CREATEGAME", "JOINGAME", etc.
                // Each message type corresponds to different game actions like user registration, game creation, etc.
            }
        };
    }

    /**
     * Returns the ID of the current game.
     *
     * @return The current game ID.
     */
    public static String getCurrentGameId() {
        return currentGameId.get();
    }

    /**
     * Returns the ID of the selected game object.
     *
     * @return The selected game object ID.
     */
    public static String getSelectedGameObjectId() {
        return selectedGameObjectId.get();
    }

    /**
     * Returns the game session manager that manages all game sessions.
     *
     * @return The {@code GameSessionManager} instance.
     */
    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }

    /**
     * Starts the client operations and the game loop.
     * Initializes the UI components and prepares the game environment.
     */
    public void start() {
        uiManager.waitForCentralUnitAndInitialize(() -> {
            Pane mainUIPane = GameUIComponents.createMainUIPane(uiManager, gameSessionManager);
            uiManager.registerComponent("mainUIPane", mainUIPane);

            // Additional UI components like chat panes, admin panels, and game selection...
        });
    }

    /**
     * Starts the game loop for updating and drawing the game state.
     * Uses JavaFX's {@code AnimationTimer} to continuously update and draw.
     */
    public void startGameLoop() {
        AsyncManager.runLoop(() -> {
            // 1) update your game state off‑FX
            update();
        });
        AnimationTimer timer = new AnimationTimer() {
            GraphicsContext gc = CentralGraphicalUnit.getInstance().getGraphicsContext();

            @Override
            public void handle(long now) {
                update();
                draw(gc);
                long elapsedTime = LevelTimer.getInstance().getElapsedTimeInSeconds();
                Platform.runLater(() -> {
                    // Update GUI element with the elapsed time
                });
            }
        };
        timer.start();
    }

    /**
     * Updates the game state by processing key presses and updating game objects.
     * Throttles updates based on the configured FPS.
     */
    public void update() {
        long now = System.currentTimeMillis();
        // Use the adjustable frame interval (target FPS).
        if (now - lastFrameTime < frameIntervalMs) {
            return;  // Not enough time has passed.
        }
        lastFrameTime = now;

        Set<KeyCode> currentPressedKeys = KeyboardState.getPressedKeys();

        // Send KEY_PRESS messages for each key pressed.
        for (KeyCode key : currentPressedKeys) {
            Message keyPressMsg = new Message("KEY_PRESS", new Object[]{ key.toString() }, "GAME");

            String[] concealed = keyPressMsg.getConcealedParameters();
            if (concealed == null || concealed.length < 2) {
                concealed = new String[2];
            }
            concealed[0] = getSelectedGameObjectId();
            concealed[1] = getCurrentGameId();
            keyPressMsg.setConcealedParameters(concealed);

            Client.sendMessageBestEffort(keyPressMsg);
        }

        Set<KeyCode> newlyReleased = new HashSet<>(prevPressedKeys);
        newlyReleased.removeAll(currentPressedKeys);

        // (Other game state update logic can follow here.)

        prevPressedKeys = new HashSet<>(currentPressedKeys);
    }

    /**
     * Draws the current game state on the provided {@code GraphicsContext}.
     * This method is called during each frame to render the game.
     *
     * @param gc The {@code GraphicsContext} used to draw the game.
     */
    private void draw(GraphicsContext gc) {
        String gameId = currentGameId.get();
        if (gameId == null || gameId.isEmpty()) {
            return;  // No game chosen.
        }

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        Game game = gameSessionManager.getGameSession(gameId);
        if (game == null) {
            return;
        }

        game.draw(gc);
    }

    /**
     * Retrieves the game session by its ID.
     *
     * @param gameId The ID of the game session to retrieve.
     * @return The {@code Game} session associated with the provided ID.
     */
    public static Game getGameById(String gameId) {
        return getInstance().getGameSessionManager().getGameSession(gameId);
    }

    /**
     * Main entry point for launching the game context and the JavaFX application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        GameContext context = new GameContext();
        new Thread(() -> {
            context.start();
            Platform.runLater(context::startGameLoop);
        }).start();

        System.out.println("Launching JavaFX...");
        Application.launch(GUI.class, args);
    }
}
