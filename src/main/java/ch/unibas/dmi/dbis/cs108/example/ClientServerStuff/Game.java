package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObjectFactory;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Player2;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Platform;
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
 * these objects. Supports both authoritative and non-authoritative modes.
 */
@Getter
public class Game {

    private final String gameId;
    private final String gameName;
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
    private final MessageHogger gameMessageHogger;

    // Whether the game has started.
    private boolean startedFlag = false;

    // A concurrent Set<String> backed by a ConcurrentHashMap
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    // Flag indicating if this game is authoritative or not.
    private boolean authoritative = false;

    // === New fields for controlling framerate and tracking ticks ===
    private volatile int targetFps = 60;         // desired frames per second
    private volatile long tickCount = 0;         // increments each loop

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

    public void setStartedFlag(boolean state) {
        this.startedFlag = state;
    }

    public boolean getStartedFlag() {
        return startedFlag;
    }

    public void setAuthoritative(boolean authoritative) {
        this.authoritative = authoritative;
      
    }

    public boolean isAuthoritative() {
        return authoritative;
    }

    /**
     * Queues an asynchronous task to process an incoming message,
     * then routes it to the correct GameObject.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Routes the message to the correct GameObject by matching the first concealed
     * parameter to the object's UUID.
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
     * Creates a new GameObject of the specified type and parameters,
     * assigns it the given UUID, and adds it to this game.
     */
    public Future<GameObject> addGameObjectAsync(String type, String uuid, Object... params) {
        return AsyncManager.run(() -> {
            // 1) look for an existing object with this UUID
            for (GameObject go : gameObjects) {
                if (uuid.equals(go.getId())) {
                    // found—just return it (no re‑creation)
                    return go;
                }
            }
    
            // 2) not found, so create, assign id, add, wire up:
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            newObject.setParentGame(this);
            gameObjects.add(newObject);
            return newObject;
        });
    }
    

    /**
     * The main loop that processes all objects at a fixed framerate (targetFps):
     * 1) Drains inbound messages for each object
     * 2) Processes commands for each object
     * 3) Performs local updates with deltaTime
     * 4) Checks and resolves collisions among collidable objects
     * 5) Increments tickCount
     * 6) Sleeps the thread to maintain the target framerate
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
                        a.resolveCollision(b);
                        // Example: zero out y velocity if Player2 collides with Platform
                        if (a instanceof Player2 && b instanceof Platform) {
                            ((Player2) a).getVel().y = 0;
                        } else if (b instanceof Player2 && a instanceof Platform) {
                            ((Player2) b).getVel().y = 0;
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

    public long getTickCount() {
        return tickCount;
    }

    /**
     * Draws all game objects onto the provided JavaFX GraphicsContext.
     */
    public void draw(GraphicsContext gc) {
        for (GameObject go : gameObjects) {
            go.draw(gc);
        }
    }

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
     * Used to prevent duplicate object creation (e.g., duplicate players).
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
