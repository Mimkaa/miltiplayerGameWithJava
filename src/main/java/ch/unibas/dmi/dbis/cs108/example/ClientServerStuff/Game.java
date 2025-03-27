package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Getter;

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
     * Creates a {@code Game} instance with the specified UUID and user-friendly name.
     *
     * @param gameId   The UUID of this game session.
     * @param gameName The name/identifier of this game session.
     */
    public Game(String gameId, String gameName) {
        this.gameId = gameId;
        this.gameName = gameName;
    }

    /*
     * Optional: If you want to keep a constructor that only takes a name,
     * you can overload it like this, generating a random UUID automatically:
     *
     * public Game(String gameName) {
     *     this(UUID.randomUUID().toString(), gameName);
     * }
     */

    /**
     * Queues an asynchronous task to process an incoming {@link Message}.
     * The message will be routed to the appropriate {@link GameObject} based
     * on the concealed target name.
     *
     * @param msg The {@code Message} to be routed.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Routes the given message to the correct {@link GameObject} based on the first
     * concealed parameter in the message (assumed to be the target object's name).
     *
     * @param msg The {@code Message} to be routed.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetName = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getName().equals(targetName)) {
                    go.addIncomingMessageAsync(msg);
                    go.collectMessageUpdatesOnce();
                    System.out.println("Routed message to " + go.getName());
                    break;
                }
            }
        }
    }

    /**
     * Updates (moves or otherwise changes) the specified active {@link GameObject}
     * by name, using the provided queue for outgoing messages. This is done
     * asynchronously via the {@link AsyncManager}.
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
     * {@link GameObject}s in this game. The loop runs indefinitely, calling
     * {@link GameObject#processCommandsNonBlocking()} for each object repeatedly.
     */
    public void startPlayersCommandProcessingLoop() {
        AsyncManager.runLoop(() -> {
            for (GameObject go : gameObjects) {
                go.processCommandsNonBlocking();
            }
        });
    }

    /**
     * Creates a new {@link GameObject} of the specified type and parameters, assigns it the
     * given UUID, adds it to this game, and returns a {@link Future} representing
     * the newly created object. The creation is performed asynchronously via {@link AsyncManager}.
     *
     * @param type   The type identifier (e.g., "Player", "Square", etc.).
     * @param uuid   The UUID to assign to the newly created object.
     * @param params Additional constructor parameters for creating the object.
     * @return A {@code Future<GameObject>} that can be used to retrieve the created object.
     */
    public Future<GameObject> addGameObjectAsync(String type, String uuid, Object... params) {
        return AsyncManager.run(() -> {
            // Create a new game object using the factory.
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            gameObjects.add(newObject);
            return newObject;
        });
    }

    /**
     * Returns an unmodifiable list of all {@link GameObject}s managed by this game.
     *
     * @return A {@code List<GameObject>} of all objects currently in the game.
     */
    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    /**
     * Shuts down asynchronous execution for this game. This method should be called
     * once the game is no longer needed, allowing cleanup of any running tasks.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }
}
