package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import java.util.UUID;

public class Level {

    public Level() {
        // Constructor can set up level parameters if needed.
    }

    public void initializeLevel() {
        String sessionId = GameContext.getCurrentGameId();
        if (sessionId == null) {
            System.out.println("No session id available. Level cannot be initialized.");
            return;
        }

        // Create multiple floor platforms.
        // For example, let's create 5 platforms with increasing x positions and varying y positions.
        for (int i = 0; i < 5; i++) {
            // Adjust these values to your liking.
            float x = 100.0f + i * 300.0f;
            float y = 300.0f - i * 30.0f; // Slightly higher as we go along.
            float width = 250.0f;         // Platform width
            float height = 20.0f;         // Platform height
            Object[] floorParams = new Object[] {
                    UUID.randomUUID().toString(),  // Unique ID for the platform
                    sessionId,                     // Target game session ID
                    "Platform",                    // Object type must match your factory key
                    "Floor" + (i + 1),             // Name of the platform (e.g., Floor1, Floor2, …)
                    x,                             // x-coordinate
                    y,                             // y-coordinate
                    width,                         // width
                    height,                        // height
                    sessionId                      // gameId for the constructor
            };
            Client.sendMessageStatic(new Message("CREATEGO", floorParams, "REQUEST"));
        }

        // Create the door.
        Object[] doorParams = new Object[] {
                UUID.randomUUID().toString(),
                sessionId,
                "Door",
                "Door1",
                850.0f,   // x-coordinate
                280.0f,   // y-coordinate
                50.0f,    // width
                120.0f,   // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", doorParams, "REQUEST"));

        // Create the key.
        Object[] keyParams = new Object[] {
                UUID.randomUUID().toString(),
                sessionId,
                "Key",
                "Key1",
                150.0f,   // x-coordinate
                100.0f,   // y-coordinate
                30.0f,    // width
                30.0f,    // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", keyParams, "REQUEST"));

        // Create two players: Alfred and Gerald.
        Object[] alfredParams = new Object[] {
                UUID.randomUUID().toString(),
                sessionId,
                "Player",
                "Alfred",
                50.0f,   // x-coordinate
                200.0f,  // y-coordinate
                40.0f,   // width
                40.0f,   // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", alfredParams, "REQUEST"));

        Object[] geraldParams = new Object[] {
                UUID.randomUUID().toString(),
                sessionId,
                "Player",
                "Gerald",
                150.0f,  // x-coordinate
                200.0f,  // y-coordinate
                40.0f,   // width
                40.0f,   // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", geraldParams, "REQUEST"));

        System.out.println("Level started! Multiple floors, Door, Key, and two Players (Alfred & Gerald) created.");
    }
}
