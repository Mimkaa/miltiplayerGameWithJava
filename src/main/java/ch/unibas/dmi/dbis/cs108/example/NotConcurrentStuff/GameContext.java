package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;

public class GameContext {
    private final GameSessionManager gameSessionManager;
    private final Client client;
    // Add other objects here as needed (e.g., ChatManager, AsyncManager, etc.)

    public GameContext() {
        this.gameSessionManager = new GameSessionManager();
        this.client = new Client();
        // Initialize additional objects as needed.
    }

    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }

    public Client getClient() {
        return client;
    }
}
