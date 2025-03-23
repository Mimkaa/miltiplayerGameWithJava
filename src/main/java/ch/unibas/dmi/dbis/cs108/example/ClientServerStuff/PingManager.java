package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.InetAddress;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@code PingManager} class sends periodic "PING" messages to a specified server
 * and maintains a timestamp of the last time a ping was sent. It is useful for
 * measuring round-trip times or simply keeping a connection alive.
 */
public class PingManager {

    /**
     * The outgoing message queue to which ping messages are enqueued.
     */
    private final ConcurrentLinkedQueue<Message> outgoingQueue;

    /**
     * The {@link InetAddress} of the server to which pings are sent.
     */
    private final InetAddress serverAddress;

    /**
     * The destination port on the server for the pings.
     */
    private final int serverPort;

    /**
     * The {@link ScheduledExecutorService} that schedules periodic ping tasks.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * The period (in milliseconds) between consecutive pings.
     */
    private final long periodMillis;

    /**
     * The timestamp (in milliseconds) of the last time a ping was enqueued.
     */
    private volatile long lastPingTime = 0;

    /**
     * Constructs a new {@code PingManager} for sending periodic ping messages.
     *
     * @param outgoingQueue  the queue to which ping messages will be offered
     * @param serverAddress  the destination server's {@link InetAddress}
     * @param serverPort     the destination server's port
     * @param periodMillis   the period between consecutive pings, in milliseconds
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
     * Starts the periodic ping process, scheduling ping messages to be sent
     * at fixed intervals defined by {@link #periodMillis}.
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
     * Stops the periodic ping process by shutting down the scheduler.
     * Any ongoing or future ping tasks are halted.
     */
    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Constructs and enqueues a "PING" message, storing the current system time
     * as the last ping timestamp. This method is called by the periodic scheduler.
     *
     * @throws IOException if there is an error creating or enqueuing the ping message
     */
    private void sendPing() throws IOException {
        Message pingMessage = new Message("PING", new Object[] { System.currentTimeMillis() }, "REQUEST");
        Client.sendMessageStatic(pingMessage);
        System.out.println(pingMessage);
        lastPingTime = System.currentTimeMillis();
    }

    /**
     * Returns the elapsed time in milliseconds since the last ping was enqueued.
     * If no ping has been sent yet, this value may be the time since object creation.
     *
     * @return the time difference in milliseconds as an integer
     */
    public int getTimeDifferenceMillis() {
        long currentTime = System.currentTimeMillis();
        return (int) (currentTime - lastPingTime);
    }
}
