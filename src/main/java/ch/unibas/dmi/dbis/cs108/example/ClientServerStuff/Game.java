package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObjectFactory;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Player;
import javafx.scene.canvas.GraphicsContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * The {@code Game} class manages a collection of {@link GameObject} instances within a
 * game session. It provides methods to create objects, process inbound messages, update
 * object states, and handle basic collision detection and resolution.
 */
@Getter
public class Game {

    /**
     * A unique string identifier for this game session.
     */
    private final String gameId;

    /**
     * A descriptive name for this game session (e.g., "MyGameSession").
     */
    private final String gameName;

    /**
     * A thread-safe list holding all {@link GameObject} instances within this game.
     */
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();

    /**
     * A {@link MessageHogger} dedicated to handling messages specifically tagged as "GAME" messages.
     */
    private final MessageHogger gameMessageHogger;

    private boolean startedFlag = false;

    // Creates a concurrent Set<String> backed by a ConcurrentHashMap
    /**
     * A concurrent set of usernames (or user IDs) currently in the game,
     * backed by a {@link ConcurrentHashMap}.
     */
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a new {@code Game} instance with the given unique ID and name.
     * This also initializes a {@link MessageHogger} for GAME messages and starts
     * the main processing loop for player commands.
     *
     * @param gameId   The unique ID of this game session.
     * @param gameName The human-readable name for this game session.
     */
    public Game(String gameId, String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;

        // Initialize the dedicated message hogger for GAME messages.
        this.gameMessageHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message msg) {
                // Only process if msg.getOption() is "GAME"
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

        // Start a single main loop for processing all game objects.
        startPlayersCommandProcessingLoop();
    }

    public void setStartedFlag(boolean state)
    {
        startedFlag = state;
    }

    public boolean getStartedFlag()
    {
        return startedFlag;
    }

    /**
     * Adds an incoming {@link Message} to the asynchronous queue, which will then
     * be routed to the correct {@link GameObject}.
     *
     * @param msg The {@link Message} to add for processing.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Finds the correct {@link GameObject} based on the first concealed parameter (UUID)
     * and routes the given {@link Message} to that object's message queue.
     *
     * @param msg The {@link Message} to route.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetObjectUuid = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getId().equals(targetObjectUuid)) {
                    // Directly add to object's message queue.
                    go.addIncomingMessage(msg);
                    System.out.println("Routed message to GameObject with UUID: " + targetObjectUuid);
                    return;
                }
            }
        }
    }

    /**
     * Creates a new {@link GameObject} of the specified type with the given UUID and parameters.
     * This method runs asynchronously and returns a {@link Future} that resolves to the
     * created {@link GameObject}.
     *
     * @param type  The type of the object to create (e.g., "Player", "NPC", etc.).
     * @param uuid  The unique identifier to assign to this object.
     * @param params Additional parameters required for constructing the object.
     * @return A {@link Future} resolving to the newly created {@link GameObject}.
     */
    public Future<GameObject> addGameObjectAsync(String type, String uuid, Object... params) {
        return AsyncManager.run(() -> {
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            gameObjects.add(newObject);
            return newObject;
        });
    }

    /**
     * Starts the main loop for processing commands and updates for all {@link GameObject}s.
     * <p>This loop runs asynchronously and performs:</p>
     * <ul>
     *   <li>Draining inbound messages for each object.</li>
     *   <li>Processing commands for each object.</li>
     *   <li>Updating local object states based on elapsed time (deltaTime).</li>
     *   <li>Checking and resolving collisions between collidable objects.</li>
     * </ul>
     */
    public void startPlayersCommandProcessingLoop() {
        final long[] lastUpdate = { System.nanoTime() };
        AsyncManager.runLoop(() -> {
            long now = System.nanoTime();
            float deltaTime = (now - lastUpdate[0]) / 1_000_000_000f; // convert nanoseconds to seconds
            lastUpdate[0] = now;

            // Process each object's messages, commands, and update with deltaTime.
            for (GameObject go : gameObjects) {
                go.processIncomingMessages();
                go.processCommands();
                go.myUpdateLocal(deltaTime);
            }

            // Check and resolve collisions among collidable objects.
            for (int i = 0; i < gameObjects.size(); i++) {
                GameObject a = gameObjects.get(i);
                if (!a.isCollidable()) continue;
                // Reset collision flag for visual feedback if applicable.
                if (a instanceof Player) {
                    ((Player) a).setCollisionDetected(false);
                }
                for (int j = i + 1; j < gameObjects.size(); j++) {
                    GameObject b = gameObjects.get(j);
                    if (!b.isCollidable()) continue;
                    if (b instanceof Player) {
                        ((Player) b).setCollisionDetected(false);
                    }
                    if (a.intersects(b)) {
                        // Resolve the collision (push objects apart).
                        a.resolveCollision(b);
                        // Mark collision so that drawing can change colors.
                        if (a instanceof Player) {
                            ((Player) a).setCollisionDetected(true);
                        }
                        if (b instanceof Player) {
                            ((Player) b).setCollisionDetected(true);
                        }
                        // System.out.println("Collision resolved between " + a.getName() + " and " + b.getName());
                    }
                }
            }
        });
    }

    /**
     * Draws all {@link GameObject}s onto the provided JavaFX {@link GraphicsContext}.
     *
     * @param gc The {@link GraphicsContext} on which the objects will be drawn.
     */
    public void draw(GraphicsContext gc) {
        for (GameObject go : gameObjects) {
            go.draw(gc);
        }
    }

    /**
     * Gracefully shuts down this game, stopping the asynchronous manager
     * and freeing any associated resources.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }
}
