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
            float y = 700.0f - i * 30.0f;
            float width = 250.0f;
            float height = 20.0f;
            Object[] floorParams = new Object[] {
                    sessionId,                 // game session id
                    "Platform",                // object type
                    "Floor" + (i + 1),         // object name
                    x,                         // x-coordinate
                    y,                         // y-coordinate
                    width,                     // width
                    height,
                    sessionId
            };
            Client.sendMessageStatic(new Message("CREATEGO", floorParams, "REQUEST"));
        }

        // Create the door.
        // Expected parameter order (for Door):
        // [sessionId, objectType, objectName, x, y, width, height]
        Object[] doorParams = new Object[] {
                sessionId,    // game session id
                "Door",       // object type
                "Door1",      // object name
                850.0f,       // x-coordinate
                300.0f,       // y-coordinate
                50.0f,        // width
                120.0f,
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", doorParams, "REQUEST"));

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
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", keyParams, "REQUEST"));

        // Create two players: Alfred and Gerald.
        // Expected parameter order (for Player):
        // [sessionId, objectType, objectName, x, y, width, height]
        Object[] alfredParams = new Object[] {
                sessionId,    // game session id
                "Player2",     // object type
                "Alfred",     // object name
                200.0f,        // x-coordinate
                200.0f,       // y-coordinate
                40.0f,        // width
                40.0f,
                sessionId

        };
        Client.sendMessageStatic(new Message("CREATEGO", alfredParams, "REQUEST"));

        Object[] geraldParams = new Object[] {
                sessionId,    // game session id
                "Player2",     // object type
                "Gerald",     // object name
                150.0f,       // x-coordinate
                200.0f,       // y-coordinate
                40.0f,        // width
                40.0f,
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", geraldParams, "REQUEST"));

        // ------------------------------------------------
        // Create a moving platform from x=1300,y=580 to x=850,y=300
        // using your MovingPlatform constructor:
        // (String name, float startX, float endX, float startY, float endY,
        //  float width, float height, float periodX, float periodY, String gameId)
        // ------------------------------------------------

        Object[] floorParams = new Object[] {
                sessionId,                 // game session id
                "Platform",                // object type
                "Floor 5",                  // object name
                900,                         // x-coordinate
                400,                         // y-coordinate
                100,                        // width
                20,
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", floorParams, "REQUEST"));


        System.out.println("Level started! Multiple floors, Door, Key, and two Players (Alfred & Gerald) created.");
    }
}
