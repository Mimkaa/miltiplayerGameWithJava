package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MultiQueueSender {
    private final int numQueues;
    private final ConcurrentLinkedQueue<OutgoingMessage>[] queues;
    private final ReliableUDPSender[] senders;
    
    // Helper class to bundle a Message with its destination.
    private static class OutgoingMessage {
        Message msg;
        InetAddress address;
        int port;
        
        public OutgoingMessage(Message msg, InetAddress address, int port) {
            this.msg = msg;
            this.address = address;
            this.port = port;
        }
    }
    
    @SuppressWarnings("unchecked")
    public MultiQueueSender(java.net.DatagramSocket socket, int windowSize, long timeoutMillis, int numQueues) {
        this.numQueues = numQueues;
        queues = new ConcurrentLinkedQueue[numQueues];
        senders = new ReliableUDPSender[numQueues];
        for (int i = 0; i < numQueues; i++) {
            queues[i] = new ConcurrentLinkedQueue<>();
            // Create a separate ReliableUDPSender instance for each queue.
            senders[i] = new ReliableUDPSender(socket, windowSize, timeoutMillis);
            startSenderThread(i);
        }
    }
    
    /**
     * Starts a dedicated sender thread for the queue at the given index.
     */
    private void startSenderThread(final int index) {
        AsyncManager.runLoop(() -> {
            OutgoingMessage om = queues[index].poll();
            if (om != null) {
                try {
                    senders[index].sendMessage(om.msg, om.address, om.port);
                    System.out.println("Queue " + index + " sent message to " + om.address + ":" + om.port);
                } catch (Exception e) {
                    System.err.println("Queue " + index + " error sending message to " 
                        + om.address + ":" + om.port + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Enqueues a message for sending.
     * The message is placed in the queue with the fewest messages.
     *
     * @param msg     The message to send.
     * @param address The destination InetAddress.
     * @param port    The destination port.
     */
    public void enqueue(Message msg, InetAddress address, int port) {
        int targetIndex = 0;
        int minSize = Integer.MAX_VALUE;
        // Choose the queue with the least number of messages.
        for (int i = 0; i < numQueues; i++) {
            int size = queues[i].size();
            if (size < minSize) {
                minSize = size;
                targetIndex = i;
            }
        }
        queues[targetIndex].offer(new OutgoingMessage(msg, address, port));
        System.out.println("Enqueued message in queue " + targetIndex + " for " + address + ":" + port);
    }
    
    /**
     * (Optional) Returns the total number of messages across all queues.
     */
    public int getTotalQueueSize() {
        int total = 0;
        for (int i = 0; i < numQueues; i++) {
            total += queues[i].size();
        }
        return total;
    }
    
    /**
     * Asynchronously acknowledges a message with the given UUID across all ReliableUDPSender instances,
     * and then triggers immediate delivery (if any messages are unblocked).
     */
    public void acknowledge(String uuid) {
        AsyncManager.run(() -> {
            for (int i = 0; i < numQueues; i++) {
                // Acknowledge the message on this sender.
                senders[i].acknowledge(uuid);
            }
            System.out.println("Acknowledged and triggered delivery for UUID: " + uuid);
        });
    }
}

