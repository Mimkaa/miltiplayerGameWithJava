package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.*;
import ch.unibas.dmi.dbis.cs108.example.highscore.LevelTimer;
import javafx.scene.canvas.GraphicsContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * The {@code Game} class manages a collection of {@link GameObject}s within a given
 * game or session. It provides methods to create, update, and route messages to
 * these objects. It supports both authoritative and non-authoritative modes.
 */
@Getter
public class Game {

    private final String gameId;
    private final String gameName;
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
    private final MessageHogger gameMessageHogger;

    // Timer for the level
    private LevelTimer levelTimer;

    // Whether the game has started.
    private boolean startedFlag = false;

    // A concurrent Set<String> backed by a ConcurrentHashMap to track users.
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    // Flag indicating if this game is authoritative or not.
    private boolean authoritative = false;

    // Fields for controlling framerate and tracking ticks
    private volatile int targetFps = 60;         // Desired frames per second
    private volatile long tickCount = 0;         // Increments each loop

    /**
     * Constructs a new {@code Game} instance with the given game ID and name.
     * Initializes the game objects, message hogger, and starts the command processing loop.
     *
     * @param gameId   The ID of the game.
     * @param gameName The name of the game.
     */
    public Game(String gameId, String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;

        // Initialize the dedicated message hogger for GAME messages.
        this.gameMessageHogger = new MessageHogger() {
            @Override
            protected void processBestEffortMessage(Message msg) {
                // Only process if msg.getOption() == "GAME"
                if ("GAME".equalsIgnoreCase(msg.getOption())) {
                    String[] concealed = msg.getConcealedParameters();
                    if (concealed != null && concealed.length >= 2) {
                        String targetGameObjectUuid = concealed[0];
                        String msgGameUuid = concealed[1];
                        if (msgGameUuid.equals(Game.this.gameId)) {
                            routeMessageToGameObject(msg);
                        } else {
                            System.out.println("Ignoring GAME message: mismatch with " + Game.this.gameId);
                        }
                    } else {
                        System.out.println("Ignoring GAME message: insufficient concealed parameters.");
                    }
                } else {
                    System.out.println("Ignoring non-GAME message in gameMessageHogger: " + msg.getMessageType());
                }
            }
        };

        // Start the main loop for processing all game objects at a fixed framerate.
        startPlayersCommandProcessingLoop();
    }

    /**
     * Sets whether the game is authoritative or not.
     *
     * @param authoritative {@code true} if the game is authoritative, {@code false} otherwise.
     */
    public void setAuthoritative(boolean authoritative) {
        this.authoritative = authoritative;
    }

    /**
     * Returns whether the game is authoritative or not.
     *
     * @return {@code true} if the game is authoritative, {@code false} otherwise.
     */
    public boolean isAuthoritative() {
        return authoritative;
    }

    /**
     * Queues an asynchronous task to process an incoming message,
     * then routes it to the correct {@code GameObject}.
     *
     * @param msg The message to be processed.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Routes the message to the correct {@code GameObject} by matching the first concealed
     * parameter to the object's UUID.
     *
     * @param msg The message to route to the game object.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetObjectUuid = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getId().equals(targetObjectUuid)) {
                    go.addIncomingMessage(msg);
                    System.out.println("Routed message to GameObject with UUID: " + targetObjectUuid);
                    return;
                }
            }
        }
    }

    /**
     * Starts the level and the associated timer.
     * Marks the game as started.
     */
    public void startLevel() {
        if (!startedFlag) {
            levelTimer.start();  // Starts the level timer
            startedFlag = true;   // Game marked as started
            System.out.println("Level started. Timer started.");
        }
    }

    /**
     * Stops the level and the associated timer.
     * Marks the game as stopped.
     */
    public void stopLevel() {
        if (startedFlag) {
            levelTimer.stop();  // Stops the timer
            startedFlag = false;  // Game marked as stopped
            System.out.println("Level stopped. Timer stopped.");
        }
    }

    /**
     * Returns the elapsed time for the level in seconds.
     *
     * @return The elapsed time in seconds.
     */
    public long getElapsedTime() {
        return levelTimer.getElapsedTimeInSeconds();
    }

    /**
     * Getter for the started flag.
     *
     * @return {@code true} if the game has started, {@code false} otherwise.
     */
    public boolean getStartedFlag() {
        return startedFlag;
    }

    /**
     * Setter for the started flag.
     *
     * @param state The state to set for the started flag.
     */
    public void setStartedFlag(boolean state) {
        this.startedFlag = state;
    }

    /**
     * Creates a new {@code GameObject} of the specified type and parameters,
     * assigns it the given UUID, and adds it to this game.
     *
     * @param type The type of the {@code GameObject} to create.
     * @param uuid The UUID of the new {@code GameObject}.
     * @param params Additional parameters to initialize the new object.
     * @return A {@code Future} representing the asynchronous creation of the object.
     */
    public Future<GameObject> addGameObjectAsync(String type, String uuid, Object... params) {
        return AsyncManager.run(() -> {
            // 1) Look for an existing object with this UUID
            for (GameObject go : gameObjects) {
                if (uuid.equals(go.getId())) {
                    // Found—just return it (no re-creation)
                    return go;
                }
            }

            // 2) Not found, so create, assign id, add, wire up:
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            newObject.setParentGame(this);
            gameObjects.add(newObject);
            return newObject;
        });
    }

    /**
     * The main loop that processes all objects at a fixed framerate (targetFps).
     * This loop:
     * 1) Drains inbound messages for each object,
     * 2) Processes commands for each object,
     * 3) Performs local updates with deltaTime,
     * 4) Checks and resolves collisions among collidable objects,
     * 5) Increments tickCount,
     * 6) Sleeps the thread to maintain the target framerate.
     */
    public void startPlayersCommandProcessingLoop() {
        final long[] lastFrameTime = { System.nanoTime() };

        AsyncManager.runLoop(() -> {
            long startFrameTime = System.nanoTime();
            float deltaTime = (startFrameTime - lastFrameTime[0]) / 1_000_000_000f;
            lastFrameTime[0] = startFrameTime;

            // 1) & 2) Process inbound messages & commands, then update each GameObject
            for (GameObject go : gameObjects) {
                go.processIncomingMessages();
                //go.processCommands();
                go.myUpdateLocal(deltaTime);
            }

            // 3) Check and resolve collisions among collidable objects
            for (int i = 0; i < gameObjects.size(); i++) {
                GameObject a = gameObjects.get(i);
                if (!a.isCollidable()) continue;
                for (int j = i + 1; j < gameObjects.size(); j++) {
                    GameObject b = gameObjects.get(j);
                    if (!b.isCollidable()) continue;

                    if (a.intersects(b)) {
                        if (a instanceof Key && b instanceof Door) {
                            Message winMsg = new Message("WIN", new Object[]{"You won the game!"}, "RESPONSE");
                            System.out.println("key door collision");
                            //Server.getInstance().broadcastMessageToAll(winMsg);
                        }
                        a.resolveCollision(b);
                        // Example: zero out y velocity if Player2 collides with Platform
                        if (a instanceof Player2 && b instanceof Platform) {
                            ((Player2) a).getVel().y = 0;
                        } else if (b instanceof Player2 && a instanceof Platform) {
                            ((Player2) b).getVel().y = 0;
                        }

                        if (a instanceof Key && b instanceof Platform) {
                            ((Key) a).setVelocityY(0.0f);
                        } else if (b instanceof Key && a instanceof Platform) {
                            ((Key) b).setVelocityY(0.0f);
                        }
                    }
                }
            }

            // 4) Increment the global tickCount
            tickCount++;

            // 5) Sleep to maintain the target framerate
            long targetFrameTimeNanos = 1_000_000_000L / targetFps;
            long frameProcessingTime = System.nanoTime() - startFrameTime;
            long sleepTimeNanos = targetFrameTimeNanos - frameProcessingTime;

            if (sleepTimeNanos > 0) {
                try {
                    // Sleep for the leftover time, in nanoseconds
                    Thread.sleep(
                            sleepTimeNanos / 1_000_000,
                            (int) (sleepTimeNanos % 1_000_000)
                    );
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /**
     * Returns the current tick count of the game loop.
     *
     * @return The current tick count.
     */
    public long getTickCount() {
        return tickCount;
    }

    /**
     * Draws all game objects onto the provided JavaFX {@code GraphicsContext}.
     *
     * @param gc The {@code GraphicsContext} to draw the game objects onto.
     */
    public void draw(GraphicsContext gc) {
        for (GameObject go : gameObjects) {
            go.draw(gc);
        }
    }

    /**
     * Returns the ID of the selected game object, if any.
     *
     * @return The ID of the selected game object, or {@code null} if no object is selected.
     */
    public String getSelectedGameObjectId() {
        for (GameObject go : gameObjects) {
            if (go.isSelected()) {
                return go.getId();
            }
        }
        return null;
    }

    /**
     * Gracefully shuts down the game by stopping the asynchronous manager.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }

    /**
     * Checks if a game object with the given name already exists in this game.
     * This helps prevent duplicate object creation (e.g., duplicate players).
     *
     * @param name The name of the game object to check.
     * @return {@code true} if a game object with the given name exists, {@code false} otherwise.
     */
    public boolean containsObjectByName(String name) {
        for (GameObject go : gameObjects) {
            if (go.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
