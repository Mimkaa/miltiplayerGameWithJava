package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageHogger {
    // Queue for reliable processing (non-GAME messages).
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    // Queue for best-effort (GAME) messages.
    private final BlockingQueue<Message> bestEffortQueue = new LinkedBlockingQueue<>();

    private final TaskScheduler taskScheduler = new TaskScheduler();
    private volatile boolean running = true;

    public MessageHogger() {
        MessageHub.getInstance().addHogger(this);

        // Loop for best-effort GAME messages
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

        // Loop for reliable messages
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

    protected void processMessage(Message msg) {
        //System.out.println(Thread.currentThread().getName() + " processed reliably: " + msg);
    }

    protected void processBestEffortMessage(Message msg) {
        //System.out.println(Thread.currentThread().getName() + " processed best-effort GAME: " + msg);
    }

    public void addMessage(Message msg) {
        if ("GAME".equalsIgnoreCase(msg.getOption())) {
            bestEffortQueue.offer(msg);
        } else {
            messageQueue.offer(msg);
        }
    }

    public void stop() {
        running = false;
        taskScheduler.stop();
        MessageHub.getInstance().removeHogger(this);
    }
}
