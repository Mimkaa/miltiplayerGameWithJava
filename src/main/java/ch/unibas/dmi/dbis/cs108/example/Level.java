package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

public class Level {

    public Level() {
        // Constructor can set up level parameters if needed.
    }

    public void initializeLevel() {
        // Example: Create a floor platform.
        String sessionId = GameContext.getCurrentGameId();
        Object[] floorParams = new Object[] {
                sessionId, "Platform", "Floor", 100.0f, 300.0f, 20000.0f, 20.0f, sessionId
        };
        // Build and send the CREATEGO message for the floor.
        Client.sendMessageStatic(new ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message("CREATEGO", floorParams, "REQUEST"));

        // Create additional level objects (doors, keys, etc.) similarly.
        // For example, create a door:
        Object[] doorParams = new Object[] {
                sessionId, "Door", "Door1", 850.0f, 280.0f, 50.0f, 120.0f, sessionId
        };
        Client.sendMessageStatic(new ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message("CREATEGO", doorParams, "REQUEST"));

        // And a key:
        Object[] keyParams = new Object[] {
                sessionId, "Key", "Key1", 150.0f, 100.0f, 30.0f, 30.0f, sessionId
        };
        Client.sendMessageStatic(new ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message("CREATEGO", keyParams, "REQUEST"));
    }
}
