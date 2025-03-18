package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReliableUDPSender {
    private final DatagramSocket socket;
    private final int windowSize;
    private final long timeoutMillis;
    
    // Use AtomicInteger for thread-safe sequence generation.
    private final AtomicInteger nextSeqNum = new AtomicInteger(1);
    // Buffer to store pending messages, keyed by their UUID.
    private final ConcurrentHashMap<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    
    private class PendingMessage {
        Message message;
        volatile long lastSentTime;
        InetAddress destination;
        int destPort;
        
        PendingMessage(Message message, long lastSentTime, InetAddress destination, int destPort) {
            this.message = message;
            this.lastSentTime = lastSentTime;
            this.destination = destination;
            this.destPort = destPort;
        }
    }
    
    public ReliableUDPSender(DatagramSocket socket, int windowSize, long timeoutMillis) {
        this.socket = socket;
        this.windowSize = windowSize;
        this.timeoutMillis = timeoutMillis;
        // Start checking for timeouts asynchronously.
        AsyncManager.runLoop(() -> checkTimeouts());
    }
    
    /**
     * Sends a message reliably to the given destination.
     * This method assigns both a sequence number and a UUID to the message.
     * The sending itself is scheduled asynchronously.
     *
     * @param msg         The message to send.
     * @param destination The destination InetAddress.
     * @param destPort    The destination port.
     * @return The generated UUID for the message.
     */
    public void sendMessage(Message msg, InetAddress destination, int destPort) {
        
        // Schedule the entire send operation asynchronously.
        AsyncManager.run(() -> {
            try {
                // Generate the UUID immediately so we can return it.
                final String uuid = UUID.randomUUID().toString();

                // Atomically assign the sequence number.
                int seq = nextSeqNum.getAndIncrement();
                msg.setSequenceNumber(seq);
                msg.setUUID(uuid);
                
                if (pendingMessages.size() < windowSize) {
                    // Add the message to the pending map.
                    pendingMessages.put(uuid, new PendingMessage(msg, System.currentTimeMillis(), destination, destPort));
                    // Send the packet.
                    sendPacket(msg, destination, destPort);
                    System.out.println("Sent: " + MessageCodec.encode(msg));
                } else {
                    System.out.println("Window full. Message not sent: " + MessageCodec.encode(msg));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
    }
    
    private void sendPacket(Message msg, InetAddress destination, int destPort) {
    AsyncManager.run(() -> {
        try {
            String encoded = MessageCodec.encode(msg);
            byte[] data = encoded.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, destination, destPort);
            socket.send(packet);
            System.out.println("Asynchronously sent packet: " + encoded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
    }
    
    /**
     * Checks for timeouts among pending messages and retransmits them if needed.
     * For each pending message whose timeout has expired, it is retransmitted only if
     * it is unblocked (i.e. either it is the base message or its immediate predecessor,
     * as determined by the sequence number, is no longer pending).
     * Otherwise, its timer is updated.
     */
    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        if (pendingMessages.isEmpty()) {
            return;
        }
        
        // Create a sorted list of pending messages based on their sequence numbers.
        List<PendingMessage> sorted = new ArrayList<>(pendingMessages.values());
        sorted.sort(Comparator.comparingLong(pm -> pm.message.getSequenceNumber()));
        
        for (PendingMessage pm : sorted) {
            long seq = pm.message.getSequenceNumber();
            if (now - pm.lastSentTime >= timeoutMillis) {
                boolean canRetransmit = false;
                if (sorted.get(0).message.getSequenceNumber() == seq) {
                    // Base (lowest sequence) message.
                    canRetransmit = true;
                } else {
                    // Check if the immediate predecessor is no longer pending.
                    boolean predecessorExists = sorted.stream().anyMatch(p -> p.message.getSequenceNumber() == seq - 1);
                    if (!predecessorExists) {
                        canRetransmit = true;
                    }
                }
                if (canRetransmit) {
                    try {
                        sendPacket(pm.message, pm.destination, pm.destPort);
                        pm.lastSentTime = now;
                        System.out.println("Retransmitted message with UUID " + pm.message.getUUID() +
                                           " (seq " + seq + "): " + MessageCodec.encode(pm.message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Update timer if predecessor is still pending.
                    pm.lastSentTime = now;
                    System.out.println("Updated timer for message with UUID " + pm.message.getUUID() +
                                       " (seq " + seq + ")");
                }
            }
        }
    }

    /**
     * Force-resends any pending message whose timer has expired,
     * bypassing the predecessor check.
     * This function iterates through all pending messages and, if the
     * elapsed time since last sent is at least timeoutMillis, resends the message.
     */
    public void forceResendTimeouts() {
        long now = System.currentTimeMillis();
        if (pendingMessages.isEmpty()) {
            return;
        }
        // Iterate over all pending messages.
        for (PendingMessage pm : pendingMessages.values()) {
            if (now - pm.lastSentTime >= timeoutMillis) {
                try {
                    sendPacket(pm.message, pm.destination, pm.destPort);
                    pm.lastSentTime = now;
                    System.out.println("Force resent message with UUID " + pm.message.getUUID() +
                                       " (seq " + pm.message.getSequenceNumber() + "): " + MessageCodec.encode(pm.message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    

    
    
    /**
     * Processes an ACK that acknowledges a message with a specific UUID.
     * This method uses AsyncManager.run() to schedule ACK processing asynchronously.
     *
     * @param uuid The UUID of the acknowledged message.
     */
    public void acknowledge(String uuid) {
        AsyncManager.run(() -> {
            System.out.println("Received ACK for UUID " + uuid);
            pendingMessages.remove(uuid);
        });
    }
}
