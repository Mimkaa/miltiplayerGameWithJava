package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread‑safe, 1‑second rolling rate meter.
 * <p>
 * Call {@link #mark()} every time you receive a message.
 * Read the latest sample via {@link #getMessagesPerSecond()}.
 * </p>
 */
public final class MessageRateMeter {

    /** incremented by worker threads for every message received */
    private static final AtomicInteger COUNTER = new AtomicInteger();

    /** last full‑second sample (updated by the scheduler) */
    private static volatile int messagesPerSecond = 0;

    /** single daemon thread that ticks once a second */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "msg‑rate‑meter");
                t.setDaemon(true);               // won’t keep the JVM alive
                return t;
            });

    static {
        // start the once‑per‑second task immediately
        SCHEDULER.scheduleAtFixedRate(() -> {
            int mps = COUNTER.getAndSet(0);      // atomic swap & reset
            messagesPerSecond = mps;             // publish the sample
            //System.out.printf("[NET] %d msg/s (last second)%n", mps);
        }, 1, 1, TimeUnit.SECONDS);
    }

    /** utility class — no instances */
    private MessageRateMeter() {}

    /** Call this from wherever you’ve just successfully decoded a message. */
    public static void mark() {
        COUNTER.incrementAndGet();
    }

    /** Latest messages‑per‑second sample (updated each real second). */
    public static int getMessagesPerSecond() {
        return messagesPerSecond;
    }

    /** Optional — clean shutdown if you ever need it. */
    public static void shutdown() {
        SCHEDULER.shutdownNow();
    }
}

