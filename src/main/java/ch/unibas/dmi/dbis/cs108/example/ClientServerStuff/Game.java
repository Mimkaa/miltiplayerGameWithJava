package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * The {@code Game} class manages a collection of {@link GameObject}s within a given
 * game or session. It provides methods to create, update, and route messages to
 * these objects.
 */
@Getter
public class Game {

    /**
     * The UUID of this game session, uniquely identifying it on the server.
     */
    private final String gameId;

    /**
     * The user-friendly name of this game or session.
     */
    private final String gameName;

    /**
     * A thread-safe list of {@link GameObject}s participating in this game.
     */
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();

    /**
     * A dedicated MessageHogger for processing messages whose option is "GAME".
     */
    private final MessageHogger gameMessageHogger;

    /**
     * Creates a {@code Game} instance with the specified UUID and user-friendly name.
     * Also starts the asynchronous command processing loop for the game objects.
     *
     * @param gameId   The UUID of this game session.
     * @param gameName The name/identifier of this game session.
     */
    public Game(String gameId, String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;
        // Initialize the dedicated message hogger for GAME messages.
        this.gameMessageHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message msg) {
                // Process only messages with option "GAME"
                if ("GAME".equalsIgnoreCase(msg.getOption())) {
                    String[] concealed = msg.getConcealedParameters();
                    if (concealed != null && concealed.length >= 2) {
                        // First concealed parameter: target game object's UUID.
                        // Second concealed parameter: game session's UUID.
                        String targetGameObjectUuid = concealed[0];
                        String msgGameUuid = concealed[1];
                        if (msgGameUuid.equals(Game.this.gameId)) {
                            // Only process if the game UUID matches this game.
                            routeMessageToGameObject(msg);
                        } else {
                            System.out.println("Ignoring GAME message: game UUID mismatch. Expected: " 
                                    + Game.this.gameId + ", got: " + msgGameUuid);
                        }
                    } else {
                        System.out.println("Ignoring GAME message: insufficient concealed parameters.");
                    }
                } else {
                    System.out.println("Ignoring non-GAME message in gameMessageHogger: " + msg.getMessageType());
                }
            }
        };

        // Start the asynchronous command processing loop for all game objects.
        startPlayersCommandProcessingLoop();
    }

    /**
     * Queues an asynchronous task to process an incoming message.
     *
     * @param msg The {@code Message} to be routed.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Adds an incoming message intended for this game.
     * If the message option is "GAME", it is forwarded to the gameMessageHogger;
     * otherwise, it is processed normally.
     *
     * @param msg The incoming message.
     */
    public void addIncomingGameMessage(Message msg) {
        if ("GAME".equalsIgnoreCase(msg.getOption())) {
            gameMessageHogger.addMessage(msg);
        } else {
            addIncomingMessage(msg);
        }
    }

    /**
     * Routes the given message to the appropriate {@link GameObject} based on the first
     * concealed parameter (assumed to be the target object's UUID).
     *
     * @param msg The {@code Message} to be routed.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetObjectUuid = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getId().equals(targetObjectUuid)) {
                    go.addIncomingMessageAsync(msg);
                    go.collectMessageUpdatesOnce();
                    System.out.println("Routed message to GameObject with UUID: " + targetObjectUuid);
                    break;
                }
            }
        }
    }

    /**
     * Updates the specified active {@link GameObject} by name using the provided outgoing message queue.
     *
     * @param objectName    The name of the game object to update.
     * @param outgoingQueue The queue to be used for sending messages from this object.
     */
    public void updateActiveObject(String objectName, ConcurrentLinkedQueue<Message> outgoingQueue) {
        AsyncManager.run(() -> {
            for (GameObject go : gameObjects) {
                if (go.getName().equals(objectName)) {
                    go.updateAsync();
                    break;
                }
            }
        });
    }

    /**
     * Starts an asynchronous loop that continuously processes commands for all
     * {@link GameObject}s in this game.
     */
    public void startPlayersCommandProcessingLoop() {
        AsyncManager.runLoop(() -> {
            for (GameObject go : gameObjects) {
                go.processCommandsNonBlocking();
            }
        });
    }

    /**
     * Creates a new {@link GameObject} of the specified type and parameters, assigns it the given UUID,
     * adds it to this game, and returns a {@link Future} representing the newly created object.
     *
     * @param type   The type identifier (e.g., "Player", "Square", etc.).
     * @param uuid   The UUID to assign to the newly created object.
     * @param params Additional constructor parameters.
     * @return A {@code Future<GameObject>} that can be used to retrieve the created object.
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
     * Returns the list of all {@link GameObject}s in this game.
     *
     * @return A {@code List<GameObject>} of all game objects.
     */
    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    /**
     * Shuts down asynchronous execution for this game, allowing cleanup of tasks.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }
}
