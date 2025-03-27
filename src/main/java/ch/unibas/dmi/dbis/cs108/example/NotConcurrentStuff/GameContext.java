package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import javafx.application.Application;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;



public class GameContext {
    private final GameSessionManager gameSessionManager;
    private final Client client;
    private MessageHogger testHogger;
    
    // AtomicReference to store the current game ID in a thread-safe way.
    private final AtomicReference<String> currentGameId = new AtomicReference<>();
    // AtomicReference to store the selected game object's UUID.
    private final AtomicReference<String> selectedGameObjectId = new AtomicReference<>();

    public GameContext() {
        this.gameSessionManager = new GameSessionManager();
        this.client = new Client();

        // Initialize the custom MessageHogger.
        testHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message receivedMessage) {
                String type = receivedMessage.getMessageType();
                if ("CREATEGAME".equals(type)) {
                    System.out.println("Creating a new game");
                    // Expecting two parameters: [gameId, gameName]
                    String receivedId = receivedMessage.getParameters()[0].toString();
                    String receivedGameName = receivedMessage.getParameters()[1].toString();
                    gameSessionManager.addGameSession(receivedId, receivedGameName);
                    // Optionally update the current game id.
                    currentGameId.set(receivedId);
                    System.out.println("Game created with id: " + receivedId);
                } else if ("JOINGAME".equals(type)) {
                    System.out.println("Joining game");
                    // Retrieve the game id from the message's first parameter.
                    String gameID = receivedMessage.getParameters()[0].toString();
                    currentGameId.set(gameID);
                    System.out.println("Current game id set to: " + currentGameId.get());
                } else if ("SELECTGO".equals(type)) {
                    System.out.println("Processing SELECTGO command");
                    // Expect the target GameObject id in the first parameter.
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
                    // Retrieve the game from the session manager.
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
                } else {
                    System.out.println("Unknown message type: " + type);
                }
            }
        };
    }

    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }

    public Client getClient() {
        return client;
    }
    
    public String getCurrentGameId() {
        return currentGameId.get();
    }
    
    public String getSelectedGameObjectId() {
        return selectedGameObjectId.get();
    }

    /**
     * Starts the client operations.
     */
    public void start() {
        client.setUsername("tsdfg");
        System.out.println("Set client username: tsdfg");
        new Thread(client::run).start();
        client.startConsoleReaderLoop();
    }

    /**
     * Sends a CREATEGAME message to the client.
     */
    public void sendCreateGameToClient(String gameName) {
        // For testing, we use "GameSession1" as the game id.
        Message createGameMessage = new Message("CREATEGAME", new Object[]{"GameSession1", gameName}, "REQUEST");
        Client.sendMessageStatic(createGameMessage);
    }
    
    /**
     * Sends a JOINGAME message to the client.
     */
    public void sendJoinGameToClient() {
        Message joinGameMessage = new Message("JOINGAME", new Object[]{"GameSession1"}, "REQUEST");
        Client.sendMessageStatic(joinGameMessage);
    }
    
    /**
     * Sends a SELECTGO message to the client with a target GameObject id.
     */
    public void sendSelectGOToClient(String targetGameObjectId) {
        Message selectGOMsg = new Message("SELECTGO", new Object[]{targetGameObjectId}, "REQUEST");
        Client.sendMessageStatic(selectGOMsg);
    }
    
    /**
     * Prints all game sessions stored in the GameSessionManager.
     */
    public void printAllGameSessions() {
        System.out.println("📦 All registered game sessions:");
        gameSessionManager.getAllGameSessions().forEach((gameId, game) -> {
            System.out.println("🆔 Game ID: " + gameId + " | Name: " + game.getGameName());
        });
    }
    
    public static void main(String[] args) {
        // Start non-GUI code (client, game context, etc.) on a separate thread.
        new Thread(() -> {
            GameContext context = new GameContext();
            context.start();
            // For testing, send a CREATEGAME message.
            context.sendCreateGameToClient("Test Game");
            try {
                Thread.sleep(2000);
                context.sendJoinGameToClient();
                Thread.sleep(2000);
                // For testing, send SELECTGO with a dummy id; in a real scenario, this id should be that of an existing object.
                context.sendSelectGOToClient("someGameObjectId");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Optionally, print out all game sessions.
            try {
                Thread.sleep(2000);
                context.printAllGameSessions();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    
        // Launch the JavaFX GUI. This call blocks until the GUI is closed.
        Application.launch(GUI.class, args);
    }
}
