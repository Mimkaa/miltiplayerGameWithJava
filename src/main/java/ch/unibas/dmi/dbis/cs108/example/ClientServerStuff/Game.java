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
 * The {@code Game} class manages a collection of {@link GameObject}s within a game session.
 * It provides methods to create, update and route messages to these objects.
 * <p>
 * Zusätzlich wird in regelmäßigen Intervallen ein Snapshot des aktuellen Zustands
 * (z. B. Objekt-ID, x- und y-Position) erstellt und über die serverseitige Versandmethode
 * an alle Clients versendet, um die Multiplayer-Synchronisation zu verbessern.
 */
@Getter
public class Game {

    private final String gameId;
    private final String gameName;
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
    private final MessageHogger gameMessageHogger;

    // Set für Usernamen (z. B. zur Anzeige in der UI)
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    // Snapshot-Variablen: Alle 100 ms soll ein Snapshot versendet werden.
    private long lastSnapshotNano = System.nanoTime();
    private static final float SNAPSHOT_INTERVAL = 0.1f; // 0.1 Sekunden = 100ms

    /**
     * Constructs a new Game instance with the given gameId and gameName.
     *
     * @param gameId   the unique game session ID
     * @param gameName the display name of the game session
     */
    public Game(String gameId, String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;

        // Initialisiere den dedizierten MessageHogger für GAME-Nachrichten.
        this.gameMessageHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message msg) {
                // Nur verarbeiten, wenn msg.getOption() "GAME" ist.
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

        // Starte den Haupt-Loop für die Verarbeitung aller GameObjects.
        startPlayersCommandProcessingLoop();
    }

    /**
     * Queues an asynchronous task to process an incoming message,
     * then routes it to the correct GameObject.
     *
     * @param msg the incoming message
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Routes the given message to the correct GameObject by matching the first concealed
     * parameter (assumed to be the GameObject's UUID) to an existing GameObject.
     *
     * @param msg the message to route
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
     * Creates a new GameObject of the specified type and parameters, assigns it the given UUID,
     * and adds it to this game.
     *
     * @param type   the type of GameObject (e.g., "Player", "FallingObject")
     * @param uuid   the unique identifier to assign to the new object
     * @param params constructor parameters for the GameObject
     * @return a Future representing the pending creation of the GameObject
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
     * The main loop that processes all objects:
     * <ul>
     *   <li>Processes inbound messages, commands and local updates for each GameObject.</li>
     *   <li>Checks and resolves collisions among collidable GameObjects.</li>
     *   <li>Sends a snapshot of the current game state at regular intervals.</li>
     * </ul>
     */
    public void startPlayersCommandProcessingLoop() {
        AsyncManager.runLoop(() -> {
            // 1) Für jedes GameObject: Verarbeite Nachrichten, Befehle und führe lokale Updates aus.
            for (GameObject go : gameObjects) {
                go.processIncomingMessages();
                go.processCommands();
                go.myUpdateLocal();
            }

            // 2) Kollisionsprüfung: Vergleiche alle kollidierbaren Objekte paarweise.
            for (int i = 0; i < gameObjects.size(); i++) {
                GameObject a = gameObjects.get(i);
                if (!a.isCollidable()) continue;
                // Setze Kollisionsflag (z. B. für visuelles Feedback bei Playern) zurück.
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
                        a.resolveCollision(b);
                        if (a instanceof Player) {
                            ((Player) a).setCollisionDetected(true);
                        }
                        if (b instanceof Player) {
                            ((Player) b).setCollisionDetected(true);
                        }
                        System.out.println("Collision resolved between " + a.getName() + " and " + b.getName());
                    }
                }
            }

            // 3) Sende in regelmäßigen Intervallen einen Snapshot des aktuellen Spielzustands.
            long now = System.nanoTime();
            float deltaSnapshot = (now - lastSnapshotNano) / 1_000_000_000f;
            if (deltaSnapshot >= SNAPSHOT_INTERVAL) {
                broadcastSnapshot();
                lastSnapshotNano = now;
            }
        });
    }

    /**
     * Creates a snapshot of the current game state (e.g., each object's ID, x- and y-position)
     * and sends it to all connected clients using the server-side messaging.
     * <p>
     * This method is part of the hybrid synchronization approach: while clients simulate locally,
     * they receive periodic snapshots from the server to correct any drift.
     */
    public void broadcastSnapshot() {
        Object[] snapshot = new Object[gameObjects.size()];
        int i = 0;
        for (GameObject go : gameObjects) {
            // Prüfe, ob go.getId() und die Positionen gültige Werte liefern.
            snapshot[i++] = new Object[]{ go.getId(), go.getX(), go.getY() };
        }
        Message snapshotMsg = new Message("SNAPSHOT", snapshot, "GAME");
        Server.getInstance().sendServerMessage(snapshotMsg);
        System.out.println("Broadcast snapshot with " + snapshot.length + " objects.");
    }


    /**
     * Draws all GameObjects onto the provided JavaFX GraphicsContext.
     *
     * @param gc the GraphicsContext to draw on
     */
    public void draw(GraphicsContext gc) {
        for (GameObject go : gameObjects) {
            go.draw(gc);
        }
    }

    /**
     * Gracefully shuts down the game (stops the AsyncManager, etc.).
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }
}
