package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.physics.Collidable;
import javafx.scene.canvas.GraphicsContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import lombok.Getter;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import javafx.geometry.Rectangle2D;

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

        // Start the main loop that processes all objects.
        startPlayersCommandProcessingLoop();
    }

    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetObjectUuid = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getId().equals(targetObjectUuid)) {
                    // Directly add the message to the object’s queue.
                    go.addIncomingMessage(msg);
                    System.out.println("Routed message to GameObject with UUID: " + targetObjectUuid);
                    return;
                }
            }
        }
    }

    public Future<GameObject> addGameObjectAsync(String type, String uuid, Object... params) {
        return AsyncManager.run(() -> {
            GameObject newObject = GameObjectFactory.create(type, params);
            newObject.setId(uuid);
            gameObjects.add(newObject);
            return newObject;
        });
    }

    public void startPlayersCommandProcessingLoop() {
        AsyncManager.runLoop(() -> {
            for (GameObject go : gameObjects) {
                // 1) Process incoming messages.
                go.processIncomingMessages();
                // 2) Process queued commands.
                go.processCommands();
                // 3) Perform local update.
                go.myUpdateLocal();
            }
            // Check for collisions after updating all objects.
            checkCollisions();
        });
    }

    public void draw(GraphicsContext gc) {
        for (GameObject go : gameObjects) {
            go.draw(gc);
        }
    }

    /**
     * Loops through all game objects that implement Collidable and checks for intersections.
     * When a collision is detected, it prints a message (or you could trigger a game-over event).
     */
    public void checkCollisions() {
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go1 = gameObjects.get(i);
            if (!(go1 instanceof Collidable)) continue;
            for (int j = i + 1; j < gameObjects.size(); j++) {
                GameObject go2 = gameObjects.get(j);
                if (!(go2 instanceof Collidable)) continue;
                if (((Collidable) go1).intersects((Collidable) go2)) {
                    System.out.println("Collision detected between " + go1.getName() + " and " + go2.getName());
                    // Here you can add additional collision response logic (e.g., game over, bounce, etc.)
                }
            }
        }
    }

    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] (ID: " + gameId + ") async manager stopped.");
    }
}
