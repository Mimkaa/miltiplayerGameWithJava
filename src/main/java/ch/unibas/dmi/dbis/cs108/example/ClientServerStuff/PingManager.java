package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.InetAddress;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingManager {

    // The outgoing message queue to which ping messages are enqueued.
    private final ConcurrentLinkedQueue<Message> outgoingQueue;
    private final InetAddress serverAddress;
    private final int serverPort;
    private final ScheduledExecutorService scheduler;
    private final long periodMillis;
    
    // Memorize the time (in ms) when the last ping was enqueued.
    private volatile long lastPingTime = 0;

    /**
     * Constructs a new PingManager.
     *
     * @param outgoingQueue  The queue to which ping messages will be offered.
     * @param serverAddress  The destination server's InetAddress.
     * @param serverPort     The destination server's port.
     * @param periodMillis   The period between pings in milliseconds.
     */
    public PingManager(ConcurrentLinkedQueue<Message> outgoingQueue,
                       InetAddress serverAddress, int serverPort,
                       long periodMillis) {
        this.outgoingQueue = outgoingQueue;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.periodMillis = periodMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts the periodic ping process.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendPing();
            } catch (IOException e) {
                System.err.println("Failed to enqueue ping: " + e.getMessage());
                e.printStackTrace();
            }
        }, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the periodic ping process.
     */
    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Constructs and enqueues a PING message.
     * Memorizes the time at which the ping was enqueued.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void sendPing() throws IOException {
        // Create a new ping message.
        Message pingMessage = new Message("PING", new Object[] { "Ping" }, "REQUEST");

        String[] concealedPrms = { "something1", "something2"};

        pingMessage.setConcealedParameters(concealedPrms);

        // Get the current time in milliseconds and set it as a concealed parameter.
        long timestamp = System.currentTimeMillis();
        pingMessage.setConcealedParameters(new String[] { String.valueOf(timestamp) });

        // Offer the ping message to the outgoing queue.
        outgoingQueue.offer(pingMessage);
        
        // Memorize the time when the ping was enqueued.
        lastPingTime = timestamp;
        
    }

    /**
     * Returns the difference in milliseconds between the current time and the last ping time.
     *
     * @return the time difference in milliseconds as an integer.
     */
    public int getTimeDifferenceMillis() {
        long currentTime = System.currentTimeMillis();
        return (int) (currentTime - lastPingTime);
    }
}
