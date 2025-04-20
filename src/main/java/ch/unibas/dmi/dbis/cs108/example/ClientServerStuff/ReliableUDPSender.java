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

/**
 * The {@code ReliableUDPSender} class provides a mechanism for sending messages
 * over UDP with basic reliability. It uses a sliding window concept to control
 * how many messages can be unacknowledged at once, and retransmits messages that
 * are not acknowledged within a specified timeout period.
 */
public class ReliableUDPSender {

    /**
     * The {@link DatagramSocket} used to send UDP packets.
     */
    private final DatagramSocket socket;

    /**
     * The maximum number of unacknowledged messages allowed in the window.
     * If the window is full, new messages are not sent until space is freed
     * by acknowledgments.
     */
    private final int windowSize;

    /**
     * The timeout duration (in milliseconds) after which a message is considered
     * lost and eligible for retransmission if it is the base message or unblocked.
     */
    private final long timeoutMillis;

    /**
     * An {@link AtomicInteger} used to generate sequence numbers for outgoing messages.
     */
    private final AtomicInteger nextSeqNum = new AtomicInteger(1);

    /**
     * A map of UUID to {@link PendingMessage}, representing all unacknowledged messages.
     */
    private final ConcurrentHashMap<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();

    /**
     * Encapsulates a message that has been sent but not yet acknowledged.
     * Contains metadata required for potential retransmission.
     */
    private class PendingMessage {
        Message message;
        volatile long lastSentTime;
        InetAddress destination;
        int destPort;

        /**
         * Constructs a {@code PendingMessage} with the given message, last-sent time,
         * and destination details.
         *
         * @param message      the {@link Message} being sent
         * @param lastSentTime the timestamp (in milliseconds) when the message was last sent
         * @param destination  the destination {@link InetAddress}
         * @param destPort     the destination port
         */
        PendingMessage(Message message, long lastSentTime, InetAddress destination, int destPort) {
            this.message = message;
            this.lastSentTime = lastSentTime;
            this.destination = destination;
            this.destPort = destPort;
        }
    }

    /**
     * Constructs a {@code ReliableUDPSender} bound to the given socket, with a specified
     * window size and retransmission timeout. Starts a background loop to monitor
     * retransmissions.
     *
     * @param socket        the {@link DatagramSocket} used for sending messages
     * @param windowSize    the maximum number of unacknowledged messages
     * @param timeoutMillis the timeout (in milliseconds) after which an unacknowledged
     *                      message is eligible for retransmission
     */
    public ReliableUDPSender(DatagramSocket socket, int windowSize, long timeoutMillis) {
        this.socket = socket;
        this.windowSize = windowSize;
        this.timeoutMillis = timeoutMillis;
        AsyncManager.runLoop(this::checkTimeouts);
    }

    /**
     * Sends a message to a specified destination. Assigns a new sequence number and UUID
     * to the message. If the window is full, the message is not sent.
     *
     * @param msg         the {@link Message} to send
     * @param destination the destination {@link InetAddress}
     * @param destPort    the destination port
     */
    public void sendMessage(Message msg, InetAddress destination, int destPort) {
        AsyncManager.run(() -> {
            try {
                String uuid = UUID.randomUUID().toString();
                int seq = nextSeqNum.getAndIncrement();
                msg.setSequenceNumber(seq);
                msg.setUUID(uuid);

                if (pendingMessages.size() < windowSize) {
                    pendingMessages.put(uuid, new PendingMessage(msg, System.currentTimeMillis(), destination, destPort));
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

    /**
     * Sends the given message asynchronously as a UDP packet.
     *
     * @param msg         the {@link Message} to encode and send
     * @param destination the destination IP address
     * @param destPort    the destination port
     */
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
     * Checks the list of pending messages and retransmits messages that have
     * timed out based on {@link #timeoutMillis}.
     *
     * <p>Messages are retransmitted if:</p>
     * <ul>
     *   <li>They are the base message (lowest sequence number)</li>
     *   <li>Their predecessor message is already acknowledged (gap-free logic)</li>
     * </ul>
     */
    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        if (pendingMessages.isEmpty()) {
            return;
        }

        List<PendingMessage> sorted = new ArrayList<>(pendingMessages.values());
        sorted.sort(Comparator.comparingLong(pm -> pm.message.getSequenceNumber()));

        for (PendingMessage pm : sorted) {
            long seq = pm.message.getSequenceNumber();
            if (now - pm.lastSentTime >= timeoutMillis) {
                boolean canRetransmit = false;

                if (sorted.get(0).message.getSequenceNumber() == seq) {
                    canRetransmit = true;
                } else {
                    boolean predecessorExists = sorted.stream().anyMatch(
                            p -> p.message.getSequenceNumber() == seq - 1
                    );
                    if (!predecessorExists) {
                        canRetransmit = true;
                    }
                }

                if (canRetransmit) {
                    try {
                        sendPacket(pm.message, pm.destination, pm.destPort);
                        pm.lastSentTime = now;
                        System.out.println("Retransmitted message with UUID "
                                + pm.message.getUUID() + " (seq " + seq + "): "
                                + MessageCodec.encode(pm.message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    pm.lastSentTime = now;
                    System.out.println("Updated timer for message with UUID "
                            + pm.message.getUUID() + " (seq " + seq + ")");
                }
            }
        }
    }

    /**
     * Immediately resends any pending message that has exceeded the timeout threshold,
     * regardless of whether it’s the base message or unblocked.
     */
    public void forceResendTimeouts() {
        long now = System.currentTimeMillis();
        if (pendingMessages.isEmpty()) {
            return;
        }
        for (PendingMessage pm : pendingMessages.values()) {
            if (now - pm.lastSentTime >= timeoutMillis) {
                try {
                    sendPacket(pm.message, pm.destination, pm.destPort);
                    pm.lastSentTime = now;
                    System.out.println("Force resent message with UUID "
                            + pm.message.getUUID() + " (seq "
                            + pm.message.getSequenceNumber() + "): "
                            + MessageCodec.encode(pm.message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Marks a message as acknowledged, removing it from the pending queue.
     * This method is executed asynchronously.
     *
     * @param uuid the UUID of the acknowledged message
     */
    public void acknowledge(String uuid) {
        // ===== SYNCHRONOUS removal =====
        System.out.println("Received ACK for UUID " + uuid);
        PendingMessage removed = pendingMessages.remove(uuid);
        if (removed != null) {
            System.out.println(" Removed pending message with UUID " + uuid);
        } else {
            System.err.println(" No pending message found for UUID " + uuid);
        }
    }
}
