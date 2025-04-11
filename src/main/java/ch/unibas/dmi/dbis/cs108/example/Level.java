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

        // --- Create 4 "normal" floor platforms ---
        // We'll space them out so they still fit on screen more easily.
        for (int i = 0; i < 4; i++) {
            float x = 100.0f + i * 300.0f;   // Adjust if you need smaller gaps
            float y = 700.0f - i * 30.0f;
            float width = 250.0f;
            float height = 20.0f;
            Object[] floorParams = new Object[] {
                    sessionId,                  // game session id
                    "Platform",                 // object type
                    "Floor" + (i + 1),          // object name
                    x,                          // x-coordinate
                    y,                          // y-coordinate
                    width,                      // width
                    height,
                    sessionId
            };
            Client.sendMessageStatic(new Message("CREATEGO", floorParams, "REQUEST"));
        }

        // --- Create the key (optional) ---
        // If you still want a key for this level, keep this block:
        Object[] keyParams = new Object[] {
                sessionId,
                "Key",
                "Key1",
                150.0f,   // x
                100.0f,   // y
                30.0f,    // width
                30.0f,    // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", keyParams, "REQUEST"));

        // --- Create two players (Alfred and Gerald) ---
        // Delay to ensure server state is ready before adding players.
        try {
            Thread.sleep(5000);  // 5-second delay
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        Object[] alfredParams = new Object[] {
                sessionId,
                "Player2",
                "Alfred",
                200.0f,   // x
                200.0f,   // y
                40.0f,    // width
                40.0f,
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", alfredParams, "REQUEST"));

        Object[] geraldParams = new Object[] {
                sessionId,
                "Player2",
                "Gerald",
                150.0f,   // x
                200.0f,   // y
                40.0f,
                40.0f,
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", geraldParams, "REQUEST"));

        // --- Create the final distant platform (Floor5) ---
        // Placed far enough that you can't reach it by just jumping.
        Object[] finalPlatformParams = new Object[] {
                sessionId,
                "Platform",
                "Floor5",
                1315.0f,  // x-coordinate (far gap)
                580.0f,   // y-coordinate
                150.0f,   // width
                20.0f,    // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", finalPlatformParams, "REQUEST"));

        // --- Place the door on the final platform ---
        // Position the door so that it's flush on top of Floor5 (height=20, door=120).
        // If you want its bottom edge exactly on y=580, set this to (x, 580) and keep in mind the door extends up to 700.
        // Or position it slightly above the platform so only the door’s bottom rests on it.
        Object[] doorParams = new Object[] {
                sessionId,
                "Door",
                "Door1",
                1350.0f,   // x = centered horizontally on the 150-width platform
                460.0f,    // y = the door's bottom, so door extends up 120 units to 580
                50.0f,     // width
                120.0f,    // height
                sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", doorParams, "REQUEST"));

        System.out.println("Level started! 4 normal floors, 1 far platform, and door requiring player-throwing to reach.");
    }
}
