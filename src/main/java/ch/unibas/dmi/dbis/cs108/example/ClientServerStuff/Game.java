package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Game {
    // A thread-safe list of game objects (players, enemies, etc.).
    private final CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();

    // The game (or session) name.
    private final String gameName;

    // A fixed-size thread pool for asynchronous tasks (e.g., routing messages).
    private final ExecutorService executor;

    // Reference to our Swing panel that draws game objects.
    private GamePanel gamePanel;

    /**
     * Creates a Game instance with the specified gameName.
     * This constructor creates three default players.
     */
    public Game(String gameName) {
        this.gameName = gameName;
        // Add Player instances (which extend GameObject)
        gameObjects.add(new Player("Alice", 100.0f, 200.0f, 10.0f, gameName));
        gameObjects.add(new Player("Bob",   150.0f, 250.0f, 12.0f, gameName));
        gameObjects.add(new Player("Carol", 200.0f, 300.0f, 15.0f, gameName));
        gameObjects.add(new Square("Joe", 300, 300, 10, gameName));
        gameObjects.add(new Ricardo("Ricardo", gameName, 400, 300, "src/main/java/ch/unibas/dmi/dbis/cs108/example/ClientServerStuff/resources/ricardo.png"));
        this.executor = Executors.newFixedThreadPool(20);

        // Start collecting incoming messages for each game object.
        for (GameObject go : gameObjects) {
            go.collectMessageUpdatesOnce();
        }
    }

    /**
     * Queues an asynchronous task to route an incoming message to the correct game object.
     */
    public void addIncomingMessage(Message msg) {
        executor.submit(() -> routeMessageToGameObject(msg));
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
                    // Optionally ensure the collector is active.
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
        executor.submit(() -> {
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
            JFrame frame = new JFrame("Simple Game - " + gameName);
            // Assumes GamePanel can accept a GameObject[]; adjust accordingly.
            gamePanel = new GamePanel(gameObjects.toArray(new GameObject[0]));
            frame.add(gamePanel);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Attach key listener for the local player.
            for (GameObject go : gameObjects) {
                if (go.getName().equals(localPlayerName)) {
                    // Assuming the local game object is a Player instance.
                    
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
     * Adds a new player with the specified name to this game session.
     */
    public void addPlayer(String playerName) {
        // Create a new Player (which is a GameObject)
        GameObject newPlayer = new Player(playerName, 0.0f, 0.0f, 5.0f, gameName);
        gameObjects.add(newPlayer);
        newPlayer.collectMessageUpdatesOnce();
        System.out.println("Added new player: " + newPlayer.getName());
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
     * Shuts down the game's executor.
     */
    public void shutdown() {
        executor.shutdownNow();
        System.out.println("Game [" + gameName + "] executor stopped.");
    }
}
