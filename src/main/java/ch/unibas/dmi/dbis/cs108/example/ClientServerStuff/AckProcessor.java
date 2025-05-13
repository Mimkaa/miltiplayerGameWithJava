package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central ACK processor that enqueues ACKs as OutgoingMessage
 * into a shared BlockingDeque, to be sent by your drain thread.
 */
public final class AckProcessor {

    /** The queue where all OutgoingMessage(ACK) will be stored */
    private static final AtomicReference<BlockingDeque<OutgoingMessage>> QUEUE =
        new AtomicReference<>();

    /** temporary staging area for new ACK requests */
    private static final ConcurrentLinkedQueue<AckEntry> PENDING =
        new ConcurrentLinkedQueue<>();

    /** single daemon that wakes up every few ms and flushes ACKs into QUEUE */
    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ack-processor");
            t.setDaemon(true);
            return t;
        });

    static {
        // every 2 ms, gather pending ACK entries and enqueue OutgoingMessages
        SCHEDULER.scheduleAtFixedRate(AckProcessor::flushToQueue, 0, 2, TimeUnit.MILLISECONDS);
    }

    /** Initialize once with your shared outgoingQueue */
    public static void init(BlockingDeque<OutgoingMessage> outgoingQueue) {
        if (!QUEUE.compareAndSet(null, outgoingQueue)) {
            throw new IllegalStateException("AckProcessor already initialised");
        }
    }

    /** Queue an ACK for the given UUID & destination */
    public static void enqueue(InetSocketAddress dest, String uuid) {
        PENDING.add(new AckEntry(dest, uuid));
    }

    /** How many ACKs are waiting? */
    public static int getQueueSize() {
        return PENDING.size();
    }

    /** Shutdown the scheduler if you ever need to */
    public static void shutdown() {
        SCHEDULER.shutdownNow();
    }

    /** Internal flush: drain PENDING → wrap into OutgoingMessage → enqueue to QUEUE */
    private static void flushToQueue() {
        BlockingDeque<OutgoingMessage> queue = QUEUE.get();
        if (queue == null) return;  // not yet initialized

        List<AckEntry> batch = new ArrayList<>();
        AckEntry e;
        while ((e = PENDING.poll()) != null) {
            batch.add(e);
        }
        if (batch.isEmpty()) return;

        for (AckEntry entry : batch) {
            Message ack = new Message("ACK", new Object[]{ entry.uuid }, "GAME");
            ack.setUUID("");  // no self-UUID
            queue.offerLast(
                new OutgoingMessage(ack,
                    entry.destination.getAddress(),
                    entry.destination.getPort())
            );
        }
    }

    private record AckEntry(InetSocketAddress destination, String uuid) {}
}
