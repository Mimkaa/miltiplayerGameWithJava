package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.awt.event.KeyListener;

public class Game {
    // A thread-safe list of game objects (players, enemies, etc.).
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();

    // The game (or session) name.
    private final String gameName;

    // Reference to our Swing panel that draws game objects.
    private GamePanel gamePanel;

    // Reference to the main JFrame (stored for key binding operations).
    private JFrame frame;

    /**
     * Creates a Game instance with the specified gameName.
     * This constructor creates several default game objects.
     */
    public Game(String gameName) {
        this.gameName = gameName;
        // Add default game objects.
        gameObjects.add(new Player("Alice", 100.0f, 200.0f, 10.0f, gameName));
        gameObjects.add(new Player("Bob",   150.0f, 250.0f, 12.0f, gameName));
        gameObjects.add(new Player("Carol", 200.0f, 300.0f, 15.0f, gameName));
        // Additional game objects can be added here.
    }

     /**
     * Updates the ping indicator on the game panel.
     * This method ensures the update is performed on the EDT.
     * @param pingValue the new ping value to be displayed.
     */
    public void updatePingIndicator(int pingValue) {
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> gamePanel.setPingIndicator(pingValue));
        }
    }

    /**
     * Queues an asynchronous task to route an incoming message to the correct game object.
     */
    public void addIncomingMessage(Message msg) {
        AsyncManager.run(() -> routeMessageToGameObject(msg));
    }

    /**
     * Finds the correct game object (based on concealed[0] = object name) and enqueues the message.
     */
    private void routeMessageToGameObject(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            String targetName = concealed[0];
            for (GameObject go : gameObjects) {
                if (go.getName().equals(targetName)) {
                    go.addIncomingMessageAsync(msg);
                    // The call here is sufficient to trigger message collection.
                    go.collectMessageUpdatesOnce();
                    System.out.println("Routed message to " + go.getName());
                    break;
                }
            }
        }
    }

    /**
     * Updates (moves) the active (local) game object by name.
     * The game object's updateAsync() method is called with the provided outgoingQueue.
     */
    public void updateActiveObject(String objectName, ConcurrentLinkedQueue<Message> outgoingQueue) {
        AsyncManager.run(() -> {
            for (GameObject go : gameObjects) {
                if (go.getName().equals(objectName)) {
                    go.updateAsync(outgoingQueue);
                    break;
                }
            }
        });
    }

    /**
     * Initializes and displays the Swing UI that shows our game objects,
     * and attaches key controls to the local player.
     */
    public void initUI(String localPlayerName) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Simple Game - " + gameName);
            // Assumes GamePanel can accept a GameObject[]; adjust accordingly.
            gamePanel = new GamePanel(gameObjects.toArray(new GameObject[0]));
            frame.add(gamePanel);
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
            
            // A Swing Timer to repaint the panel at ~60 FPS and process non-blocking commands.
            Timer timer = new Timer(16, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (GameObject go : gameObjects) {
                        go.processCommandsNonBlocking();
                    }
                    gamePanel.repaint();
                }
            });
            timer.start();
        });
    }

    /**
     * Synchronously creates a new GameObject of the given type using the provided parameters,
     * generates a new UUID, sets it in the object, adds it to the game,
     * and returns the created GameObject as a Future.
     *
     * @param type   The type identifier (e.g., "Player", "Square", etc.).
     * @param uuid   The UUID to set in the new game object.
     * @param params A variable number of parameters to be passed to the object's constructor.
     * @return A Future for the newly created game object.
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
            
            // Return the reference to the created game object.
            return newObject;
        });
    }
    
    /**
     * Rebinds the key listener for the game object with the specified UUID using the stored JFrame.
     */
    public void rebindKeyListenerForObject(String uuid) {
        if (frame == null) {
            System.out.println("Frame not initialized. Cannot rebind key listeners.");
            return;
        }
        // Find the game object with the specified UUID.
        for (GameObject go : gameObjects) {
            if (go.getId().equals(uuid)) {
                // Remove all existing key listeners from the frame.
                for (KeyListener kl : frame.getKeyListeners()) {
                    frame.removeKeyListener(kl);
                }
                // Add the key listener from the found game object.
                frame.addKeyListener(go.getKeyListener());
                System.out.println("Rebound key listener for object: " + go.getName() + " (UUID: " + uuid + ")");
                // Update the panel to reflect any changes.
                updateGamePanel();
                return;
            }
        }
        System.out.println("No game object found with UUID: " + uuid);
    }
    
    /**
     * Updates the game panel with the current list of game objects.
     */
    public void updateGamePanel() {
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> 
                gamePanel.updateGameObjects(gameObjects.toArray(new GameObject[0]))
            );
        }
    }

    /**
     * Removes all current KeyListeners from the JFrame and adds the KeyListener
     * for the GameObject associated with the given UUID (if found).
     *
     * @param localPlayerId The UUID of the GameObject whose KeyListener should be added.
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
     * Returns the current list of game objects.
     */
    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    /**
     * Returns the game name (session ID).
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * Shuts down the asynchronous execution.
     */
    public void shutdown() {
        AsyncManager.shutdown();
        System.out.println("Game [" + gameName + "] async manager stopped.");
    }
}
