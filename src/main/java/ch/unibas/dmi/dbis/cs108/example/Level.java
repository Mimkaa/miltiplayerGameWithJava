package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

public class Level {

    public Level() {
        // Constructor
    }

    /**
     * Initialize the level with layout adapted to screen size.
     */
    public void initializeLevel(double screenWidth, double screenHeight) {
        String sessionId = GameContext.getCurrentGameId();
        if (sessionId == null) {
            System.out.println("No session ID. Cannot initialize level.");
            return;
        }

        System.out.println("Initializing level for screen size: " + screenWidth + " x " + screenHeight);

        // === 1. Create 4 floor platforms ===
        for (int i = 0; i < 4; i++) {
            float x = (float) (screenWidth * 0.05 + i * screenWidth * 0.2);
            float y = (float) (screenHeight * 0.75 - i * screenHeight * 0.05);
            float width = (float) (screenWidth * 0.2);
            float height = 20.0f;

            Object[] floorParams = new Object[]{
                    sessionId, "Platform", "Floor" + (i + 1),
                    x, y, width, height, sessionId
            };
            Client.sendMessageStatic(new Message("CREATEGO", floorParams, "REQUEST"));
        }

        // === 2. Key ===
        Object[] keyParams = new Object[]{
                sessionId, "Key", "Key1",
                (float) (screenWidth * 0.15), (float) (screenHeight * 0.15),
                40.0f, 40.0f, 1.0F,sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", keyParams, "REQUEST"));
        // --- Create the key (optional) ---
        // If you still want a key for this level, keep this block:
        //Object[] keyParams = new Object[] {
        //        sessionId,
        //        "Key",
        //        "Key1",
        //        150.0f,   // x
        //        100.0f,   // y
        //        30.0f,    // width
        //        30.0f,    // height
        //        sessionId
        //};
        //Client.sendMessageStatic(new Message("CREATEGO", keyParams, "REQUEST"));

        
        // --- Create two players (Alfred and Gerald) ---
        // Delay to ensure server state is ready before adding players.
        //try {
        //    Thread.sleep(5000);  // 5-second delay
        //} catch (InterruptedException ex) {
        //    Thread.currentThread().interrupt();
        //}

        // === 4. Players ===
        Object[] alfredParams = new Object[]{
                sessionId, "Player2", "Alfred",
                (float) (screenWidth * 0.2), (float) (screenHeight * 0.4),
                40.0f, 40.0f, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", alfredParams, "REQUEST"));

        Object[] geraldParams = new Object[]{
                sessionId, "Player2", "Gerald",
                (float) (screenWidth * 0.25), (float) (screenHeight * 0.4),
                40.0f, 40.0f, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", geraldParams, "REQUEST"));

        // === 5. Final Platform ===
        Object[] finalPlatformParams = new Object[]{
                sessionId, "Platform", "Floor5",
                (float) (screenWidth * 0.85), (float) (screenHeight * 0.65),
                (float) (screenWidth * 0.1), 20.0f, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", finalPlatformParams, "REQUEST"));

        // === 6. Door ===
        Object[] doorParams = new Object[]{
                sessionId, "Door", "Door1",
                (float) (screenWidth * 0.88), (float) (screenHeight * 0.5),
                50.0f, 120.0f, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", doorParams, "REQUEST"));

        System.out.println("Level initialized (responsive layout).");
    }
}
