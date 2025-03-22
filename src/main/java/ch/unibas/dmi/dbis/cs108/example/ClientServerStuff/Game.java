package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.awt.event.KeyListener;

import chat.ChatUIHelper;
import lombok.Getter;

@Getter
/**
 * The {@code Game} class manages a collection of {@link GameObject}s within a given
 * game or session. It provides methods to create, update, and route messages to
 * these objects, as well as display and control them in a Swing-based GUI.
 */
public class Game {

    /**
     * A thread-safe list of {@link GameObject}s participating in this game.
     * These can be players, enemies, or any other interactive entities.
     */
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();

    /**
     * The name of this game or session, used to identify it in messages and logs.
     */
    private final String gameName;


    /**
     * A reference to the {@link GamePanel} that renders the game objects.
     * This is created and set when  is called.
     */
    private GamePanel gamePanel;

    /**
     * The main {@link JFrame} holding the game UI. Stored for attaching key listeners
     * and other UI-related operations.
     */
    private JFrame frame;

    /**
     * Creates a {@code Game} instance with the specified game name. Additional default
     * objects can be created and added to the game objects list if needed.
     *
     * @param gameName The name/identifier of this game session.
     */
    public Game(String gameName) {
        this.gameName = gameName;
    }

    /**
     * Updates the ping indicator on the game panel. This operation is always executed
     * on the Swing Event Dispatch Thread to ensure thread safety in UI updates.
     *
     * @param pingValue The new ping value to display.
     */
    public void updatePingIndicator(int pingValue) {
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> gamePanel.setPingIndicator(pingValue));
        }
    }

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
     * Initializes and displays the Swing user interface for this game. A {@link JFrame} is
     * created with a {@link GamePanel} that contains the current list of game objects.
     * Key controls for the specified local player are also attached here.
     *
     * @param localPlayerName The name of the local player's {@link GameObject}.
     */
    public void initUI(String localPlayerName, Client client) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Simple Game - " + gameName);
            frame.setLayout(new BorderLayout());
            gamePanel = new GamePanel(gameObjects.toArray(new GameObject[0]));
            frame.add(gamePanel, BorderLayout.CENTER);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Attach key listener for the local player.
            for (GameObject go : gameObjects) {
                if (go.getName().equals(localPlayerName)) {
                    frame.addKeyListener(go.getKeyListener());
                    System.out.println("Connected controls for local player: " + go.getName());
                    break;
                }
            }

            frame.setVisible(true);
            ChatUIHelper.installChatUI(frame, client.getClientChatManager().getChatPanel());
            

            // Start a Timer to repaint the game panel at ~60 FPS.
            Timer timer = new Timer(16, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    gamePanel.repaint();
                }
            });
            timer.start();
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

            // Set the UUID in the game object.
            newObject.setId(uuid);
            
            // Add the new game object to the list.
            gameObjects.add(newObject);
            
            System.out.println("Added new " + type + ": " + newObject.getName() + " with UUID: " + uuid);
            
            // Update the game panel (if available) on the EDT.
            if (gamePanel != null) {
                SwingUtilities.invokeLater(() ->
                        gamePanel.updateGameObjects(gameObjects.toArray(new GameObject[0]))
                );
            }
            return newObject;
        });
    }

    /**
     * Updates the {@link GamePanel} with the current list of {@link GameObject}s.
     * If the panel is not {@code null}, this operation is run on the Swing Event Dispatch Thread.
     */
    public void updateGamePanel() {
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() ->
                    gamePanel.updateGameObjects(gameObjects.toArray(new GameObject[0]))
            );
        }
    }

    /**
     * Removes all current {@link KeyListener}s from the main {@link JFrame} and
     * adds the {@link KeyListener} for the {@link GameObject} with the specified UUID,
     * effectively rebinding controls to a different local player object.
     *
     * @param localPlayerId The UUID of the {@code GameObject} whose {@link KeyListener} should be attached.
     */
    public void rebindKeyListeners(String localPlayerId) {
        SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                System.err.println("No UI frame exists yet. Cannot rebind key listeners.");
                return;
            }

            // Remove all currently registered KeyListeners
            for (KeyListener kl : frame.getKeyListeners()) {
                frame.removeKeyListener(kl);
            }

            // Add the KeyListener for the matching GameObject (the local player)
            for (GameObject go : gameObjects) {
                System.out.println(go.getName());
                System.out.println(go.getClass().getName());
                System.out.println(this.frame);
                if (go.getName().equals(localPlayerId)) {
                    frame.addKeyListener(go.getKeyListener());
                    System.out.println("Connected controls for local player: " + go.getName());
                    break;
                }
            }
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
     * Returns the name of this game or session.
     *
     * @return A {@code String} representing the game name.
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * Shuts down asynchronous execution for this game. This method should be called
     * once the game is no longer needed, allowing cleanup of any running tasks.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] async manager stopped.");
    }
}
