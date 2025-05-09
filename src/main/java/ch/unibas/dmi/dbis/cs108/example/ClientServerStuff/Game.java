package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.*;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.highscore.LevelTimer;
import javafx.scene.canvas.GraphicsContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    // timer for the level
    private LevelTimer levelTimer;

    // Whether the game has started.
    private boolean startedFlag = false;

    // A concurrent Set<String> backed by a ConcurrentHashMap
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    // Flag indicating if this game is authoritative or not.
    private boolean authoritative = false;

    // === New fields for controlling framerate and tracking ticks ===
    private volatile int targetFps = 60;         // desired frames per second
    private volatile long tickCount = 0;         // increments each loop

    private static final long  NANOS_PER_COMPOSITION = 1_000_000_000L / 120;   // 8 333 333 ns
    private              long  lastCompositionNanos  = 0L;                     // init = never sent

    private static final ConcurrentLinkedQueue<Message> INIT_QUEUE =
            new ConcurrentLinkedQueue<>();


    public Game(String gameId, String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;

       

        this.gameMessageHogger = new MessageHogger() {
        @Override
        protected void processBestEffortMessage(Message msg) {
            if (!"GAME".equalsIgnoreCase(msg.getOption())) return;
            if (!"COMPOSITION".equals(msg.getMessageType())) {
            routeMessageToGameObject(msg);
            return;
            }

            Object[] rawParams = msg.getParameters();
            if (rawParams == null) return;

            Arrays.stream(rawParams)
                .parallel()                   // <— split across the common fork/join pool
                .filter(o -> o instanceof String)
                .map(o -> (String) o)
                .forEach(safe -> {
                    String withCommas = safe.replace("%", ",").replace("~", "|");
                    try {
                    Message snap = MessageCodec.decode(withCommas);
                    routeMessageToGameObject(snap);
                    } catch (Exception ex) {
                    System.err.println("Failed to decode snapshot: " + withCommas);
                    }
                });
        }
        };

        

        // Start the main loop for processing all game objects at a fixed framerate.
        startPlayersCommandProcessingLoop();
        startCompositionLoop();
        initializeDefaultObjects();
        initializeLevel(800,600);
    }

    public void setAuthoritative(boolean authoritative) {
        this.authoritative = authoritative;
    }

    public void enqueueInit(Message m) {
        INIT_QUEUE.offer(m);
    }

    /** Called once by the authoritative Game when the match really starts. */
    public  void flushInitQueue() {
        Message m;
        while ((m = INIT_QUEUE.poll()) != null)
        {
            Server.getInstance().broadcastMessageToAll(m);
        }
                           // your existing transport
    }

    // Game.java  (inside class Game)
    private void initializeDefaultObjects() {
        if(isAuthoritative())
            {
            float screenW = 800f;      // Stage width
            float screenH = 600f;     // Stage height

            /* ---------- 1. Floor platforms -------------------------------- */
            for (int i = 0; i < 4; i++) {
                float x      = (float) (screenW * 0.05 + i * screenW * 0.20);
                float y      = (float) (screenH * 0.75 - i * screenH * 0.05);
                float width  = (float) (screenW * 0.20);
                float height = 20f;

                addGameObjectAsync(
                    "Platform",
                    UUID.randomUUID().toString(),              // unique id
                    "Floor" + (i + 1),                         // name
                    x, y, width, height,                       // geom
                    gameId                                     // session / owner
                );                                      // ensure creation
            }

            /* ---------- 2. Key -------------------------------------------- */
            addGameObjectAsync(
                "Key", UUID.randomUUID().toString(),
                "Key1",
                (float)(screenW * 0.15), (float)(screenH * 0.15),
                40f, 40f, 1f,                                 // 1f = (e.g.) weight
                gameId
            );

            /* ---------- 3. Players ---------------------------------------- */
            addGameObjectAsync(
                "Player2", UUID.randomUUID().toString(),
                "Alfred",
                (float)(screenW * 0.20), (float)(screenH * 0.40),
                40f, 40f,
                gameId
            );

            addGameObjectAsync(
                "Player2", UUID.randomUUID().toString(),
                "Gerald",
                (float)(screenW * 0.25), (float)(screenH * 0.40),
                40f, 40f,
                gameId
            );

            /* ---------- 4. Final platform --------------------------------- */
            addGameObjectAsync(
                "Platform", UUID.randomUUID().toString(),
                "Floor5",
                (float)(screenW * 0.85), (float)(screenH * 0.65),
                (float)(screenW * 0.10), 20f,
                gameId
            );

            /* ---------- 5. Door ------------------------------------------- */
            addGameObjectAsync(
                "Door", UUID.randomUUID().toString(),
                "Door1",
                (float)(screenW * 0.12), (float)(screenH * 0.50),
                50f, 120f,
                gameId
            );

            System.out.println("Level initialized (factory, no network).");
        }
    }

    //  package ch.unibas.dmi.dbis.cs108.example;

    //  Game.java   ––– place the method anywhere after the constructor
    /**
     * Queues CREATEGO messages that describe the whole level layout.
     * The authoritative side calls this *before* flushInitQueue().
     *
     * @param screenW Stage width  in pixels
     * @param screenH Stage height in pixels
     */
    public void initializeLevel(double screenW, double screenH) {

        /* ---------- 1. Floor platforms -------------------------------- */
        for (int i = 0; i < 4; i++) {
            float x      = (float) (screenW * 0.05 + i * screenW * 0.20);
            float y      = (float) (screenH * 0.75 - i * screenH * 0.05);
            float width  = (float) (screenW * 0.20);
            float height = 20f;

            Object[] p = { gameId, "Platform", "Floor" + (i + 1),
                        x, y, width, height, gameId };
            enqueueInit(new Message("CREATEGO", p, "RESPONSE"));
        }

        /* ---------- 2. Key -------------------------------------------- */
        enqueueInit(new Message("CREATEGO",
            new Object[]{ gameId, "Key", "Key1",
                        (float)(screenW * 0.15), (float)(screenH * 0.15),
                        40f, 40f, 1f, gameId },
            "RESPONSE"));

        /* ---------- 3. Players ---------------------------------------- */
        enqueueInit(new Message("CREATEGO",
            new Object[]{ gameId, "Player2", "Alfred",
                        (float)(screenW * 0.20), (float)(screenH * 0.40),
                        40f, 40f, gameId },
            "RESPONSE"));

        enqueueInit(new Message("CREATEGO",
            new Object[]{ gameId, "Player2", "Gerald",
                        (float)(screenW * 0.25), (float)(screenH * 0.40),
                        40f, 40f, gameId },
            "RESPONSE"));

        /* ---------- 4. Final platform --------------------------------- */
        enqueueInit(new Message("CREATEGO",
            new Object[]{ gameId, "Platform", "Floor5",
                        (float)(screenW * 0.85), (float)(screenH * 0.65),
                        (float)(screenW * 0.10), 20f, gameId },
            "RESPONSE"));

        /* ---------- 5. Door ------------------------------------------- */
        enqueueInit(new Message("CREATEGO",
            new Object[]{ gameId, "Door", "Door1",
                        (float)(screenW * 0.12), (float)(screenH * 0.50),
                        50f, 120f, gameId },
            "RESPONSE"));

        System.out.println("Level initialized for '" + gameName +
                        "' – queued " + INIT_QUEUE.size() + " CREATEGO messages.");
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
        if (concealed == null || concealed.length == 0) return;
    
        String targetObjectUuid = concealed[0];
        AsyncManager.run(() ->
          gameObjects.stream()
            .filter(go -> go.getId().equals(targetObjectUuid))
            .findFirst()
            .ifPresent(go -> {
              go.addIncomingMessage(msg);
              System.out.println("Routed message to GameObject with UUID: " + targetObjectUuid);
            })
        );
    }
    

    // starting level and start timer
    public void startLevel() {
        if (!startedFlag) {
            levelTimer.start();  // Starts the level timer
            startedFlag = true;   // game marked as started
            System.out.println("Level started. Timer started.");
        }
    }

    // stop game and stop timer
    public void stopLevel() {
        if (startedFlag) {
            levelTimer.stop();  // Stop the timer
            startedFlag = false;  // game marked as stopped
            System.out.println("Level stopped. Timer stopped.");
        }
    }

    // method to get the time
    public long getElapsedTime() {
        return levelTimer.getElapsedTimeInSeconds();
    }

    // Getter und Setter für startedFlag
    public boolean getStartedFlag() {
        return startedFlag;
    }

    public void setStartedFlag(boolean state) {
        this.startedFlag = state;
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

    void composeAndSendUpdate() {
    
        /* ---- 2) Original logic --------------------------------------- */
        if (!authoritative) return;
    
        List<Object> encodedSnaps = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (obj instanceof Player2) {
                Player2 p       = (Player2) obj;
                Message snap    = p.createSnapshot();
                String encoded  = MessageCodec.encode(snap)
                                        .replace(",", "%")
                                        .replace("|", "~");
                encodedSnaps.add(encoded);
            }
        }
    
        Message comp = new Message("COMPOSITION",
                                   encodedSnaps.toArray(new Object[0]),
                                   "GAME");
        Server.getInstance().sendMessageBestEffort(comp);
    }

    public void startCompositionLoop() {
        final long[] lastTime = { System.nanoTime() };
    
        AsyncManager.runLoop(() -> {
            long start = System.nanoTime();
            // (optionally) compute dt if you ever need it
            // float deltaSec = (start - lastTime[0]) / 1_000_000_000f;
            lastTime[0] = start;
    
            // send one snapshot
            composeAndSendUpdate();
    
            // throttle to targetCompositionFps
            long frameNanos = 1_000_000_000L / 120;
            long elapsed  = System.nanoTime() - start;
            long sleepN   = frameNanos - elapsed;
            if (sleepN > 0) {
                try {
                    Thread.sleep(sleepN / 1_000_000, (int)(sleepN % 1_000_000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
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
                go.applyLatestSnapshot();
            }
            for (GameObject go : gameObjects) {
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
                        
                        if (a instanceof Key && b instanceof Platform) {
                            ((Key) a).setVelocityY(0.0f);
                        } else if (b instanceof Key && a instanceof Platform) {
                            ((Key) b).setVelocityY(0.0f);
                        }
                        
                    }
                }
            }
            //composeAndSendUpdate();

            // 4) Increment the global tickCount
            //tickCount++;
            if(startedFlag)
            {
                flushInitQueue();
            }

          
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
