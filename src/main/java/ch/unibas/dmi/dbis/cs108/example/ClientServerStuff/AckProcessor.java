package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@code AckProcessor} class is responsible for processing and sending acknowledgment (ACK) messages
 * for received data packets. It uses a {@link DatagramSocket} to send these ACK messages asynchronously.
 * <p>
 * When you call {@link #addAck(InetSocketAddress, String)}, an {@link AckEntry} is created and placed in a queue.
 * Once {@link #start()} is invoked, a continuous background loop handles these entries by constructing the ACK
 * messages and sending them to the specified destinations.
 * </p>
 */
public class AckProcessor {

    /**
     * The socket used for sending ACK messages.
     */
    private final DatagramSocket socket;

    /**
     * A thread-safe queue that holds acknowledgment entries (destination and UUID).
     * Entries are polled and processed asynchronously to avoid blocking the main thread.
     */
    private final ConcurrentLinkedQueue<AckEntry> ackQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a new {@code AckProcessor} with the specified {@link DatagramSocket}.
     *
     * @param socket The {@link DatagramSocket} used for sending ACK messages.
     */
    public AckProcessor(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Adds an acknowledgment entry to the queue for the specified destination and message UUID.
     * <p>
     * This method enqueues the ACK request asynchronously, so it returns immediately.
     * The actual sending of the ACK is handled by the background loop once {@link #start()} is called.
     * </p>
     *
     * @param destination The network address and port to which the ACK should be sent.
     * @param uuid        The unique identifier of the message to acknowledge.
     */
    public void addAck(InetSocketAddress destination, String uuid) {
        AsyncManager.run(() -> ackQueue.offer(new AckEntry(destination, uuid)));
    }

    /**
     * Starts an asynchronous loop that continuously polls the {@link #ackQueue} for pending ACK requests.
     * <p>
     * For each queued {@link AckEntry}, this loop constructs an ACK {@link Message} and sends it to the
     * corresponding destination. A log message is printed to the console upon successful transmission.
     * </p>
     */
    public void start() {
        AsyncManager.runLoop(() -> {
            AckEntry entry = ackQueue.poll();
            if (entry != null) {
                try {
                    // Create an ACK message with type "ACK" and the UUID as a parameter.
                    Message ackMsg = new Message("ACK", new Object[] { entry.uuid }, null);
                    // Optionally, you could also call ackMsg.setUUID(entry.uuid) if your protocol requires it.
                    String encodedAck = MessageCodec.encode(ackMsg);
                    byte[] data = encodedAck.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length,
                            entry.destination.getAddress(), entry.destination.getPort());
                    socket.send(packet);
                    System.out.println("Sent ACK for UUID " + entry.uuid + " to " + entry.destination);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Represents an acknowledgment entry containing the destination address and port,
     * along with the UUID of the message to be acknowledged.
     */
    private static class AckEntry {
        private final InetSocketAddress destination;
        private final String uuid;

        /**
         * Constructs a new {@code AckEntry} with the specified destination and UUID.
         *
         * @param destination The network address and port associated with this acknowledgment.
         * @param uuid        The unique identifier of the message being acknowledged.
         */
        public AckEntry(InetSocketAddress destination, String uuid) {
            this.destination = destination;
            this.uuid = uuid;
        }
    }
}
