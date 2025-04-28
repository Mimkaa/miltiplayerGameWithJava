package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

/**
 * Represents a game level that can be initialized with a responsive layout.
 * This class is responsible for creating and positioning the platforms, key, players,
 * and other objects within the level, adapting to the screen size.
 */
public class Level {

    private boolean initialised = false;

    /**
     * Constructs a new Level instance.
     * Initializes the level only once when called for the first time.
     */
    public Level() {
        // Konstruktor
    }

    /**
     * Initializes the level with a layout adapted to the screen size.
     * Ensures that initialization happens only once by checking the {@code initialised} flag.
     *
     * @param screenWidth  The width of the screen.
     * @param screenHeight The height of the screen.
     */
    public void initializeLevel(double screenWidth, double screenHeight) {
        if (initialised) {
            return;
        }
        initialised = true;

        // Get the current game session ID from the GameContext
        String sessionId = GameContext.getCurrentGameId();
        if (sessionId == null) {
            System.out.println("No session ID. Cannot initialize level.");
            return;
        }

        // Print out screen size information for debugging
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
                40.0f, 40.0f, 1.0F, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", keyParams, "REQUEST"));

        // === 3. Players ===
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

        // === 4. Final Platform ===
        Object[] finalPlatformParams = new Object[]{
                sessionId, "Platform", "Floor5",
                (float) (screenWidth * 0.85), (float) (screenHeight * 0.65),
                (float) (screenWidth * 0.1), 20.0f, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", finalPlatformParams, "REQUEST"));

        // === 5. Door ===
        Object[] doorParams = new Object[]{
                sessionId, "Door", "Door1",
                (float) (screenWidth * 0.87), (float) (screenHeight * 0.50),
                50.0f, 120.0f, sessionId
        };
        Client.sendMessageStatic(new Message("CREATEGO", doorParams, "REQUEST"));

        System.out.println("Level initialized (responsive layout).");
    }
}
