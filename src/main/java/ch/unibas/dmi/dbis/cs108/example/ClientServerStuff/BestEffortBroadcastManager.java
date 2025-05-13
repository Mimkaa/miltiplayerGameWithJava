package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simply enqueues clones of the given Message into each
 * BestEffortClientMessageSender’s shared BlockingDeque.
 */
public class BestEffortBroadcastManager {
    private final BlockingDeque<OutgoingMessage> outDeque;
    private final ConcurrentMap<String, BestEffortClientMessageSender> senders
        = new ConcurrentHashMap<>();

    /**
     * @param outDeque  the shared deque that all senders will enqueue into
     */
    public BestEffortBroadcastManager(BlockingDeque<OutgoingMessage> outDeque) {
        this.outDeque = outDeque;
    }

    /** Register (or replace) the sender for this user. */
    public void registerClient(String username, InetSocketAddress dest) {
        senders.put(
            username,
            new BestEffortClientMessageSender(dest, outDeque)
        );
    }

    /** Unregister: future broadcasts will skip this user. */
    public void unregisterClient(String username) {
        senders.remove(username);
    }

    /** Enqueue a clone of msg into _every_ sender. */
    public void broadcast(Message msg) {
        senders.values().forEach(sender -> {
            try {
                sender.enqueue(msg.clone());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /** Like broadcast, but skip one user. */
    public void broadcastExcept(Message msg, String excludedUsername) {
        senders.forEach((user, sender) -> {
            if (!user.equals(excludedUsername)) {
                try {
                    sender.enqueue(msg.clone());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /** Enqueue into a single client’s queue (if registered). */
    public void sendTo(String username, Message msg) throws InterruptedException {
        BestEffortClientMessageSender sender = senders.get(username);
        if (sender != null) {
            sender.enqueue(msg.clone());
        }
    }
}
