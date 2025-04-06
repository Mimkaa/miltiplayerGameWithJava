package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.UIManager;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GameUIComponents;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;

import javafx.scene.control.ComboBox;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import lombok.Getter;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;

import static ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom.client;

@Getter
public class GameContext {

    //Singleton instance
    @Getter
    private static GameContext instance;


    private final GameSessionManager gameSessionManager;
    private MessageHogger testHogger;

    // Make the current game ID and the selected game object's ID static.
    private static final AtomicReference<String> currentGameId = new AtomicReference<>();
    private static final AtomicReference<String> selectedGameObjectId = new AtomicReference<>();

    // Create a UIManager instance.
    private final UIManager uiManager = new UIManager();



    public GameContext() {
        instance = this;
        this.gameSessionManager = new GameSessionManager();

        instance = this;


        // Initialize the custom MessageHogger.
        testHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message receivedMessage) {
                String type = receivedMessage.getMessageType();

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

                    // Now update the TextArea with the current game info (name and user list).
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

                } else if ("SELECTGO".equals(type)) {
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
                        boolean found = false;
                        for (GameObject go : game.getGameObjects()) {
                            if (go.getId().equals(targetGameObjectId)) {
                                selectedGameObjectId.set(go.getId());
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

                } else if ("CREATEGO".equals(type)) {
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
                        // --- (Optionally) update your TextArea ---
                        Node oldListNode = uiManager.getComponent("usersList");
                        if (oldListNode instanceof TextArea) {
                            TextArea usersListArea = (TextArea) oldListNode;
                            Object[] params = receivedMessage.getParameters();
                            usersListArea.clear();
                            for (Object param : params) {
                                usersListArea.appendText(param.toString() + "\n");
                            }
                        }

                        // --- Update the ComboBox for whispering ---
                        Node comboNode = uiManager.getComponent("whisperUserSelect");
                        if (comboNode instanceof ComboBox) {
                            @SuppressWarnings("unchecked")
                            ComboBox<String> userSelect = (ComboBox<String>) comboNode;

                            // Clear existing items
                            userSelect.getItems().clear();

                            // Get the current client’s username
                            String currentUser = Client.getInstance().getUsername().get();

                            // Add each user except this client
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
                            // Look up the VBox where we place the whisper messages
                            Node chatNode = uiManager.getComponent("whisperChatMessagesBox");
                            if (chatNode instanceof VBox) {
                                VBox messagesBox = (VBox) chatNode;

                                // Create a new label for the incoming whisper
                                Label msgLabel = new Label(sender + ": " + message);
                                msgLabel.setWrapText(true);
                                msgLabel.setStyle("-fx-background-color: #eeeeee; -fx-padding: 5; "
                                        + "-fx-border-radius: 5; -fx-background-radius: 5;");
                                // Add the label to the VBox
                                messagesBox.getChildren().add(msgLabel);
                            }

                            // Now scroll to the bottom of the whisper chat
                            Node scrollNode = uiManager.getComponent("whisperChatScroll");
                            if (scrollNode instanceof ScrollPane) {
                                ((ScrollPane) scrollNode).setVvalue(1.0);
                            }
                        });
                    }
                }
                // --- NEW: Catch CHATLOBBY messages ---
                else if ("CHATLOBBY".equals(type)) {
                    System.out.println("Processing CHATLOBBY message: " + receivedMessage);
                    if (receivedMessage.getParameters() != null && receivedMessage.getParameters().length >= 3) {
                        // Expected parameters: [0] = sender, [1] = gameId, [2] = message text
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

                else if ("SYNCGP".equals(type)) {
                    System.out.println("Processing SYNCGP message");
                    if (receivedMessage.getParameters() == null || receivedMessage.getParameters().length < 2) {
                        System.out.println("SYNCGP message missing required parameters.");
                        return;
                    }
                    // Extract the game ID from the first parameter.
                    String gameID = receivedMessage.getParameters()[0].toString();

                    // Retrieve the game session by its ID.
                    Game game = gameSessionManager.getGameSession(gameID);
                    if (game != null) {
                        // Clear the current user list.
                        game.getUsers().clear();
                        // Loop through each parameter (starting at index 1) and add as a user.
                        for (int i = 1; i < receivedMessage.getParameters().length; i++) {
                            String user = receivedMessage.getParameters()[i].toString();
                            game.getUsers().add(user);
                        }
                        System.out.println("Updated game session " + gameID + " user list.");
                    } else {
                        System.out.println("No game session found with id: " + gameID);
                    }
                }
                else if ("EXIT".equals(type)) {
                    System.out.println("Exiting Game");
                    System.exit(0);
                } else {
                    System.out.println("Unknown message type: " + type);
                }
            }
        };
    }

    // Static getters for the game ID and selected game object ID.
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
            // 1) Create & register the existing panes
            Pane mainUIPane = GameUIComponents.createMainUIPane(uiManager, gameSessionManager);
            uiManager.registerComponent("mainUIPane", mainUIPane);

            Pane adminPane = GameUIComponents.createAdministrativePane(uiManager, gameSessionManager);
            uiManager.registerComponent("adminUIPane", adminPane);

            Pane chatPane = GameUIComponents.createglbChatPane(uiManager, gameSessionManager);
            uiManager.registerComponent("chatUIPane", chatPane);

            Pane whisperChatPane = GameUIComponents.createWhisperChatPane(uiManager, gameSessionManager);
            uiManager.registerComponent("whisperChatUIPane", whisperChatPane);

            // 2) Create & register the new Lobby Chat pane
            Pane lobbyChatPane = GameUIComponents.createLobbyChatPane(uiManager, gameSessionManager);
            uiManager.registerComponent("lobbyChatUIPane", lobbyChatPane);

            // 3) Add all these panes to a StackPane
            StackPane layeredRoot = new StackPane();
            layeredRoot.getChildren().addAll(
                    mainUIPane,
                    chatPane,
                    adminPane,
                    whisperChatPane,
                    lobbyChatPane
            );

            // 4) Set the initial visibility
            mainUIPane.setVisible(true);
            chatPane.setVisible(false);
            adminPane.setVisible(false);
            whisperChatPane.setVisible(false);
            lobbyChatPane.setVisible(false);

            // 5) Create & add the ComboBox for switching between panes
            ComboBox<String> guiInterfaces  = GameUIComponents.createGuiInterfaces(uiManager);
            StackPane.setAlignment(guiInterfaces, Pos.TOP_LEFT);
            guiInterfaces.setTranslateX(10);
            guiInterfaces.setTranslateY(10);
            layeredRoot.getChildren().add(guiInterfaces);

            // 6) Finally, add this StackPane to the CentralGraphicalUnit
            CentralGraphicalUnit.getInstance().addNode(layeredRoot);

            System.out.println("All UI components have been added via GameUIComponents.");
        });

        // register the client on the server
        Message registrationMsg = new Message(
                "REGISTER",
                new Object[] {},
                "REQUEST"
        );
        Client.sendMessageStatic(registrationMsg);
    }

    /**
     * Starts the game loop for updating and drawing the game state.
     * This example uses JavaFX's AnimationTimer and retrieves the GraphicsContext
     * from the CentralGraphicalUnit singleton.
     */
    public void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            GraphicsContext gc = CentralGraphicalUnit.getInstance().getGraphicsContext();
            @Override
            public void handle(long now) {
                update();
                draw(gc);
            }
        };
        timer.start();
    }

    /**
     * Updates the game state.
     * This method is called once per frame by the game loop.
     */
    private void update() {
        String gameId = currentGameId.get();
        if (gameId == null) {
            return;
        }
        Game game = gameSessionManager.getGameSession(gameId);
        if (game == null) {
            return;
        }
        // Update all game objects if there is at least one.
        // game.updateAllObjects();
    }

    /**
     * Draws the current game state on the provided GraphicsContext.
     *
     * @param gc The GraphicsContext to draw on.
     */
    private void draw(GraphicsContext gc) {
        String gameId = currentGameId.get();
        if (gameId == null || gameId.isEmpty()) {
            // No game chosen, do nothing
            return;
        }

        // Clear the background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        Game game = gameSessionManager.getGameSession(gameId);
        if (game == null) {
            return;
        }

        // Otherwise, draw the game
        game.draw(gc);
    }


    public static Game getGameById(String gameId) {
        return getInstance().getGameSessionManager().getGameSession(gameId);
    }

/*
    public static String getLocalClientId() {
        return ThinkOutsideTheRoom.client.getClientId();
    }
*/

    public static void main(String[] args) {
        // Create the game context.
        GameContext context = new GameContext();
        // Start the context on a separate thread.
        new Thread(() -> {
            context.start();
            Platform.runLater(context::startGameLoop);
        }).start();

        // Launch the JavaFX GUI (this call blocks until the GUI exits).
        System.out.println("Launching JavaFX...");
        Application.launch(GUI.class, args);

    }
}
