package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import java.util.Collection;

import ch.unibas.dmi.dbis.cs108.example.Cube.CubeDrawer;
import ch.unibas.dmi.dbis.cs108.example.Level;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.highscore.LevelTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GameUIComponents {

    public static Pane createMainUIPane(UIManager uiManager, GameSessionManager gameSessionManager) {
        VBox mainVBox = new VBox(10);
        mainVBox.setPadding(new Insets(20));
        mainVBox.setAlignment(Pos.TOP_CENTER);
        mainVBox.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");

        // ComboBox: Select Game
        ComboBox<String> gameSelect = new ComboBox<>();
        gameSelect.setPromptText("Select Game...");
        gameSessionManager.getAllGameSessions().values().stream()
                .map(Game::getGameName)
                .forEach(gameSelect.getItems()::add);
        gameSelect.setOnAction(e -> {
            String selectedGame = gameSelect.getSelectionModel().getSelectedItem();
            System.out.println("Selected game: " + selectedGame);
        });
        uiManager.registerComponent("gameSelect", gameSelect);

        // TextField: Overlay Input
        TextField overlayInputField = new TextField();
        overlayInputField.setPromptText("Enter text here...");
        uiManager.registerComponent("overlayInputField", overlayInputField);
        overlayInputField.setPrefWidth(100);
        overlayInputField.setMaxWidth(150);


        // Button: Create Game
        Button createGameButton = new Button("Create Game");
        createGameButton.setOnAction(e -> {
            String gameName = overlayInputField.getText().trim();
            if (gameName.isEmpty()) {
                System.out.println("Please enter a game name.");
                return;
            }
            Message msg = new Message("CREATEGAME", new Object[]{gameName}, "REQUEST");
            Client.sendMessageStatic(msg);
        });
        uiManager.registerComponent("createGameButton", createGameButton);

        // Button: Join Game
        Button joinGameButton = new Button("Join Game");
        joinGameButton.setOnAction(e -> {
            if (gameSelect == null) return;
            String selectedGame = gameSelect.getSelectionModel().getSelectedItem();
            if (selectedGame == null || selectedGame.trim().isEmpty()) {
                System.out.println("Please select a game.");
                return;
            }
            Message joinMsg = new Message("JOINGAME", new Object[]{
                    selectedGame,
                    Client.getInstance().getUsername(),
                    GameContext.getCurrentGameId() == null ? "default" : GameContext.getCurrentGameId()
            }, "REQUEST");
            Client.sendMessageStatic(joinMsg);

            Platform.runLater(() -> {
                Node startPaneNode = uiManager.getComponent("startGamePane");
                Node mainPaneNode = uiManager.getComponent("mainUIPane");

                if (startPaneNode instanceof Pane && mainPaneNode instanceof Pane) {
                    startPaneNode.setVisible(true);
                    mainPaneNode.setVisible(false);
                }
            });
        });
        uiManager.registerComponent("joinGameButton", joinGameButton);



        // Add all to VBox
        mainVBox.getChildren().addAll(
                gameSelect,
                createGameButton,
                overlayInputField,
                joinGameButton

        );

        return mainVBox;
    }

    public static Pane createStartGamePane(UIManager uiManager, GameSessionManager gameSessionManager) {
        VBox startGameVBox = new VBox(10);
        startGameVBox.setPadding(new Insets(20));
        startGameVBox.setAlignment(Pos.TOP_CENTER);
        startGameVBox.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");

        // Get the references from UIManager
        ComboBox<String> gameSelect = (ComboBox<String>) uiManager.getComponent("gameSelect");
        TextField overlayInputField = (TextField) uiManager.getComponent("overlayInputField");


        // Button: Gerald and Alfred
        Button selectGeraldButton = new Button("Select Gerald");
        Button selectAlfredButton = new Button("Select Alfred");

        // Initially hide character select buttons
        selectGeraldButton.setVisible(false);
        selectAlfredButton.setVisible(false);

        // Action handlers for button gerald and alfred
        selectGeraldButton.setOnAction(e -> {
            String sessionId = GameContext.getCurrentGameId();
            if (sessionId == null) return;
            Message msg = new Message("SELECTGO", new Object[]{sessionId, "Gerald"}, "REQUEST");
            Client.sendMessageStatic(msg);

            // Hide both buttons after selection
            selectGeraldButton.setVisible(false);
            selectAlfredButton.setVisible(false);

        });
        uiManager.registerComponent("selectGeraldButton", selectGeraldButton);


        selectAlfredButton.setOnAction(e -> {
            String sessionId = GameContext.getCurrentGameId();
            if (sessionId == null) return;
            Message msg = new Message("SELECTGO", new Object[]{sessionId, "Alfred"}, "REQUEST");
            Client.sendMessageStatic(msg);

            // Hide both buttons after selection
            selectAlfredButton.setVisible(false);
            selectGeraldButton.setVisible(false);
        });
        uiManager.registerComponent("selectAlfredButton", selectAlfredButton);

        // Button: Start Level
        Button startLevelButton = new Button("Start Level");
        startLevelButton.setOnAction(e -> {
            Level level = new Level();
            double screenWidth = startLevelButton.getScene().getWidth();
            double screenHeight = startLevelButton.getScene().getHeight();
            level.initializeLevel(screenWidth, screenHeight);

            String gameId = GameContext.getCurrentGameId();
            Message msg = new Message("STARTGAME", new Object[]{gameId}, "REQUEST");
            Client.sendMessageStatic(msg);

            level.initializeLevel(screenWidth, screenHeight);

            // Hide start button and show character selection buttons
            startLevelButton.setVisible(false);
            selectGeraldButton.setVisible(true);
            selectAlfredButton.setVisible(true);

            System.out.println("Level started!");

            LevelTimer levelTimer = LevelTimer.getInstance();
            levelTimer.start();
            System.out.println("Timer started");

        });
        uiManager.registerComponent("startLevelButton", startLevelButton);

        // Only start button is initially visible
        startGameVBox.getChildren().addAll(
                startLevelButton,
                selectGeraldButton,
                selectAlfredButton
        );

        return startGameVBox;
    }


    public static Pane createAdministrativePane(UIManager uiManager, GameSessionManager gameSessionManager) {
        Pane administrativePane = new Pane();

        TextField gameIdField = new TextField(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        gameIdField.setPromptText("Game ID");
        gameIdField.setEditable(false);
        gameIdField.setPrefWidth(200);
        gameIdField.setLayoutX(50);
        gameIdField.setLayoutY(50);
        uiManager.registerComponent("gameIdField", gameIdField);

        TextField overlayObjInputField = new TextField();
        overlayObjInputField.setPromptText("Enter text...");
        overlayObjInputField.setPrefWidth(120);
        overlayObjInputField.setLayoutX(50);
        overlayObjInputField.setLayoutY(90);
        uiManager.registerComponent("overlayObjInputField", overlayObjInputField);

        Button createObjectButton = new Button("Create Object");
        createObjectButton.setLayoutX(200);
        createObjectButton.setLayoutY(90);
        createObjectButton.setOnAction(e -> {
            String input = overlayObjInputField.getText().trim();
            if (input.isEmpty()) {
                System.out.println("Please enter parameters.");
                return;
            }
            if (input.startsWith("[") && input.endsWith("]")) {
                input = input.substring(1, input.length() - 1);
            }
            Object[] params = MessageCodec.decodeParameters(input);
            Object[] finalParams = new Object[params.length + 1];
            finalParams[0] = GameContext.getCurrentGameId();
            System.arraycopy(params, 0, finalParams, 1, params.length);
            Client.sendMessageStatic(new Message("CREATEGO", finalParams, "REQUEST"));
        });
        uiManager.registerComponent("createObjectButton", createObjectButton);

        TextArea usersListCurrGS = new TextArea();
        usersListCurrGS.setLayoutX(50);
        usersListCurrGS.setLayoutY(140);
        usersListCurrGS.setPrefWidth(300);
        usersListCurrGS.setPrefHeight(150);
        uiManager.registerComponent("gameStateShow", usersListCurrGS);

        Button refreshButton = new Button("Refresh Game Info");
        refreshButton.setLayoutX(50);
        refreshButton.setLayoutY(310);
        refreshButton.setOnAction(e -> {
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

        administrativePane.getChildren().addAll(
                gameIdField,
                overlayObjInputField,
                createObjectButton,
                usersListCurrGS,
                refreshButton
        );

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

        Pane root = new Pane();
        int offsetY = 30;

        // === SCROLLABLE MESSAGE BOX ===
        VBox messagesBox = new VBox(5);
        messagesBox.setPadding(new Insets(5));
        uiManager.registerComponent("chatMessagesBox", messagesBox);

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-radius: 10;");
        scrollPane.setLayoutX(50);
        scrollPane.setLayoutY(20 + offsetY);
        scrollPane.setPrefWidth(300);
        scrollPane.setPrefHeight(150);
        uiManager.registerComponent("chatScroll", scrollPane);

        // === INPUT FIELD ===
        TextField chatInputField = new TextField();
        chatInputField.setPromptText("Type a message...");
        chatInputField.setPrefWidth(200);
        chatInputField.setLayoutX(50);
        chatInputField.setLayoutY(180 + offsetY);

        uiManager.registerComponent("chatInputField", chatInputField);

        // === SEND BUTTON ===
        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        sendButton.setLayoutX(260);
        sendButton.setLayoutY(180 + offsetY);
        sendButton.setPrefWidth(80);

        // === ACTION ===
        sendButton.setOnAction(e -> {
            String username = Client.getInstance().getUsername().get();
            String message = chatInputField.getText();
            if (!message.trim().isEmpty()) {
                Object[] params = new String[]{username, message};
                Message responseMsg = new Message("CHATGLB", params, "REQUEST");
                Client.sendMessageStatic(responseMsg);

                Label msg = new Label(username + ": " + message);
                messagesBox.getChildren().add(msg);
                chatInputField.clear();
                scrollPane.setVvalue(1.0);
            }
        });

        // === Title label : Global Chat ===
        Label titleLabel = new Label("Global Chat");
        titleLabel.setLayoutX(50);
        titleLabel.setLayoutY(0 + offsetY);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");


        // === Add all to root ===
        root.getChildren().addAll(
                titleLabel,
                scrollPane,
                chatInputField,
                sendButton
        );

        return root;
    }


    public static Pane createWhisperChatPane(UIManager uiManager, GameSessionManager gameSessionManager) {

        // Keep the existing BorderPane for the chat
        Pane root = new Pane();
        int offsetY = 30;

        // === (1) Whisper Chat messages ===
        VBox messagesBox = new VBox(5);
        uiManager.registerComponent("whisperChatMessagesBox", messagesBox);
        messagesBox.setPadding(new Insets(5));

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        uiManager.registerComponent("whisperChatScroll", scrollPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-radius: 10;");
        scrollPane.setLayoutX(50);
        scrollPane.setLayoutY(20 + offsetY);
        scrollPane.setPrefWidth(300);
        scrollPane.setPrefHeight(150);

        // === (2) Input field ===
        TextField chatInputField = new TextField();
        chatInputField.setPromptText("Type a message...");
        chatInputField.setPrefWidth(200);
        chatInputField.setLayoutX(50);
        chatInputField.setLayoutY(180 + offsetY);
        uiManager.registerComponent("whisperChatInputField", chatInputField);

        // === (3) Send button ===
        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        sendButton.setPrefWidth(80);
        sendButton.setLayoutX(260);
        sendButton.setLayoutY(180 + offsetY);

        // === (4) Title Label ===
        Label titleLabel = new Label("Whisper Chat");
        titleLabel.setLayoutX(50);
        titleLabel.setLayoutY(0 + offsetY);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
;

        // === (5) Online Users Button ===
        Button OnlineUsersButton = new Button("Online Users Global");
        OnlineUsersButton.setLayoutX(370);
        OnlineUsersButton.setLayoutY(60 + offsetY);
        uiManager.registerComponent("OnlineUsersButton", OnlineUsersButton);

        // On button click, send GETUSERS request
        OnlineUsersButton.setOnAction(e -> {
            // This request presumably returns a list of online users
            Message selectGoMsg = new Message("GETUSERS", new Object[]{}, "REQUEST");
            Client.sendMessageStatic(selectGoMsg);
        });

        // === (6) User selection ComboBox ===
        ComboBox<String> userSelect = new ComboBox<>();
        userSelect.setPromptText("Select a user to whisper");
        userSelect.setLayoutX(370);
        userSelect.setLayoutY(100 + offsetY);
        userSelect.setPrefWidth(200);
        uiManager.registerComponent("whisperUserSelect", userSelect);


        // === (7) Send button logic ===
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

        // === (8) Add all to root pane ===
        root.getChildren().addAll(
                titleLabel,
                scrollPane,
                chatInputField,
                sendButton,
                OnlineUsersButton,
                userSelect
        );

        return root;
    }

    public static Pane createLobbyChatPane(UIManager uiManager, GameSessionManager gameSessionManager) {
        Pane root = new Pane();

        // === (1) Title Label ===
        Label titleLabel = new Label("Lobby Chat");
        titleLabel.setLayoutX(50);
        titleLabel.setLayoutY(30);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        uiManager.registerComponent("lobbyChatTitle", titleLabel);


        // === (2) Users List (Left Side Box) ===
        TextArea usersListCurrGS = new TextArea();
        usersListCurrGS.setText(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        usersListCurrGS.setPromptText("Current Game Users");
        usersListCurrGS.setEditable(false);
        usersListCurrGS.setLayoutX(370);
        usersListCurrGS.setLayoutY(50);
        usersListCurrGS.setPrefSize(180, 190);
        uiManager.registerComponent("usersListCurrGS", usersListCurrGS);


        // === (3) Chat messages ScrollPane ===
        VBox messagesBox = new VBox(5);
        messagesBox.setPadding(new Insets(5));
        uiManager.registerComponent("lobbyChatMessagesBox", messagesBox);

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-radius: 10;");
        scrollPane.setLayoutX(50);
        scrollPane.setLayoutY(50);
        scrollPane.setPrefWidth(300);
        scrollPane.setPrefHeight(150);
        uiManager.registerComponent("lobbyChatScroll", scrollPane);

        // === (4) Chat Input Field ===
        TextField chatInputField = new TextField();
        chatInputField.setPromptText("Type a lobby message...");
        chatInputField.setPrefWidth(200);
        chatInputField.setLayoutX(50);
        chatInputField.setLayoutY(210);
        uiManager.registerComponent("lobbyChatInputField", chatInputField);

        // === (5) Send Button ===
        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        sendButton.setLayoutX(260);
        sendButton.setLayoutY(210);
        sendButton.setPrefWidth(80);

        // === Send Logic ===
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

        root.getChildren().addAll(
                titleLabel,
                usersListCurrGS,
                scrollPane,
                chatInputField,
                sendButton
        );

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
        guiInterfaces.getItems().addAll("Lobby", "Glob Chat", "Lobby Chat", "Whisper Chat", "Administration", "None");
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
                whisperChatPane.setVisible("Whisper Chat".equals(selected));
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

    public static Canvas createCubeCanvas(CubeDrawer drawer) {

        final double WIDTH  = 600;
        final double HEIGHT = 400;
        final double FOV    = 500;
        final double SIZE   = 100;

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        redraw(gc, drawer, WIDTH, HEIGHT, FOV, SIZE);

        canvas.setOnMousePressed(e -> {
            if (drawer.isMouseOverCube(e.getX(), e.getY(),
                    WIDTH / 2, HEIGHT / 2, FOV, SIZE))
            {
                drawer.rotateBy(10);               // +10°
                redraw(gc, drawer, WIDTH, HEIGHT, FOV, SIZE);
            }
        });
        return canvas;
    }

    private static void redraw(GraphicsContext gc, CubeDrawer drawer,
                               double w, double h, double fov, double size) {

        gc.clearRect(0, 0, w, h);
        drawer.drawCube(gc, w / 2, h / 2, fov, size);
    }


} 
