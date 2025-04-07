package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code MessageHub} is a central dispatcher for routing messages
 * to multiple {@link MessageHogger} instances in a thread-safe and asynchronous manner.
 * <p>
 * It is implemented as a singleton and uses a {@link CopyOnWriteArrayList} to
 * manage listeners and a {@link java.util.concurrent.ExecutorService} to
 * dispatch messages without blocking the main thread.
 * </p>
 */
public class MessageHub {

    /**
     * Returns the singleton instance of the {@code MessageHub}.
     *
     * @return the global MessageHub instance
     */
    private static final MessageHub instance = new MessageHub();

    public static MessageHub getInstance() {
        return instance;
    }

    // A thread-safe list to hold MessageHoggers.
    private final List<MessageHogger> hoggers = new CopyOnWriteArrayList<>();

    // An ExecutorService to dispatch messages asynchronously.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Registers a new MessageHogger.
     */
    public void addHogger(MessageHogger hogger) {
        hoggers.add(hogger);
    }

    /**
     * Removes a MessageHogger.
     */
    public void removeHogger(MessageHogger hogger) {
        hoggers.remove(hogger);
    }

    /**
     * Dispatches the given message to all registered MessageHoggers asynchronously.
     */
    public void dispatch(Message msg) {
        for (MessageHogger hogger : hoggers) {
            executor.submit(() -> hogger.addMessage(msg));
        }
    }

    /**
     * Shuts down the hub's executor gracefully.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
