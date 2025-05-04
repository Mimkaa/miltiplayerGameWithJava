package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central ACK sender.
 *
 * <pre>
 * AckProcessor.init(socket);                 // once at start‑up
 *
 * AckProcessor.enqueue(dest, uuid);          // anywhere in code
 *
 * int backlog = AckProcessor.getQueueSize(); // optional stats
 * </pre>
 *
 * All ACKs are fired by one daemon thread (“ack‑sender”) so they leave
 * the process in order and you never create extra threads / runnables
 * under high load.
 */
public final class AckProcessor {

    /* =============================================================
     * public API
     * =========================================================== */

    /** Must be called exactly once – typically after the socket is opened. */
    public static void init(DatagramSocket socket) {
        if (!SOCKET.compareAndSet(null, socket)) {
            throw new IllegalStateException("AckProcessor already initialised");
        }
    }

    /** Queue an ACK for the given UUID & destination. */
    public static void enqueue(InetSocketAddress dest, String uuid) {
        QUEUE.add(new AckEntry(dest, uuid));
    }

    /** Optional – see how far we’re lagging behind. */
    public static int getQueueSize() {
        return QUEUE.size();
    }

    /** Optional clean shutdown (not required for daemon threads). */
    public static void shutdown() {
        SCHEDULER.shutdownNow();
    }

    /* =============================================================
     * implementation details
     * =========================================================== */

    /** holds (destination, uuid) pairs */
    private static final ConcurrentLinkedQueue<AckEntry> QUEUE =
            new ConcurrentLinkedQueue<>();

    /** socket is injected once via init() */
    private static final AtomicReference<DatagramSocket> SOCKET =
            new AtomicReference<>();

    /** single daemon that wakes up every few ms and flushes the queue */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ack‑sender");
                t.setDaemon(true);
                return t;
            });

    static {
        // drain & send every 2 ms – tweak if you need a different cadence
        SCHEDULER.scheduleAtFixedRate(AckProcessor::flush,
                                      0, 2, TimeUnit.MILLISECONDS);
    }

    /** grab everything in the queue and send it in FIFO order */
    private static void flush() {
        DatagramSocket sock = SOCKET.get();
        if (sock == null) {                // not yet initialised
            return;
        }

        // drain without allocating one packet per entry
        List<AckEntry> batch = new ArrayList<>(64);
        for (AckEntry e; (e = QUEUE.poll()) != null; ) {
            batch.add(e);
        }
        if (batch.isEmpty()) return;

        try {
            for (AckEntry e : batch) {
                Message ack = new Message("ACK",
                        new Object[]{ e.uuid }, "GAME");
                ack.setUUID("");           // ACKs have no own‑UUID
                byte[] data = MessageCodec.encode(ack)
                                          .getBytes(StandardCharsets.UTF_8);

                DatagramPacket pkt = new DatagramPacket(
                        data, data.length,
                        e.destination.getAddress(),
                        e.destination.getPort());

                sock.send(pkt);
            }
        } catch (Exception ex) {
            // log & carry on – we don’t want to kill the scheduler
            ex.printStackTrace();
        }
    }

    /** tiny value‑object for the queue */
    private record AckEntry(InetSocketAddress destination, String uuid) {}
}
