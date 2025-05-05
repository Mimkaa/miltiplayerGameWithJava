package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages per-client BestEffortClientMessageSender instances and provides
 * broadcast and unicast methods for best-effort messaging.
 */
public class BestEffortBroadcastManager {
    private static final int DEFAULT_QUEUE_CAPACITY = 1024;

    private final DatagramSocket socket;
    private final ConcurrentMap<String, BestEffortClientMessageSender> senders = new ConcurrentHashMap<>();

    /**
     * Construct a manager using the given shared socket for all senders.
     */
    public BestEffortBroadcastManager(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Register (or update) a client by username and address.
     * Shuts down any existing sender for that user.
     */
    public void registerClient(String username, InetSocketAddress address) {
        // Remove old sender if present
        BestEffortClientMessageSender old = senders.remove(username);
        if (old != null) old.shutdown();

        // Create and start a new sender
        BestEffortClientMessageSender sender =
            new BestEffortClientMessageSender(socket, address, DEFAULT_QUEUE_CAPACITY);
        senders.put(username, sender);
    }

    /**
     * Unregister a client, shutting down its sender.
     */
    public void unregisterClient(String username) {
        BestEffortClientMessageSender sender = senders.remove(username);
        if (sender != null) sender.shutdown();
    }

    /**
     * Broadcast a message to all registered clients (best-effort).
     */
    public void broadcast(Message original) {
        senders.forEach((user, sender) -> {
            try {
                sender.enqueue(original.clone());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Optionally log or drop
            }
        });
    }

    /**
     * Broadcast a message to all clients except one.
     */
    public void broadcastExcept(Message original, String excludedUsername) {
        senders.forEach((user, sender) -> {
            if (user.equals(excludedUsername)) return;
            try {
                sender.enqueue(original.clone());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Send a message to a single client.
     */
    public void sendTo(String username, Message msg) throws InterruptedException {
        BestEffortClientMessageSender sender = senders.get(username);
        if (sender != null) {
            sender.enqueue(msg.clone());
        }
    }
}

