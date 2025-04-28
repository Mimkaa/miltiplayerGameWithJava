package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@code MessageHogger} class is responsible for processing messages asynchronously
 * from two different queues: one for reliable messages and one for best-effort (GAME) messages.
 * It manages these queues and schedules tasks for processing using the {@code TaskScheduler}.
 */
public class MessageHogger {

    // Queue for reliable processing (non-GAME messages).
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    // Queue for best-effort (GAME) messages.
    private final BlockingQueue<Message> bestEffortQueue = new LinkedBlockingQueue<>();

    private final TaskScheduler taskScheduler = new TaskScheduler();
    private volatile boolean running = true;

    /**
     * Constructs a {@code MessageHogger} instance and starts the loops for processing
     * messages from the respective queues asynchronously.
     * The {@code MessageHogger} is registered with the {@code MessageHub}.
     */
    public MessageHogger() {
        MessageHub.getInstance().addHogger(this);

        // Loop for processing best-effort GAME messages asynchronously
        AsyncManager.runLoop(() -> {
            while (running) {
                try {
                    Message msg = bestEffortQueue.take();
                    taskScheduler.scheduleTask(() -> processBestEffortMessage(msg));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Loop for processing reliable messages asynchronously
        AsyncManager.runLoop(() -> {
            while (running) {
                try {
                    Message msg = messageQueue.take();
                    taskScheduler.scheduleTask(() -> processMessage(msg));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Processes a reliable message. This method can be extended to add actual
     * processing logic for reliable messages.
     *
     * @param msg The message to be processed.
     */
    protected void processMessage(Message msg) {
        //System.out.println(Thread.currentThread().getName() + " processed reliably: " + msg);
    }

    /**
     * Processes a best-effort (GAME) message. This method can be extended to add actual
     * processing logic for best-effort messages.
     *
     * @param msg The message to be processed.
     */
    protected void processBestEffortMessage(Message msg) {
        //System.out.println(Thread.currentThread().getName() + " processed best-effort GAME: " + msg);
    }

    /**
     * Adds a message to the appropriate queue based on its type.
     * If the message type is "GAME", it is added to the {@code bestEffortQueue}.
     * Otherwise, it is added to the {@code messageQueue}.
     *
     * @param msg The message to be added to the appropriate queue.
     */
    public void addMessage(Message msg) {
        if ("GAME".equalsIgnoreCase(msg.getOption())) {
            bestEffortQueue.offer(msg);
        } else {
            messageQueue.offer(msg);
        }
    }

    /**
     * Stops the {@code MessageHogger}, halting the message processing loops and
     * unregistering it from the {@code MessageHub}.
     */
    public void stop() {
        running = false;
        taskScheduler.stop();
        MessageHub.getInstance().removeHogger(this);
    }
}
