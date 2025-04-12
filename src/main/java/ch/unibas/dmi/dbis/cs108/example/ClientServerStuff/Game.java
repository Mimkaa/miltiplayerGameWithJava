package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObjectFactory;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Player2;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Platform;
import javafx.scene.canvas.GraphicsContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@Getter
public class Game {

    private final String gameId;
    private final String gameName;
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
    private final MessageHogger gameMessageHogger;

    private boolean startedFlag = false;

    // Creates a concurrent Set<String> backed by a ConcurrentHashMap.
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    // New variable to store the target frames per second.
    private volatile int targetFps = 60; // Default to 60 FPS

    // Flag for selecting the update mode.
    private volatile boolean authoritative = false;

    // --- Tick counting and snapshot history ---
    // This tick counter is incremented on every authoritative update.
    private volatile long tickCount = 0;
    // Store snapshots keyed by tick. Each tick maps to a list of snapshots.
    private final ConcurrentHashMap<Long, List<Message>> snapshotHistory = new ConcurrentHashMap<>();

    // New flag to indicate if any message was received during the current tick.
    private volatile boolean messageReceivedThisTick = false;

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

        // Start the main game loop.
        startPlayersCommandProcessingLoop();
    }

    // Mode setters and getters.
    public void setAuthoritative(boolean authoritative) {
        this.authoritative = authoritative;
    }

    public boolean isAuthoritative() {
        return authoritative;
    }
    
    public void setStartedFlag(boolean state) {
        startedFlag = state;
    }

    public boolean getStartedFlag() {
        return startedFlag;
    }
    
    // Setter for targetFps.
    public void setTargetFps(int targetFps) {
        if (targetFps > 0) {
            this.targetFps = targetFps;
        }
    }

    // Getter for targetFps.
    public int getTargetFps() {
        return targetFps;
    }

    /**
     * Queues an asynchronous task to process an incoming message,
     * then routes it to the correct GameObject.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Routes the message to the correct GameObject by matching the first concealed parameter 
     * to the object's UUID.
     * Also sets the messageReceivedThisTick flag to true.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetGameObjectUuid = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getId().equals(targetGameObjectUuid)) {
                    // Directly add to object's message queue.
                    go.addIncomingMessage(msg);
                    System.out.println("Routed message to GameObject with UUID: " + targetGameObjectUuid);
                    // Mark that a message was received in this tick.
                    messageReceivedThisTick = true;
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
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            gameObjects.add(newObject);
            newObject.setParentGame(this);
            return newObject;
        });
    }

    /**
     * Resolves collisions among collidable game objects.
     */
    private void resolveCollisions() {
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject a = gameObjects.get(i);
            if (!a.isCollidable()) continue;
            for (int j = i + 1; j < gameObjects.size(); j++) {
                GameObject b = gameObjects.get(j);
                if (!b.isCollidable()) continue;
                if (a.intersects(b)) {
                    a.resolveCollision(b);
                    if (a instanceof Player2 && b instanceof Platform) {
                        ((Player2) a).getVel().y = 0;
                    } else if (b instanceof Player2 && a instanceof Platform) {
                        ((Player2) b).getVel().y = 0;
                    }
                }
            }
        }
    }

    /**
     * Performs non-authoritative update:
     * Processes each game object's messages, commands, applies local updates,
     * and then resolves collisions.
     */
    private void updateNonAuthoritative(float deltaTime) {
        for (GameObject go : gameObjects) {
            go.processIncomingMessages();
            go.processCommands();
            go.myUpdateLocal(deltaTime);
        }
        resolveCollisions();
    }

    /**
     * Performs authoritative update:
     * Processes each game object's messages, commands, applies authoritative updates,
     * and then resolves collisions.
     * Additionally, generates snapshots of all game objects and broadcasts them,
     * but only if at least one message was received during the tick.
     */
    private void updateAuthoritative(float deltaTime) {
        // Process each game object.
        for (GameObject go : gameObjects) {
            go.processIncomingMessages();
            go.processCommands();
            go.myUpdateLocal(deltaTime);
        }
        resolveCollisions();

        // Increment the tick counter.
        tickCount++;

        // Generate and broadcast snapshots only if a message was received this tick.
        if (messageReceivedThisTick) {
            List<Message> snapshots = new ArrayList<>();
            for (GameObject go : gameObjects) {
                Message snapshot = go.createSnapshot();
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
            }
            snapshotHistory.put(tickCount, snapshots);

            // Only broadcast snapshots if the outgoing queue is empty.
            if (Server.getInstance().getOutgoingQueue().isEmpty()) {
                for (Message snapshot : snapshots) {
                    Server.getInstance().sendMessageBestEffort(snapshot);
                }
            }
        }

        // Reset the flag for the next tick.
        messageReceivedThisTick = false;
    }

    /**
     * The main loop that processes all objects. It chooses between authoritative 
     * and non-authoritative update methods based on the 'authoritative' flag.
     * The loop runs at a fixed rate defined by {@code targetFps}.
     */
    public void startPlayersCommandProcessingLoop() {
        final long[] lastFrameTime = { System.nanoTime() };

        AsyncManager.runLoop(() -> {
            long startFrameTime = System.nanoTime();
            float deltaTime = (startFrameTime - lastFrameTime[0]) / 1_000_000_000f;
            lastFrameTime[0] = startFrameTime;

            if (authoritative) {
                updateAuthoritative(deltaTime);
            } else {
                updateNonAuthoritative(deltaTime);
            }

            final long targetFrameTimeNanos = 1_000_000_000L / targetFps;
            long frameProcessingTime = System.nanoTime() - startFrameTime;
            long sleepTimeNanos = targetFrameTimeNanos - frameProcessingTime;
            if (sleepTimeNanos > 0) {
                try {
                    Thread.sleep(sleepTimeNanos / 1_000_000, (int)(sleepTimeNanos % 1_000_000));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
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
}
