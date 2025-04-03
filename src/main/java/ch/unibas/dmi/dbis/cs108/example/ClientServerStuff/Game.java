package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javafx.scene.canvas.GraphicsContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * The {@code Game} class manages a collection of {@link GameObject}s within a given
 * game or session. It provides methods to create, update, and route messages to
 * these objects.
 */
@Getter
public class Game {

    private final String gameId;
    private final String gameName;
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
    private final MessageHogger gameMessageHogger;

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

        // Start a single main loop for all objects:
        startPlayersCommandProcessingLoop();
    }

    /**
     * Queues an asynchronous task to process an incoming message,
     * then routes it to the correct GameObject.
     */
    public void addIncomingMessage(Message msg) {
        // You can still do this asynchronously if you like, or just call routeMessageToGameObject(msg).
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Routes the message to the correct GameObject by matching the first concealed param to the object's UUID.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetObjectUuid = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getId().equals(targetObjectUuid)) {
                    // Directly add to object queue (no new tasks needed)
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
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            gameObjects.add(newObject);
            return newObject;
        });
    }

    /**
     * The main loop that processes all objects:
     * - Drains inbound messages for each object
     * - Processes commands for each object
     * - Performs local updates
     */
    public void startPlayersCommandProcessingLoop() {
        AsyncManager.runLoop(() -> {
            for (GameObject go : gameObjects) {
                // 1) Drain all inbound messages, calling myUpdateGlobal(msg)
                go.processIncomingMessages();

                // 2) Process queued commands
                go.processCommands();

                // 3) Perform local update
                go.myUpdateLocal();
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

    /**
     * Gracefully shut down if desired.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }
}

