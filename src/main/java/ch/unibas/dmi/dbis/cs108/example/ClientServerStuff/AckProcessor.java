package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AckProcessor {
    // The socket used for sending ACK messages.
    private final DatagramSocket socket;
    // A concurrent queue that holds acknowledgment entries.
    private final ConcurrentLinkedQueue<AckEntry> ackQueue = new ConcurrentLinkedQueue<>();
    
    public AckProcessor(DatagramSocket socket) {
        this.socket = socket;
    }
    
    /**
     * Adds an acknowledgment entry for the given destination and message UUID.
     * @param destination The destination InetSocketAddress where the ACK should be sent.
     * @param uuid The unique identifier of the message to acknowledge.
     */
    public void addAck(InetSocketAddress destination, String uuid) {
        AsyncManager.run(() -> ackQueue.offer(new AckEntry(destination, uuid)));
    }
    
    /**
     * Starts an asynchronous loop that continuously polls the ackQueue.
     * If there are any entries, it creates an ACK message and sends it to the entry's destination.
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
     * A simple class representing an acknowledgment entry.
     */
    private static class AckEntry {
        private final InetSocketAddress destination;
        private final String uuid;
        
        public AckEntry(InetSocketAddress destination, String uuid) {
            this.destination = destination;
            this.uuid = uuid;
        }
    }
}
