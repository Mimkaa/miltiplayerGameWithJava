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
import javafx.scene.canvas.Canvas;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageRateMeter;
import ch.unibas.dmi.dbis.cs108.example.Cube.CubeDrawer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The {@code GameContext} class manages the game session, user interaction, and game updates.
 * It is responsible for handling incoming messages, updating game state, drawing the game, and managing game sessions.
 */
@Getter
public class GameContext {

    // Singleton instance.
    @Getter
    private static GameContext instance;

    public Game game;
    /*
    Add CubeDrawer as a field
    */
    private CubeDrawer cubeDrawer = new CubeDrawer();

    private double lastMouseX = 0;
    private double lastMouseY = 0;

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

    private boolean isRotating = false;
    public GameContext() {
        instance = this;
        this.gameSessionManager = new GameSessionManager();
        String id   = UUID.randomUUID().toString();
        String name = "Session-" + id.substring(0,5);
        this.game = new Game(id, name);


        // Initialize the custom MessageHogger.
        testHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message receivedMessage) {
                String type   = receivedMessage.getMessageType();
                String option = receivedMessage.getOption();   // e.g. "REQUEST", "RESPONSE", "GAME"
                String uuid   = receivedMessage.getUUID();

                if ("REGISTER".equals(type)) {
                    System.out.println("Processing REGISTER message");
                    // Retrieve concealed parameters.
                    String[] concealed = receivedMessage.getConcealedParameters();
                    if (concealed == null || concealed.length == 0) {
                        System.out.println("REGISTER message missing concealed parameters.");
                        return;
                    }
                    // The last concealed parameter is assumed to be the username.
                    String receivedUsername = concealed[concealed.length - 1];
                    // Get the current client's username.
                    String currentUsername = Client.getInstance().getUsername().get();

                    if (currentUsername.equals(receivedUsername)) {
                        System.out.println("Welcome: " + receivedUsername);
                    } else {
                        System.out.println("User joined: " + receivedUsername);
                    }

                }


                if ("CREATEGAME".equals(type)) {
                    System.out.println("Processing CREATEGAME response");
                    String receivedId = receivedMessage.getParameters()[0].toString();
                    String receivedGameName = receivedMessage.getParameters()[1].toString();
                    gameSessionManager.addGameSession(receivedId, receivedGameName);
                    System.out.println("Game created with id: " + receivedId);

                    // Update the ComboBox with the new game name.
                    Platform.runLater(() -> {
                        Node node = uiManager.getComponent("gameSelect");
                        if (node instanceof ComboBox) {
                            ComboBox<String> gameSelect = (ComboBox<String>) node;
                            if (!gameSelect.getItems().contains(receivedGameName)) {
                                gameSelect.getItems().add(receivedGameName);
                            }
                        }
                    });

                } else if ("JOINGAME".equals(type)) {
                    System.out.println("Processing JOINGAME response");
                    String gameID = receivedMessage.getParameters()[0].toString();
                    String username = receivedMessage.getParameters()[1].toString();
                    String prevGameId =  receivedMessage.getParameters()[2].toString();
                    if (Client.getInstance().getUsername().get().equals(username))
                    {
                        currentGameId.set(gameID);
                        System.out.println(receivedMessage);
                        System.out.println("Current game id set to: " + currentGameId.get());
                        Platform.runLater(() -> {
                            Node fieldNode = uiManager.getComponent("gameIdField");
                            if (fieldNode instanceof TextField) {
                                ((TextField) fieldNode).setText(gameID);
                            }
                        });
                    }

                    gameSessionManager.getGameSession(gameID).getUsers().add(username);
                    if(!prevGameId.equals("default"))
                    {
                        gameSessionManager.getGameSession(prevGameId).getUsers().remove(username);
                    }

                    // Update the TextArea with the current game info.
                    Platform.runLater(() -> {
                        Node usersListNode = uiManager.getComponent("usersListCurrGS");
                        if (usersListNode instanceof TextArea) {
                            Game game = gameSessionManager.getGameSession(currentGameId.get());
                            if (game != null) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Current Game Name: ").append(game.getGameName()).append("\n");
                                sb.append("Users in this game:\n");
                                for (String user : game.getUsers()) {
                                    sb.append(user).append("\n");
                                }
                                ((TextArea) usersListNode).setText(sb.toString());
                            }
                        }
                    });

                }
                else if ("SELECTGO".equals(type)) {
                    System.out.println("Processing SELECTGO command");
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 1) {
                        System.out.println("SELECTGO message missing target GameObject id.");
                        return;
                    }
                    String targetGameObjectId = receivedMessage.getParameters()[0].toString();
                    String gameId = currentGameId.get();
                    if (gameId == null) {
                        System.out.println("No current game id set.");
                        return;
                    }
                    Game game = gameSessionManager.getGameSession(gameId);
                    if (game != null) {
                        // Clear the selected flag in all game objects.
                        for (GameObject go : game.getGameObjects()) {
                            go.setSelected(false);
                        }

                        boolean found = false;
                        for (GameObject go : game.getGameObjects()) {
                            if (go.getId().equals(targetGameObjectId)) {
                                selectedGameObjectId.set(go.getId());
                                go.setSelected(true);
                                found = true;
                                System.out.println("Selected game object with id: " + go.getId());
                                break;
                            }
                        }
                        if (!found) {
                            System.out.println("No game object with id " + targetGameObjectId + " found in game " + gameId);
                        }
                    } else {
                        System.out.println("No game session found with id: " + gameId);
                    }
                }
                else if ("CREATEGO".equals(type)) {
                    System.out.println("Processing CREATEGO response");
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 3) {
                        System.out.println("CREATEGO message missing required parameters. Expected at least 3 parameters.");
                        return;
                    }
                    String newGameObjectUuid = receivedMessage.getParameters()[0].toString();
                    String targetGameId = receivedMessage.getParameters()[1].toString();
                    String objectType = receivedMessage.getParameters()[2].toString();
                    Game game = gameSessionManager.getGameSession(targetGameId);
                    if (game != null) {
                        Object[] constructorParams = new Object[0];
                        if (receivedMessage.getParameters().length > 3) {
                            constructorParams = Arrays.copyOfRange(
                                    receivedMessage.getParameters(), 3, receivedMessage.getParameters().length
                            );
                        }
                        Future<GameObject> futureObj = game.addGameObjectAsync(objectType, newGameObjectUuid, constructorParams);
                        try {
                            GameObject newObj = futureObj.get();
                            System.out.println("Created new game object with UUID: " + newGameObjectUuid
                                    + " and name: " + newObj.getName() + " in game session: " + targetGameId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("No game session found with id: " + targetGameId);
                    }

                } else if ("CHANGENAME".equals(type)) {
                    System.out.println("Processing CHANGENAME response");
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 3) {
                        System.out.println("CHANGENAME message missing required parameters. Expected at least 3 parameters.");
                        return;
                    }
                    String sessionId = receivedMessage.getParameters()[0].toString();
                    String objectId = receivedMessage.getParameters()[1].toString();
                    String newName = receivedMessage.getParameters()[2].toString();
                    Game game = gameSessionManager.getGameSession(sessionId);
                    if (game != null) {
                        boolean found = false;
                        for (GameObject go : game.getGameObjects()) {
                            if (go.getId().equals(objectId)) {
                                go.setName(newName);
                                found = true;
                                System.out.println("CHANGENAME processed: Updated GameObject " + objectId + " name to " + newName);
                                break;
                            }
                        }
                        if (!found) {
                            System.out.println("CHANGENAME: No game object with id " + objectId + " found in session " + sessionId);
                        }
                    } else {
                        System.out.println("CHANGENAME: No game session found with id: " + sessionId);
                    }

                } else if ("CHATGLB".equals(type)) {
                    System.out.println(receivedMessage);
                    if (receivedMessage.getParameters() != null && receivedMessage.getParameters().length >= 2) {
                        String sender = receivedMessage.getParameters()[0].toString();
                        String message = receivedMessage.getParameters()[1].toString();

                        Platform.runLater(() -> {
                            Node chatNode = uiManager.getComponent("chatMessagesBox");
                            if (chatNode instanceof VBox) {
                                VBox messagesBox = (VBox) chatNode;

                                Label msgLabel = new Label(sender + ": " + message);
                                msgLabel.setWrapText(true);
                                msgLabel.setStyle("-fx-background-color: #eeeeee; -fx-padding: 5; "
                                        + "-fx-border-radius: 5; -fx-background-radius: 5;");
                                messagesBox.getChildren().add(msgLabel);
                            }

                            Node scrollNode = uiManager.getComponent("chatScroll");
                            if (scrollNode instanceof ScrollPane) {
                                ((ScrollPane) scrollNode).setVvalue(1.0);
                            }
                        });
                    }

                } else if ("DELETEGO".equals(type)) {
                    System.out.println("Processing DELETEGO command");
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 2) {
                        System.out.println("DELETEGO message missing required parameters. Expected: [gameSessionId, objectId]");
                        return;
                    }
                    String sessionId = receivedMessage.getParameters()[0].toString();
                    String objectId = receivedMessage.getParameters()[1].toString();
                    Game game = gameSessionManager.getGameSession(sessionId);
                    if (game != null) {
                        boolean removed = game.getGameObjects().removeIf(go -> go.getId().equals(objectId));
                        if (removed) {
                            System.out.println("Deleted game object with id: " + objectId + " from session " + sessionId);
                        } else {
                            System.out.println("No game object with id " + objectId + " found in session " + sessionId);
                        }
                    } else {
                        System.out.println("No game session found with id: " + sessionId);
                    }

                } else if ("GETUSERS".equals(type)) {
                    Platform.runLater(() -> {
                        // Update the TextArea.
                        Node oldListNode = uiManager.getComponent("usersList");
                        if (oldListNode instanceof TextArea) {
                            TextArea usersListArea = (TextArea) oldListNode;
                            Object[] params = receivedMessage.getParameters();
                            usersListArea.clear();
                            for (Object param : params) {
                                usersListArea.appendText(param.toString() + "\n");
                            }
                        }

                        // Update the ComboBox for whispering.
                        Node comboNode = uiManager.getComponent("whisperUserSelect");
                        if (comboNode instanceof ComboBox) {
                            @SuppressWarnings("unchecked")
                            ComboBox<String> userSelect = (ComboBox<String>) comboNode;

                            // Clear existing items.
                            userSelect.getItems().clear();

                            // Get the current client’s username.
                            String currentUser = Client.getInstance().getUsername().get();

                            // Add each user except this client.
                            for (Object param : receivedMessage.getParameters()) {
                                String user = param.toString();
                                if (!user.equals(currentUser)) {
                                    userSelect.getItems().add(user);
                                }
                            }
                        }
                    });
                }
                else if ("WHISPER".equals(type)) {
                    System.out.println("Processing WHISPER message: " + receivedMessage);
                    if (receivedMessage.getParameters() != null && receivedMessage.getParameters().length >= 2) {
                        String sender = receivedMessage.getParameters()[0].toString();
                        String message = receivedMessage.getParameters()[1].toString();

                        Platform.runLater(() -> {
                            Node chatNode = uiManager.getComponent("whisperChatMessagesBox");
                            if (chatNode instanceof VBox) {
                                VBox messagesBox = (VBox) chatNode;
                                Label msgLabel = new Label(sender + ": " + message);
                                msgLabel.setWrapText(true);
                                msgLabel.setStyle("-fx-background-color: #eeeeee; -fx-padding: 5; "
                                        + "-fx-border-radius: 5; -fx-background-radius: 5;");
                                messagesBox.getChildren().add(msgLabel);
                            }

                            Node scrollNode = uiManager.getComponent("whisperChatScroll");
                            if (scrollNode instanceof ScrollPane) {
                                ((ScrollPane) scrollNode).setVvalue(1.0);
                            }
                        });
                    }
                }
                else if ("CHATLOBBY".equals(type)) {
                    System.out.println("Processing CHATLOBBY message: " + receivedMessage);
                    if (receivedMessage.getParameters() != null && receivedMessage.getParameters().length >= 3) {
                        String sender = receivedMessage.getParameters()[0].toString();
                        String gameId = receivedMessage.getParameters()[1].toString();
                        String message = receivedMessage.getParameters()[2].toString();

                        Platform.runLater(() -> {
                            Node lobbyChatNode = uiManager.getComponent("lobbyChatMessagesBox");
                            if (lobbyChatNode instanceof VBox) {
                                VBox lobbyMessagesBox = (VBox) lobbyChatNode;
                                Label msgLabel = new Label(sender + " (" + gameId + "): " + message);
                                msgLabel.setWrapText(true);
                                msgLabel.setStyle("-fx-background-color: #eeeeee; -fx-padding: 5; "
                                        + "-fx-border-radius: 5; -fx-background-radius: 5;");
                                lobbyMessagesBox.getChildren().add(msgLabel);
                            }
                            Node scrollNode = uiManager.getComponent("lobbyChatScroll");
                            if (scrollNode instanceof ScrollPane) {
                                ((ScrollPane) scrollNode).setVvalue(1.0);
                            }
                        });
                    }
                }

                else if ("CHANGEUSERNAME".equals(type)) {
                    /* Expected parameters: [ "OK"/"FAIL", oldName, newName ] */
                    Object[] p = receivedMessage.getParameters();
                    if (p == null || p.length < 3) {
                        System.out.println("CHANGEUSERNAME response missing parameters");
                        return;
                    }

                    boolean ok      = "OK".equalsIgnoreCase(p[0].toString());
                    String oldName  = p[1].toString();
                    String newName  = p[2].toString();

                    if (ok) {


                        // 2) Print success and refresh any UI that shows player lists
                        System.out.printf("changed:" + oldName +" to " + newName);

                    }
                }

                else if ("STARTGAME".equals(type)) {
                    System.out.println("Processing STARTGAME message: " + receivedMessage);
                    if (receivedMessage.getParameters() != null && receivedMessage.getParameters().length > 0) {
                        String gameId = receivedMessage.getParameters()[0].toString();
                        Game toggledGame = gameSessionManager.getGameSession(gameId);
                        if (toggledGame != null) {
                            toggledGame.setStartedFlag(!toggledGame.getStartedFlag());
                            System.out.println("Game " + gameId + " started flag toggled. New value: " + toggledGame.getStartedFlag());

                            Platform.runLater(() -> {
                                Node gameStateNode = uiManager.getComponent("gameStateShow");
                                if (gameStateNode instanceof TextArea) {
                                    TextArea gameStateShow = (TextArea) gameStateNode;
                                    Collection<Game> allGames = gameSessionManager.getAllGameSessionsVals();
                                    StringBuilder sb = new StringBuilder();

                                    if (allGames.isEmpty()) {
                                        sb.append("No games currently available.\n");
                                    } else {
                                        for (Game g : allGames) {
                                            sb.append("Game ID: ").append(g.getGameId()).append("\n");
                                            sb.append("Name: ").append(g.getGameName()).append("\n");
                                            sb.append("Started? ").append(g.getStartedFlag()).append("\n");
                                            sb.append("Players:\n");
                                            for (String user : g.getUsers()) {
                                                sb.append("  - ").append(user).append("\n");
                                            }
                                            sb.append("\n");
                                        }
                                    }
                                    gameStateShow.setText(sb.toString());
                                }
                            });

                        } else {
                            System.out.println("No game session found with id: " + gameId);
                        }
                    } else {
                        System.out.println("STARTGAME message missing required parameters.");
                    }
                }
                else if ("SYNCGP".equals(type)) {
                    System.out.println("Processing SYNCGP message");
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 2) {
                        System.out.println("SYNCGP message missing required parameters.");
                        return;
                    }
                    String gameID = receivedMessage.getParameters()[0].toString();
                    Game game = gameSessionManager.getGameSession(gameID);
                    if (game != null) {
                        game.getUsers().clear();
                        for (int i = 1; i < receivedMessage.getParameters().length; i++) {
                            String user = receivedMessage.getParameters()[i].toString();
                            game.getUsers().add(user);
                        }
                        System.out.println("Updated game session " + gameID + " user list.");
                    } else {
                        System.out.println("No game session found with id: " + gameID);
                    }
                }
                else if ("WIN".equals(type)) {
                    System.out.println("processing WIN message");
                    Game currentGame = GameContext.getInstance().getGameSessionManager().getGameSession(GameContext.getCurrentGameId());
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 2) {
                        return;
                    }
                    Object raw = receivedMessage.getParameters()[1];   // compile-time type: Object

                    long elapsedTime;

                    if (raw instanceof Number n) {        // Java 16 pattern-matching form
                        elapsedTime = n.longValue();      // works for Float, Double, Integer, Long …
                    } else if (raw instanceof String s) { // JSON or text payload?
                        elapsedTime = Long.parseLong(s);
                    } else {
                        throw new IllegalArgumentException(
                                "Elapsed-time parameter must be numeric, but got " + raw.getClass());
                    }


                    Platform.runLater(() -> {
                        String message = "Level Completed and Time: " + elapsedTime + " seconds";

                        // shows panel
                        Label winLabel = new Label(message);
                        winLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #008011; -fx-background-color: rgba(255,255,255,0.8);");
                        winLabel.setAlignment(Pos.CENTER);
                        CentralGraphicalUnit.getInstance().addNode(winLabel);

                        // save the time in a txt. file
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("highscore.txt", true))) {
                            for (String username : currentGame.getUsers()) {
                                writer.write(username + " completed the Level in " + elapsedTime + " seconds \n");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                }
                //else if ("EXIT".equals(type)) {
                //    System.out.println("Exiting Game");
                //    System.exit(0);
                //}

                else {

                    System.out.println("Unknown message type: " + type);
                }

                if ("RESPONSE".equalsIgnoreCase(option)
                        && uuid != null
                        && !uuid.isEmpty())
                {
                    // build a proper ACK message
                    Message ack = new Message(
                            "ACK",                  // messageType
                            new Object[]{ uuid },   // parameters: the UUID we're ACKing
                            "GAME"               // or whatever option your protocol expects
                    );
                    // this will append your username as concealedParam and enqueue it
                    Client.sendMessageBestEffort(ack);
                    System.out.println("→ Sent ACK for UUID=" + uuid);
                }
            }
        };
    }

    public static String getCurrentGameId() {
        return currentGameId.get();
    }

    public static String getSelectedGameObjectId() {
        return selectedGameObjectId.get();
    }

    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }

    /**
     * Starts the client operations and the game loop.
     */
    public void start() {
        uiManager.waitForCentralUnitAndInitialize(() -> {
            Pane mainUIPane = GameUIComponents.createMainUIPane(uiManager, gameSessionManager);
            uiManager.registerComponent("mainUIPane", mainUIPane);

            Pane startGamePane = GameUIComponents.createStartGamePane(uiManager, gameSessionManager);
            uiManager.registerComponent("startGamePane", startGamePane);

            Button startGameButton = (Button) uiManager.getComponent("startGameButton"); // start button

            Pane adminPane = GameUIComponents.createAdministrativePane(uiManager, gameSessionManager);
            uiManager.registerComponent("adminUIPane", adminPane);

            Pane chatPane = GameUIComponents.createglbChatPane(uiManager, gameSessionManager);
            uiManager.registerComponent("chatUIPane", chatPane);

            Pane whisperChatPane = GameUIComponents.createWhisperChatPane(uiManager, gameSessionManager);
            uiManager.registerComponent("whisperChatUIPane", whisperChatPane);

            Pane lobbyChatPane = GameUIComponents.createLobbyChatPane(uiManager, gameSessionManager);
            uiManager.registerComponent("lobbyChatUIPane", lobbyChatPane);

            StackPane layeredRoot = new StackPane();
            layeredRoot.getChildren().addAll(
                    mainUIPane,
                    startGamePane,
                    chatPane,
                    adminPane,
                    whisperChatPane,
                    lobbyChatPane
            );

            // initialise the cube
            Canvas cubeCanvas = GameUIComponents.createCubeCanvas(cubeDrawer);
            layeredRoot.getChildren().add(cubeCanvas);

            setupMouseEventHandlers();

            mainUIPane.setVisible(true);
            startGamePane.setVisible(false);
            chatPane.setVisible(false);
            adminPane.setVisible(false);
            whisperChatPane.setVisible(false);
            lobbyChatPane.setVisible(false);

            ComboBox<String> guiInterfaces  = GameUIComponents.createGuiInterfaces(uiManager);
            StackPane.setAlignment(guiInterfaces, Pos.TOP_LEFT);
            guiInterfaces.setTranslateX(10);
            guiInterfaces.setTranslateY(10);
            layeredRoot.getChildren().add(guiInterfaces);

            // ─── create the controls ─────────────────────────────────────────────
            TextField renameField = new TextField();
            renameField.setPromptText(Client.getInstance().getUsername().get());    // gray hint text

            Button changeNameBtn = new Button("Change User name");

            // small horizontal container so the field and button stay together
            HBox renameBar = new HBox(6, renameField, changeNameBtn);
            renameBar.setAlignment(Pos.TOP_RIGHT);      // field then button

            // ─── pin the bar to the top-right of the StackPane ──────────────────
            StackPane.setAlignment(renameBar, Pos.TOP_RIGHT);

            // push it 10 px from the right & 10 px from the top
            renameBar.setTranslateX(-10);   // negative X moves left from the edge
            renameBar.setTranslateY(10);

            changeNameBtn.setOnAction(ev -> {
                String newName = renameField.getText().trim();

                if (newName.isEmpty()) {
                    return;                       // nothing to do
                }

                /* 1)  update the local username property */


                /* 2)  (optional but recommended) tell the server so everyone else
                    sees the change – here we reuse your CHANGENAME protocol   */
                Message changeReq = new Message(
                        "CHANGEUSERNAME",
                        new Object[]{
                                Client.getInstance().getUsername().get(),
                                newName
                        },
                        "REQUEST"
                );

                /* 3)  clean up the field */
                renameField.clear();
                renameField.setPromptText(newName);

                Client.sendMessageStatic(changeReq);
                Client.getInstance().setUsername(newName);

            });

            // finally add to the layer stack
            layeredRoot.getChildren().add(renameBar);
            renameBar.setPickOnBounds(false);


            CentralGraphicalUnit.getInstance().addNode(layeredRoot);
            System.out.println("All UI components have been added via GameUIComponents.");
        });




    }

    private void startGame() {
        String gameId = currentGameId.get();
        Game game = gameSessionManager.getGameSession(gameId);
        if (game != null) {
            LevelTimer.getInstance().start();
            game.startLevel();
            System.out.println("Game started. Timer started.");
        } else {
            System.out.println("No game session found for the current game ID.");
        }
    }

    /**
     * Starts the game loop for updating and drawing the game state.
     * Uses JavaFX's AnimationTimer to continuously update and draw.
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
                    // Update your GUI element here with the elapsed time
                    String message = "Elapsed Time: " + elapsedTime + " seconds";
                });
            }
        };
        timer.start();
    }

    /**
     * Updates the game state.
     * Throttles updates to the configured FPS (60 FPS by default).
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
     * Draws the current game state on the provided GraphicsContext.
     */
    private void draw(GraphicsContext gc) {
        String gameId = currentGameId.get();
        if (gameId == null || gameId.isEmpty()) {
            return;                          // No game chosen
        }

        /* clear frame */
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        /* draw game scene  */
        Game game = gameSessionManager.getGameSession(gameId);
        if (game != null) {
            game.draw(gc);
        }



        /* overlay: messages‑per‑second meter */
        int    mps       = MessageRateMeter.getMessagesPerSecond();
        String rateText  = mps + " msg/s";

        gc.save();                                         // 1) push entire state

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.TOP);

        double x = gc.getCanvas().getWidth() - 10;   // 10‑px padding from right
        double y = 100;                              // 100‑px from top
        gc.fillText(rateText, x, y);

        gc.restore();                                      // 2) pop → previous state
    }

    private void setupMouseEventHandlers() {

        System.out.println("Setting up mouse event handlers");
        Canvas canvas = CentralGraphicalUnit.getInstance().getGraphicsContext().getCanvas();

        if (canvas == null) {
            System.out.println("Canvas is null! The Canvas was not initialized properly.");
            return;
        }

        canvas.setVisible(true);  // Ensure canvas is visible

        // MousePressed Event: Check if the mouse clicks on the cube (like a button)
        canvas.setOnMousePressed(event -> {
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            // Check if the mouse is over the cube
            if (isMouseOverCube(mouseX, mouseY)) {
                // If clicked on the cube, perform rotation or other action
                System.out.println("Cube clicked, rotating...");
                rotateCube();  // Call the rotation function or any action
            }
        });
    }

    private boolean isMouseOverCube(double mouseX, double mouseY) {
        // Perspective projection parameters
        double fov = 500;  // Field of View (adjust for visual effect)
        double size = 100;  // Size of the cube
        double centerX = CentralGraphicalUnit.getInstance().getGraphicsContext().getCanvas().getWidth() / 2;
        double centerY = CentralGraphicalUnit.getInstance().getGraphicsContext().getCanvas().getHeight() / 2;

        // Let's project the cube's 3D coordinates to 2D
        double[][] vertices = cubeDrawer.getProjectedVertices(fov, size, centerX, centerY);

        // Now check if the mouse is inside the projected 2D cube's boundaries
        for (int i = 0; i < 4; i++) {  // Check only the front face (4 vertices)
            double x1 = vertices[i][0];
            double y1 = vertices[i][1];
            double x2 = vertices[(i + 1) % 4][0];
            double y2 = vertices[(i + 1) % 4][1];

            // Check if the mouse is inside the 2D square formed by the front face
            if (isPointInTriangle(mouseX, mouseY, x1, y1, x2, y2)) {
                return true;  // Mouse is over the cube
            }
        }

        return false;
    }

    private boolean isPointInTriangle(double px, double py, double x1, double y1, double x2, double y2) {
        // This is a simple check to determine if the point is within the triangle.
        double area = 0.5 * (-y2 * x1 + y1 * x2 + x2 * py - y2 * px - x1 * py + x1 * y2);
        double s = 1 / (2 * area) * (py * x2 - px * y2 + y2 * x1 - x2 * y1 + x1 * y2 - y1 * x2);
        double t = 1 / (2 * area) * (px * y1 - py * x1 + y1 * x2 - x1 * y2 + x2 * y1 - y2 * x1);

        return (s > 0 && t > 0 && 1 - s - t > 0);
    }

    private void rotateCube() {
        // Rotate the cube by a fixed angle when clicked
        cubeDrawer.setAngleX(cubeDrawer.getAngleX() + 10);  // Rotate by a fixed angle (adjust as needed)
        cubeDrawer.setAngleY(cubeDrawer.getAngleY() + 10);  // Rotate by a fixed angle (adjust as needed)
        System.out.println("Cube rotated to angleX: " + cubeDrawer.getAngleX() + ", angleY: " + cubeDrawer.getAngleY());
    }

    public static Game getGameById(String gameId) {
        return getInstance().getGameSessionManager().getGameSession(gameId);
    }

    public List<GameObject> getGameObjects() {
        return game.getGameObjects();
    }

    public List<String> getPlayerNames() {
        return getGameObjects().stream()
                .map(GameObject::getName)
                .collect(Collectors.toList());
    }


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