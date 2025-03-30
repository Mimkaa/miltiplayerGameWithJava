package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Player;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.UIManager;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class GameContext {
    private final GameSessionManager gameSessionManager;
    private final Client client;
    private MessageHogger testHogger;
    
    // Make the current game ID and the selected game object's ID static.
    private static final AtomicReference<String> currentGameId = new AtomicReference<>();
    private static final AtomicReference<String> selectedGameObjectId = new AtomicReference<>();

    // Create a UIManager instance.
    private final UIManager uiManager = new UIManager();

    public GameContext() {
        this.gameSessionManager = new GameSessionManager();
        this.client = new Client();

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
                    // Update the current game id.
                    currentGameId.set(receivedId);
                    System.out.println("Game created with id: " + receivedId);
                    
                    // Update the ComboBox with the new game name.
                    Platform.runLater(() -> {
                        Node node = uiManager.getComponent("gameSelect");
                        if (node instanceof ComboBox) {
                            ComboBox<String> gameSelect = (ComboBox<String>) node;
                            // Check for duplicates if necessary.
                            if (!gameSelect.getItems().contains(receivedGameName)) {
                                gameSelect.getItems().add(receivedGameName);
                            }
                        }
                    });

                } else if ("JOINGAME".equals(type)) {
                    System.out.println("Processing JOINGAME response");
                    String gameID = receivedMessage.getParameters()[0].toString();
                    currentGameId.set(gameID);
                    System.out.println("Current game id set to: " + currentGameId.get());
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
                            constructorParams = Arrays.copyOfRange(receivedMessage.getParameters(), 3, receivedMessage.getParameters().length);
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

    public Client getClient() {
        return client;
    }
    
    /**
     * Starts the client operations and the game loop.
     */
    public void start() {
        uiManager.waitForCentralUnitAndInitialize(() -> {
            // --- Overlay ComboBox for game selection ---
            ComboBox<String> gameSelect = new ComboBox<>();
            gameSelect.setPromptText("Select Game...");
            // Populate the ComboBox with game names from the gameSessionManager.
            gameSessionManager.getAllGameSessions().values().stream()
                    .map(Game::getGameName)
                    .forEach(gameSelect.getItems()::add);
            // Optionally, add a listener to update game state on selection change.
            gameSelect.setOnAction(e -> {
                String selectedGame = gameSelect.getSelectionModel().getSelectedItem();
                System.out.println("Selected game: " + selectedGame);
                // Update your game state as needed.
            });
            // Position the ComboBox at the top center.
            StackPane.setAlignment(gameSelect, Pos.TOP_CENTER);
            // Give it a margin.
            StackPane.setMargin(gameSelect, new Insets(10, 10, 0, 10));
            CentralGraphicalUnit.getInstance().addNode(gameSelect);
            uiManager.registerComponent("gameSelect", gameSelect);
            
            // --- Overlay Input Field ---
            TextField overlayInputField = new TextField();
            overlayInputField.setPromptText("Enter text here...");
            // Position the input field just below the ComboBox.
            StackPane.setAlignment(overlayInputField, Pos.TOP_CENTER);
            overlayInputField.setTranslateY(40);
            CentralGraphicalUnit.getInstance().addNode(overlayInputField);
            uiManager.registerComponent("overlayInputField", overlayInputField);
            
            // --- First Overlay Button: Create Game Button ---
            Button createGameButton = new Button("Create Game Button");
            StackPane.setAlignment(createGameButton, Pos.TOP_CENTER);
            createGameButton.setTranslateY(80);
            CentralGraphicalUnit.getInstance().addNode(createGameButton);
            uiManager.registerComponent("createGameButton", createGameButton);
            createGameButton.setOnAction(e -> {
                String gameName = overlayInputField.getText().trim();
                if (gameName.isEmpty()) {
                    System.out.println("Please enter a game name.");
                    return;
                }
                Message createGameMessage = new Message("CREATEGAME", new Object[]{gameName}, "REQUEST");
                Client.sendMessageStatic(createGameMessage);
            });
            
            // --- Second Overlay Button: Join Game Button ---
            Button joinGameButton = new Button("Join Game Button");
            StackPane.setAlignment(joinGameButton, Pos.TOP_CENTER);
            joinGameButton.setTranslateY(120);
            CentralGraphicalUnit.getInstance().addNode(joinGameButton);
            uiManager.registerComponent("joinGameButton", joinGameButton);
            joinGameButton.setOnAction(e -> {
                if (uiManager.getComponent("gameSelect") instanceof ComboBox) {
                    String selectedGameName = ((ComboBox<String>) uiManager.getComponent("gameSelect"))
                                                    .getSelectionModel().getSelectedItem();
                    if (selectedGameName == null || selectedGameName.trim().isEmpty()) {
                        System.out.println("Please select a game from the combo box.");
                        return;
                    }
                    Message joinGameMessage = new Message("JOINGAME", new Object[]{selectedGameName}, "REQUEST");
                    Client.sendMessageStatic(joinGameMessage);
                    System.out.println("Sent JOINGAME message with game name: " + selectedGameName);
                } else {
                    System.out.println("Game selector not found.");
                }
            });
            
            // --- Third Overlay Button: Create Object Button ---
            Button createObjectButton = new Button("Create Object Button");
            // Position this button below the join game button.
            StackPane.setAlignment(createObjectButton, Pos.TOP_CENTER);
            createObjectButton.setTranslateY(160);
            CentralGraphicalUnit.getInstance().addNode(createObjectButton);
            uiManager.registerComponent("createObjectButton", createObjectButton);

            createObjectButton.setOnAction(e -> {
                // Retrieve the current game session ID.
                String sessionId = GameContext.getCurrentGameId();
                if (sessionId == null || sessionId.isEmpty()) {
                    System.out.println("No current game session to create an object in.");
                    return;
                }
                
                // Get the name from the overlay input field (registered as "overlayInputField").
                Node node = uiManager.getComponent("overlayInputField");
                if (!(node instanceof TextField)) {
                    System.out.println("Overlay input field not found or is not a TextField.");
                    return;
                }
                String playerName = ((TextField) node).getText().trim();
                if (playerName.isEmpty()) {
                    System.out.println("Please enter a name for the player in the input field.");
                    return;
                }

                // Define the object type, for example "Player". Change as needed.
                String objectType = "Player";

                // Create the CREATEGO message. In this example we provide:
                //  - the sessionId (again) as the first argument,
                //  - objectType as the second argument,
                //  - playerName (retrieved from the input field),
                //  - position and radius details,
                //  - and sessionId again at the end for the game ID.
                Message createObjectMsg = new Message(
                    "CREATEGO", 
                    new Object[]{ 
                        sessionId,      // or some new UUID if needed 
                        objectType, 
                        playerName, 
                        200.0f, 
                        100.0f, 
                        25.0f, 
                        sessionId 
                    },
                    "REQUEST"
                );

                // Send the message via the static client method.
                Client.sendMessageStatic(createObjectMsg);

                System.out.println("Sent CREATEGO message for new game object with name: " + playerName);
            });

                    // --- Fourth Overlay Button: Select Object Button ---
            Button selectObjectButton = new Button("Select Object Button");
            StackPane.setAlignment(selectObjectButton, Pos.TOP_CENTER);
            selectObjectButton.setTranslateY(200);
            CentralGraphicalUnit.getInstance().addNode(selectObjectButton);
            uiManager.registerComponent("selectObjectButton", selectObjectButton);
            selectObjectButton.setOnAction(e -> {
                // Retrieve the text from the overlay input field.
                Node node = uiManager.getComponent("overlayInputField");
                if (node instanceof TextField) {
                    String objectName = ((TextField) node).getText().trim();
                    if (objectName.isEmpty()) {
                        System.out.println("Please enter an object name to select.");
                        return;
                    }
                    // Retrieve the current game session ID.
                    String sessionId = GameContext.getCurrentGameId();
                    if (sessionId == null || sessionId.isEmpty()) {
                        System.out.println("No current game session available.");
                        return;
                    }
                    // Create the SELECTGO message with the object name.
                    Message selectGoMsg = new Message("SELECTGO", new Object[]{sessionId,objectName}, "REQUEST");
                    Client.sendMessageStatic(selectGoMsg);
                    System.out.println("Sent SELECTGO message with object name: " + objectName);
                } else {
                    System.out.println("Overlay input field not found.");
                }
            });
            
            System.out.println("Overlay ComboBox, input field, and three buttons added to UI.");
        });




        Scanner inputScanner = new Scanner(System.in);
        String suggestedNickname = Nickname_Generator.generateNickname();
        System.out.println("Suggested Nickname: " + suggestedNickname);
        System.out.println("Please press enter to use the suggested name, or type your own: ");
        String userName = inputScanner.nextLine();
        if (userName.isEmpty()) {
            userName = suggestedNickname;
        }
        System.out.println("Entered nickname: " + userName);

        // Set up the client.
        client.setUsername(userName);
        new Thread(client::run).start();
        client.startConsoleReaderLoop();
        // register the clinet on the server
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
        game.updateAllObjects();
    }

    /**
     * Draws the current game state on the provided GraphicsContext.
     *
     * @param gc The GraphicsContext to draw on.
     */
    private void draw(GraphicsContext gc) {
        // Set and fill the background with white.
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        
        String gameId = currentGameId.get();
        if (gameId == null) {
            return;
        }
        Game game = gameSessionManager.getGameSession(gameId);
        if (game == null) {
            return;
        }
        
        // Draw all game objects.
        game.draw(gc);
    }

    public static void main(String[] args) {
        // Create the game context.
        GameContext context = new GameContext();
        // Start the context on a separate thread.
        new Thread(() -> {
            context.start();
            Platform.runLater(() -> context.startGameLoop());
        }).start();

        // Launch the JavaFX GUI (this call blocks until the GUI exits).
        Application.launch(GUI.class, args);
    }
}
