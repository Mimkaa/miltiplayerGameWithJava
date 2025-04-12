package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import java.rmi.server.ObjID;
import java.util.Collection;

import ch.unibas.dmi.dbis.cs108.example.Level;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GameUIComponents {

    public static Pane createMainUIPane(UIManager uiManager, GameSessionManager gameSessionManager) {
        // Create a Pane and set its preferred size
        Pane mainUIPane = new Pane();
        mainUIPane.setPrefSize(640, 480); // Ensure enough space is available
        mainUIPane.setTranslateX(0);
        mainUIPane.setTranslateY(50);
        mainUIPane.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");

        // --- Overlay ComboBox for game selection ---
        ComboBox<String> gameSelect = new ComboBox<>();
        gameSelect.setPromptText("Select Game...");
        gameSessionManager.getAllGameSessions().values().stream()
                .map(Game::getGameName)
                .forEach(gameSelect.getItems()::add);
        gameSelect.setOnAction(e -> {
            String selectedGame = gameSelect.getSelectionModel().getSelectedItem();
            System.out.println("Selected game: " + selectedGame);
        });
        gameSelect.setLayoutX(10);
        gameSelect.setLayoutY(10);
        mainUIPane.getChildren().add(gameSelect);
        uiManager.registerComponent("gameSelect", gameSelect);

        // --- Game ID Input Field to display current game id ---
        TextField gameIdField = new TextField(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        gameIdField.setPromptText("Game ID");
        gameIdField.setEditable(false);
        gameIdField.setLayoutX(200);
        gameIdField.setLayoutY(240);
        mainUIPane.getChildren().add(gameIdField);
        uiManager.registerComponent("gameIdField", gameIdField);

        // --- Overlay Input Field ---
        TextField overlayInputField = new TextField();
        overlayInputField.setPromptText("Enter text here...");
        overlayInputField.setLayoutX(200);
        overlayInputField.setLayoutY(40);
        mainUIPane.getChildren().add(overlayInputField);
        uiManager.registerComponent("overlayInputField", overlayInputField);

        // --- First Overlay Button: Create Game Button ---
        Button createGameButton = new Button("Create Game Button");
        createGameButton.setLayoutX(200);
        createGameButton.setLayoutY(80);
        mainUIPane.getChildren().add(createGameButton);
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
        joinGameButton.setLayoutX(200);
        joinGameButton.setLayoutY(120);
        mainUIPane.getChildren().add(joinGameButton);
        uiManager.registerComponent("joinGameButton", joinGameButton);
        joinGameButton.setOnAction(e -> {
            if (uiManager.getComponent("gameSelect") instanceof ComboBox) {
                String selectedGameName = ((ComboBox<String>) uiManager.getComponent("gameSelect"))
                        .getSelectionModel().getSelectedItem();
                if (selectedGameName == null || selectedGameName.trim().isEmpty()) {
                    System.out.println("Please select a game from the combo box.");
                    return;
                }


                Message joinGameMessage = new Message("JOINGAME", new Object[]{selectedGameName, Client.getInstance().getUsername(), GameContext.getCurrentGameId()==null?"default":GameContext.getCurrentGameId()}, "REQUEST");
                Client.sendMessageStatic(joinGameMessage);
                System.out.println("Sent JOINGAME message with game name: " + selectedGameName);
            } else {
                System.out.println("Game selector not found.");
            }
        });

        // --- Third Overlay Button: Create Object Button ---
        Button createObjectButton = new Button("Create Object Button");
        createObjectButton.setLayoutX(200);
        createObjectButton.setLayoutY(160);
        mainUIPane.getChildren().add(createObjectButton);
        uiManager.registerComponent("createObjectButton", createObjectButton);
        createObjectButton.setOnAction(e -> {
            String sessionId = GameContext.getCurrentGameId();
            if (sessionId == null || sessionId.isEmpty()) {
                System.out.println("No current game session to create an object in.");
                return;
            }
            Node node = uiManager.getComponent("overlayInputField");
            if (!(node instanceof TextField)) {
                System.out.println("Overlay input field not found or is not a TextField.");
                return;
            }
            String input = ((TextField) node).getText().trim();
            if (input.isEmpty()) {
                System.out.println("Please enter the parameters for the game object in the input field.");
                return;
            }
            if (input.startsWith("[") && input.endsWith("]")) {
                input = input.substring(1, input.length() - 1);
            }
            Object[] userParameters = MessageCodec.decodeParameters(input);
            Object[] finalParameters = new Object[userParameters.length + 1];
            finalParameters[0] = sessionId;
            System.arraycopy(userParameters, 0, finalParameters, 1, userParameters.length);
            Message createObjectMsg = new Message("CREATEGO", finalParameters, "REQUEST");
            Client.sendMessageStatic(createObjectMsg);
            System.out.println("Sent CREATEGO message with parameters: " + java.util.Arrays.toString(finalParameters));
        });

        // --- Fourth Overlay Button: Select Object Button ---
        Button selectObjectButton = new Button("Select Object Button");
        selectObjectButton.setLayoutX(200);
        selectObjectButton.setLayoutY(200);
        mainUIPane.getChildren().add(selectObjectButton);
        uiManager.registerComponent("selectObjectButton", selectObjectButton);
        selectObjectButton.setOnAction(e -> {
            Node node = uiManager.getComponent("overlayInputField");
            if (node instanceof TextField) {
                String objectName = ((TextField) node).getText().trim();
                if (objectName.isEmpty()) {
                    System.out.println("Please enter an object name to select.");
                    return;
                }
                String sessionId = GameContext.getCurrentGameId();
                if (sessionId == null || sessionId.isEmpty()) {
                    System.out.println("No current game session available.");
                    return;
                }
                Message selectGoMsg = new Message("SELECTGO", new Object[]{sessionId, objectName}, "REQUEST");
                Client.sendMessageStatic(selectGoMsg);
                System.out.println("Sent SELECTGO message with object name: " + objectName);
            } else {
                System.out.println("Overlay input field not found.");
            }
        });

        // --- New Overlay Button: Start Level Button ---
        Button startLevelButton = new Button("Start Level");
        startLevelButton.setLayoutX(200);
        startLevelButton.setLayoutY(220); // Make sure this is within the pane
        mainUIPane.getChildren().add(startLevelButton);
        uiManager.registerComponent("startLevelButton", startLevelButton);
        startLevelButton.setOnAction(e -> {
            String gameId = GameContext.getCurrentGameId();
            Message msg = new Message("STARTGAME", new Object[]{gameId}, "REQUEST");
            Client.sendMessageStatic(msg);
            Level level = new Level();
            level.initializeLevel();
            
            
            System.out.println("Level started!");
            
        });

        return mainUIPane;
    }

    public static Pane createAdministrativePane(UIManager uiManager, GameSessionManager gameSessionManager) {
        Pane administrativePane = new Pane();
        administrativePane.setTranslateX(0);
        administrativePane.setTranslateY(30);
        administrativePane.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");

        // TextArea to show the game states
        TextArea usersListCurrGS = new TextArea();
        usersListCurrGS.setText(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        usersListCurrGS.setPromptText("Game ID");
        usersListCurrGS.setEditable(false);
        usersListCurrGS.setLayoutX(200);
        usersListCurrGS.setLayoutY(50);
        usersListCurrGS.setPrefWidth(200);
        usersListCurrGS.setPrefHeight(150);
        administrativePane.getChildren().add(usersListCurrGS);

        // Register the TextArea with the UI manager so we can retrieve it later
        uiManager.registerComponent("gameStateShow", usersListCurrGS);

        // --- Add a "Refresh" button that, when clicked, lists all games in the TextArea ---
        Button refreshButton = new Button("Refresh Game Info");
        refreshButton.setLayoutX(200);
        refreshButton.setLayoutY(210);
        administrativePane.getChildren().add(refreshButton);

        // On button click, update the TextArea with info from all game sessions
        refreshButton.setOnAction(e -> {
            // Because button actions run on the JavaFX thread, we can directly access UI elements
            Node gameStateNode = uiManager.getComponent("gameStateShow");
            if (gameStateNode instanceof TextArea) {
                TextArea gameStateShow = (TextArea) gameStateNode;

                // Fetch all games from the GameSessionManager
                Collection<Game> allGames = gameSessionManager.getAllGameSessionsVals(); 
                // Replace "getAllGameSessionsVals()" with your actual method name if different

                // Build a string listing every game’s details
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
                        sb.append("\n"); // blank line between games
                    }
                }

                // Display the info in the TextArea
                gameStateShow.setText(sb.toString());
            }
        });

        return administrativePane;
    }



    public static Pane createCheckPane(UIManager uiManager, GameSessionManager gameSessionManager) {
        Pane CheckPane = new Pane();
        CheckPane.setTranslateX(0);
        CheckPane.setTranslateY(30);
        CheckPane.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");






        Button OnlineUsersButton = new Button("Online Users Global");
        OnlineUsersButton.setLayoutX(200);
        OnlineUsersButton.setLayoutY(210);
        CheckPane.getChildren().add(OnlineUsersButton);
        uiManager.registerComponent("OnlineUsersButtonnn", OnlineUsersButton);

        Button OnlineUsersButtonvvv = new Button("BUtton2");
        OnlineUsersButtonvvv.setLayoutX(100);
        OnlineUsersButtonvvv.setLayoutY(210);
        CheckPane.getChildren().add(OnlineUsersButtonvvv);
        uiManager.registerComponent("OnlineUsersButtonnnvvv", OnlineUsersButtonvvv);






        return CheckPane;
    }

    public static Pane createglbChatPane(UIManager uiManager, GameSessionManager gameSessionManager) {

        BorderPane root = new BorderPane();
        root.setPrefSize(600, 500);

        // CHAT MESSAGES
        VBox messagesBox = new VBox(5);
        uiManager.registerComponent("chatMessagesBox", messagesBox);
        messagesBox.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(messagesBox);
        uiManager.registerComponent("chatScroll", scrollPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-radius: 10;");

        // INPUT FIELD & SEND BUTTON
        TextField chatInputField = new TextField();
        chatInputField.setPromptText("Type a message...");
        chatInputField.setPrefWidth(400);

        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");

        HBox inputBox = new HBox(10, chatInputField, sendButton);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(10));

        VBox chatContainer = new VBox();
        chatContainer.setAlignment(Pos.BOTTOM_CENTER);
        chatContainer.setPadding(new Insets(10));
        chatContainer.setMouseTransparent(false);

        chatContainer.getChildren().addAll(scrollPane, inputBox);

        // Center everything
        VBox centerWrapper = new VBox();
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.getChildren().add(chatContainer);
        root.setCenter(centerWrapper);

        // SEND ACTION
        sendButton.setOnAction(e -> {
            String username = Client.getInstance().getUsername().get();
            String message = chatInputField.getText();
            if (!message.trim().isEmpty()) {
                Object[] params = new String[]{username, message};
                Message responseMsg = new Message("CHATGLB", params, "REQUEST");
                Client.sendMessageStatic(responseMsg);

                // Local display
                Label msg = new Label(username + ": " + message);
                messagesBox.getChildren().add(msg);
                chatInputField.clear();
                scrollPane.setVvalue(1.0);
            }
        });

        uiManager.registerComponent("chatInputField", chatInputField);
        return root;
    }

    public static Pane createWhisperChatPane(UIManager uiManager, GameSessionManager gameSessionManager) {

        // Keep the existing BorderPane for the chat
        BorderPane root = new BorderPane();
        root.setPrefSize(600, 500);

        // === (1) The main Whisper Chat messages & input, same as before ===
        VBox messagesBox = new VBox(5);
        uiManager.registerComponent("whisperChatMessagesBox", messagesBox);
        messagesBox.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        uiManager.registerComponent("whisperChatScroll", scrollPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-radius: 10;");

        TextField chatInputField = new TextField();
        chatInputField.setPromptText("Type a message...");
        chatInputField.setPrefWidth(400);
        uiManager.registerComponent("whisperChatInputField", chatInputField);

        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");

        HBox inputBox = new HBox(10, chatInputField, sendButton);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(10));

        VBox chatContainer = new VBox();
        chatContainer.setAlignment(Pos.BOTTOM_CENTER);
        chatContainer.setPadding(new Insets(10));
        chatContainer.setMouseTransparent(false);
        chatContainer.getChildren().addAll(scrollPane, inputBox);

        VBox centerWrapper = new VBox();
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.getChildren().add(chatContainer);

        // Put the chat in the center of our BorderPane
        root.setCenter(centerWrapper);

        // === (2) Add the OnlineUsersButton and a ComboBox for user selection ===

        Pane topPane = new Pane();
        topPane.setPrefHeight(100);

        // The "Online Users Global" button
        Button OnlineUsersButton = new Button("Online Users Global");
        OnlineUsersButton.setLayoutX(20);
        OnlineUsersButton.setLayoutY(80);
        topPane.getChildren().add(OnlineUsersButton);
        uiManager.registerComponent("OnlineUsersButton", OnlineUsersButton);

        // On button click, send GETUSERS request
        OnlineUsersButton.setOnAction(e -> {
            // This request presumably returns a list of online users
            Message selectGoMsg = new Message("GETUSERS", new Object[]{}, "REQUEST");
            Client.sendMessageStatic(selectGoMsg);
        });

        // Instead of a TextArea, use a ComboBox to select whom we want to whisper to
        ComboBox<String> userSelect = new ComboBox<>();
        userSelect.setPromptText("Select a user to whisper");
        userSelect.setLayoutX(20);
        userSelect.setLayoutY(120);
        userSelect.setPrefWidth(200);

        // Register this combo box with the UIManager so we can populate it
        // when the GETUSERS response comes back in GameContext
        uiManager.registerComponent("whisperUserSelect", userSelect);

        topPane.getChildren().add(userSelect);

        // Put the topPane with our button and combo box at the top of the BorderPane
        root.setTop(topPane);

        // === (3) The existing send button logic, but using "WHISPER" message ===
        sendButton.setOnAction(e -> {
            String username = Client.getInstance().getUsername().get();
            String message = chatInputField.getText().trim();

            // Also get the target user from the ComboBox
            String targetUser = userSelect.getSelectionModel().getSelectedItem();
            if (targetUser == null || targetUser.isEmpty()) {
                System.out.println("Please select a user to whisper to!");
                return;
            }

            if (!message.isEmpty()) {
                // For example, you can pass the target user as a parameter:
                Object[] params = new String[]{Client.getInstance().getUsername().get(), targetUser, message};
                // Then your server can handle "WHISPER" by delivering
                // "message" from "username" to "targetUser".
                Message responseMsg = new Message("WHISPER", params, "REQUEST");
                Client.sendMessageStatic(responseMsg);

                // Local display
                Label msg = new Label("Whisper to " + targetUser + ": " + message);
                messagesBox.getChildren().add(msg);
                chatInputField.clear();
                scrollPane.setVvalue(1.0);
            }
        });

        // Return the final whisper chat pane
        return root;
    }

    public static Pane createLobbyChatPane(UIManager uiManager, GameSessionManager gameSessionManager) {
        // Main BorderPane layout
        BorderPane root = new BorderPane();
        root.setPrefSize(600, 500);

        // ------------------------------------------
        // (A) Top: Title Label
        // ------------------------------------------
        Label titleLabel = new Label("Lobby Chat");
        titleLabel.setPadding(new Insets(10));
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        root.setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        // ------------------------------------------
        // (B) Left: Users List
        // ------------------------------------------
        VBox leftSideBox = new VBox(10);
        leftSideBox.setPadding(new Insets(10));

        TextArea usersListCurrGS = new TextArea();
        usersListCurrGS.setText(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        usersListCurrGS.setPromptText("Current Game Users");
        usersListCurrGS.setEditable(false);
        usersListCurrGS.setPrefSize(150, 400);

        leftSideBox.getChildren().add(usersListCurrGS);
        uiManager.registerComponent("usersListCurrGS", usersListCurrGS);
        root.setLeft(leftSideBox);

        // ------------------------------------------
        // (C) Center: Chat Messages
        // ------------------------------------------
        // A VBox to hold chat messages
        VBox messagesBox = new VBox(5);
        messagesBox.setPadding(new Insets(10));
        uiManager.registerComponent("lobbyChatMessagesBox", messagesBox);

        // A ScrollPane containing the messages
        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-radius: 10;");
        uiManager.registerComponent("lobbyChatScroll", scrollPane);

        root.setCenter(scrollPane);

        // ------------------------------------------
        // (D) Bottom: Chat Input + Send Button
        // ------------------------------------------
        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(10));

        TextField chatInputField = new TextField();
        chatInputField.setPromptText("Type a lobby message...");
        chatInputField.setPrefWidth(400);
        uiManager.registerComponent("lobbyChatInputField", chatInputField);

        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        bottomBox.getChildren().addAll(chatInputField, sendButton);

        root.setBottom(bottomBox);

        // ------------------------------------------
        // (E) Send Button Logic
        // ------------------------------------------
        sendButton.setOnAction(e -> {
            String username = Client.getInstance().getUsername().get();
            String gameId = GameContext.getCurrentGameId(); // Retrieve the current game ID
            String message = chatInputField.getText().trim();
            if (!message.isEmpty() && gameId != null && !gameId.isEmpty()) {
                // Send username, gameId, and the message text
                Object[] params = new String[]{username, gameId, message};
                Message lobbyMsg = new Message("CHATLOBBY", params, "REQUEST");
                Client.sendMessageStatic(lobbyMsg);

                // Local display in the chat messages area
                Label msgLabel = new Label(username + " (" + gameId + "): " + message);
                messagesBox.getChildren().add(msgLabel);
                chatInputField.clear();
                scrollPane.setVvalue(1.0);
            }
        });

        return root;
    }







    /**
     * Creates and returns a toggle button to control the visibility of the given mainUIPane.
     *
     * @param mainUIPane the pane whose visibility is toggled.
     * @return the toggle button.
     */
    public static Button createTogglePaneButton(Pane mainUIPane) {
        Button togglePaneButton = new Button("Toggle UI Pane Visibility");
        // Align the button to the top left using StackPane's alignment
        StackPane.setAlignment(togglePaneButton, Pos.TOP_LEFT);
        togglePaneButton.setTranslateX(0);
        togglePaneButton.setTranslateY(0);
        togglePaneButton.setOnAction(e -> {
            boolean currentVisibility = mainUIPane.isVisible();
            mainUIPane.setVisible(!currentVisibility);
            System.out.println("Main UI Pane is now " + (mainUIPane.isVisible() ? "visible" : "hidden"));
        });
        return togglePaneButton;
    }

    public static ComboBox<String> createGuiInterfaces(UIManager uiManager) {

        ComboBox<String> guiInterfaces = new ComboBox<>();
        StackPane.setAlignment(guiInterfaces, Pos.TOP_LEFT);
        guiInterfaces.setTranslateX(0);
        guiInterfaces.setTranslateY(0);
        guiInterfaces.getItems().addAll("Lobby", "Glob Chat", "Lobby Chat", "Wisper Chat", "Administration", "None");
        guiInterfaces.setPromptText("Select GUI Interface");

        guiInterfaces.getSelectionModel().select("None");

        guiInterfaces.setOnAction(e -> {
            String selected = guiInterfaces.getSelectionModel().getSelectedItem();
            System.out.println("Selected chat mode: " + selected);

            // Show/hide the Lobby pane
            Node lobbyPaneNode = uiManager.getComponent("mainUIPane");
            if (lobbyPaneNode instanceof Pane) {
                Pane lobbyPane = (Pane) lobbyPaneNode;
                lobbyPane.setVisible("Lobby".equals(selected));
            }

            // Show/hide the Administration pane
            Node adminPaneNode = uiManager.getComponent("adminUIPane");
            if (adminPaneNode instanceof Pane) {
                Pane adminPane = (Pane) adminPaneNode;
                adminPane.setVisible("Administration".equals(selected));
            }

            // Show/hide the global chat pane
            Node chatPaneNode = uiManager.getComponent("chatUIPane");
            if (chatPaneNode instanceof Pane) {
                Pane chatPane = (Pane) chatPaneNode;
                chatPane.setVisible("Glob Chat".equals(selected));
            }

            // Show/hide the whisper chat pane
            Node whisperChatPaneNode = uiManager.getComponent("whisperChatUIPane");
            if (whisperChatPaneNode instanceof Pane) {
                Pane whisperChatPane = (Pane) whisperChatPaneNode;
                whisperChatPane.setVisible("Wisper Chat".equals(selected));
            }

            // === Add the block for your "Lobby Chat" pane ===
            Node lobbyChatPaneNode = uiManager.getComponent("lobbyChatUIPane");
            if (lobbyChatPaneNode instanceof Pane) {
                Pane lobbyChatPane = (Pane) lobbyChatPaneNode;
                lobbyChatPane.setVisible("Lobby Chat".equals(selected));
            }
        });

        uiManager.registerComponent("guiInterfaces", guiInterfaces);
        return guiInterfaces;
    }




} 
