package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.Level;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

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
                Message joinGameMessage = new Message("JOINGAME", new Object[]{
                        selectedGameName,
                        Client.getInstance().getUsername(),
                        GameContext.getCurrentGameId() == null ? "default" : GameContext.getCurrentGameId()
                }, "REQUEST");
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
            Level level = new Level();
            level.initializeLevel();
            System.out.println("Level started!");
        });

        return mainUIPane;
    }

    public static Pane createAdministrativePane(UIManager uiManager, GameSessionManager gameSessionManager) {
        // ... (existing administrative pane code) ...
        Pane administrativePane = new Pane();
        administrativePane.setTranslateX(0);
        administrativePane.setTranslateY(30);
        administrativePane.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");

        TextArea usersListCurrGS = new TextArea();
        usersListCurrGS.setText(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        usersListCurrGS.setPromptText("Game ID");
        usersListCurrGS.setEditable(false);
        usersListCurrGS.setLayoutX(200);
        usersListCurrGS.setLayoutY(50);
        usersListCurrGS.setPrefWidth(200);
        usersListCurrGS.setPrefHeight(150);
        administrativePane.getChildren().add(usersListCurrGS);
        uiManager.registerComponent("usersListCurrGS", usersListCurrGS);

        Button OnlineUsersButton = new Button("Online Users Global");
        OnlineUsersButton.setLayoutX(200);
        OnlineUsersButton.setLayoutY(210);
        administrativePane.getChildren().add(OnlineUsersButton);
        uiManager.registerComponent("OnlineUsersButton", OnlineUsersButton);
        OnlineUsersButton.setOnAction(e -> {
            Message selectGoMsg = new Message("GETUSERS", new Object[]{}, "REQUEST");
            Client.sendMessageStatic(selectGoMsg);
        });

        TextArea usersList = new TextArea();
        usersList.setText(GameContext.getCurrentGameId() != null ? GameContext.getCurrentGameId() : "");
        usersList.setPromptText("Game ID");
        usersList.setEditable(false);
        usersList.setLayoutX(200);
        usersList.setLayoutY(240);
        usersList.setPrefWidth(200);
        usersList.setPrefHeight(150);
        administrativePane.getChildren().add(usersList);
        uiManager.registerComponent("usersList", usersList);

        return administrativePane;
    }

    public static Button createTogglePaneButton(Pane mainUIPane) {
        Button togglePaneButton = new Button("Toggle UI Pane Visibility");
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
        });

        uiManager.registerComponent("guiInterfaces", guiInterfaces);
        return guiInterfaces;
    }
}
