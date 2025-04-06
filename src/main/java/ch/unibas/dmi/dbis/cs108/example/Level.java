package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

public class Level {

    public Level() {
        // Constructor can set up level parameters if needed.
    }

    public void initializeLevel() {
        String sessionId = GameContext.getCurrentGameId();
        System.out.println("Game Session ID: " + sessionId);
        if (sessionId == null) {
            System.out.println("No session id available. Level cannot be initialized.");
            return;
        }

        // Create multiple floor platforms.
        // Expected parameter order (for Platform):
        // [sessionId, objectType, objectName, x, y, width, height]
        for (int i = 0; i < 5; i++) {
            float x = 100.0f + i * 300.0f;
            float y = 300.0f - i * 30.0f;
            float width = 250.0f;
            float height = 20.0f;
            Object[] floorParams = new Object[] {
                    sessionId,                 // game session id
                    "Platform",                // object type
                    "Floor" + (i + 1),         // object name
                    x,                         // x-coordinate
                    y,                         // y-coordinate
                    width,                     // width
                    height                     // height
            };
            Client.sendMessageStatic(new Message("CREATEGO", floorParams, "GAME"));
        }

        // Create the door.
        // Expected parameter order (for Door):
        // [sessionId, objectType, objectName, x, y, width, height]
        Object[] doorParams = new Object[] {
                sessionId,    // game session id
                "Door",       // object type
                "Door1",      // object name
                850.0f,       // x-coordinate
                280.0f,       // y-coordinate
                50.0f,        // width
                120.0f        // height
        };
        Client.sendMessageStatic(new Message("CREATEGO", doorParams, "GAME"));

        // Create the key.
        // Expected parameter order (for Key):
        // [sessionId, objectType, objectName, x, y, width, height, mass]
        Object[] keyParams = new Object[] {
                sessionId,    // game session id
                "Key",        // object type
                "Key1",       // object name
                150.0f,       // x-coordinate
                100.0f,       // y-coordinate
                30.0f,        // width
                30.0f,        // height
                100.0f        // mass
        };
        Client.sendMessageStatic(new Message("CREATEGO", keyParams, "GAME"));

        // Create two players: Alfred and Gerald.
        // Expected parameter order (for Player):
        // [sessionId, objectType, objectName, x, y, width, height]
        Object[] alfredParams = new Object[] {
                sessionId,    // game session id
                "Player",     // object type
                "Alfred",     // object name
                50.0f,        // x-coordinate
                200.0f,       // y-coordinate
                40.0f,        // width
                40.0f         // height
        };
        Client.sendMessageStatic(new Message("CREATEGO", alfredParams, "GAME"));

        Object[] geraldParams = new Object[] {
                sessionId,    // game session id
                "Player",     // object type
                "Gerald",     // object name
                150.0f,       // x-coordinate
                200.0f,       // y-coordinate
                40.0f,        // width
                40.0f         // height
        };
        Client.sendMessageStatic(new Message("CREATEGO", geraldParams, "GAME"));

        System.out.println("Level started! Multiple floors, Door, Key, and two Players (Alfred & Gerald) created.");
    }
}
