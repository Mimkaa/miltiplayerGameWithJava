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
     * A {@link ConcurrentHashMap} that holds pending messages keyed by their UUID.
     * Each pending message is encapsulated in a {@link PendingMessage} object.
     */
    private final ConcurrentHashMap<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();

    /**
     * The {@code PendingMessage} class captures details about an in-flight (unacknowledged)
     * message, including the time it was last sent and its destination address and port.
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
         * @param message     the {@link Message} being sent
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
     * window size and retransmission timeout. A background loop (via {@link AsyncManager#runLoop(Runnable)})
     * is started to continuously check for message timeouts and handle retransmissions.
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
        AsyncManager.runLoop(() -> checkTimeouts());
    }

    /**
     * Sends a message to a specified destination. A sequence number and UUID
     * are automatically assigned to the message before sending. If the window
     * of unacknowledged messages is full, the message is not sent.
     *
     * <p>This method returns immediately, scheduling the actual send
     * asynchronously via {@link AsyncManager#run(Runnable)}.</p>
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
     * Asynchronously sends a packet containing the given message to the specified address and port.
     *
     * @param msg         the {@link Message} to encode and send
     * @param destination the destination {@link InetAddress}
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
     * Periodically checks for timeouts among pending messages. If a message has exceeded
     * its {@link #timeoutMillis}, it may be retransmitted if it is either:
     * <ul>
     *   <li>The base (lowest-sequence) message in the queue</li>
     *   <li>Its immediate predecessor message has been acknowledged (i.e., is no longer pending)</li>
     * </ul>
     * If neither condition is met, the message's last-sent timestamp is updated
     * (deferring actual retransmission).
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
     * Immediately resends any pending message whose last-sent time exceeds
     * {@link #timeoutMillis}, ignoring whether it is the base message or unblocked.
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
     * Acknowledges a message with the specified UUID, removing it from the
     * {@link #pendingMessages} map. This method schedules the removal
     * asynchronously via {@link AsyncManager#run(Runnable)}.
     *
     * @param uuid the UUID of the acknowledged message
     */
    public void acknowledge(String uuid) {
        AsyncManager.run(() -> {
            System.out.println("Received ACK for UUID " + uuid);
            pendingMessages.remove(uuid);
        });
    }
}
