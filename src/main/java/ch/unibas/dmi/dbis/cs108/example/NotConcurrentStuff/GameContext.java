package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;

import java.util.Random;
import java.util.UUID;

public class GameContext {
    private final GameSessionManager gameSessionManager;
    private final Client client;
    MessageHogger testHogger;
    // Add other objects here as needed (e.g., ChatManager, AsyncManager, etc.)

    public GameContext() {
        this.gameSessionManager = new GameSessionManager();
        this.client = new Client();

        // Initialize the custom MessageHogger.
        testHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message recievedMessage) {
                if ("CREATEGAME".equals(recievedMessage.getMessageType())) {
                    System.out.println("Creating a new game");
                    String recievedId = recievedMessage.getParameters()[0].toString();
                    String recievedGameName = recievedMessage.getParameters()[1].toString();
                    gameSessionManager.addGameSession(recievedId,recievedGameName);

                }
            }

        };
    };
        // Initialize additional objects as needed.


    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }

    public Client getClient() {
        return client;
    }

    public void start(){
        client.setUsername("tsdfg");
        System.out.println("Set client username: " + "tsdfg");

        //client.initChatManager();
        //client.startGraphicsStuff();
        new Thread(client::run).start();
        client.startConsoleReaderLoop();

    }


    public void sendCreateGametoClient(String gameName) {

        Message createGameMessage = new Message("CREATEGAME", new Object[]{"GameSession1"}, "REQUEST");
        Client.sendMessageStatic(createGameMessage);
    }

    public void printAllGameSessions() {
        System.out.println("📦 All registered game sessions:");
        gameSessionManager.getAllGameSessions().forEach((gameId, game) -> {
            System.out.println("🆔 Game ID: " + gameId + " | Name: " + game.getGameName());
        });
    }


}
