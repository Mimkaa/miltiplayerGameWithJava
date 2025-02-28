package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class Game {
    // Global queue for incoming messages.
    private static final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    
    // Reference to the distributor thread.
    private Thread distributorThread;

    private Thread updateThread;

    // Thread-safe list of players.
    private final List<Player> players = new CopyOnWriteArrayList<>();

    public Game() {

        // Manually create 3 players.
        players.add(new Player("Alice", 100.0f, 200.0f, 10.0f));
        players.add(new Player("Bob", 150.0f, 250.0f, 12.0f));
        players.add(new Player("Carol", 200.0f, 300.0f, 15.0f));


        // Create the distributor thread: it polls messages from the queue and routes them to the right player.
        distributorThread = new Thread(() -> {
            while (true) {
                Message msg = incomingQueue.poll();
                if (msg != null) {
                    // Extract sender's name from the concealed parameters.
                    String[] concealed = msg.getConcealedParameters();
                    if (concealed != null && concealed.length > 0) {
                        String senderName = concealed[0];
                        // Route the message to the matching player.
                        for (Player p : players) {
                            if (p.getName().equals(senderName)) {
                                p.addIncomingMessage(msg);
                                System.out.println("Consumer routed message to " + p.getName());
                                break;
                            }
                        }
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        
      
        distributorThread.start();

         // Create the update thread that updates players and processes their messages.
        updateThread = new Thread(() -> {
            while (true) {
                // Process incoming messages for all players.
                for (Player p : players) {
                    p.updateMessages();
                }
                
                try {
                    Thread.sleep(1); // Sleep briefly for responsiveness.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        updateThread.start();
    }
    
    /**
     * Adds a new player with the specified name.
     * You can adjust the default values for the player's position and speed as needed.
     *
     * @param playerName the name of the new player
     */
    public void addPlayer(String playerName) {
        // For example, create a new player with default position (0,0) and speed 5.0f.
        Player newPlayer = new Player(playerName, 0.0f, 0.0f, 5.0f);
        players.add(newPlayer);
        System.out.println("Added new player: " + newPlayer);
    }
    
    /**
     * Returns the current list of players.
     *
     * @return list of players
     */
    public List<Player> getPlayers() {
        return players;
    }
    
    /**
     * Adds an incoming message to the global message queue.
     * This method allows other parts of the game to manually add messages to be processed.
     *
     * @param msg the message to add
     */
    public void addIncomingMessage(Message msg) {
        incomingQueue.offer(msg);
    }

    // Other methods for the Game class can use the incomingQueue and players list.
}
