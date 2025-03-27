package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import java.util.concurrent.ConcurrentLinkedQueue;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;

public class MessageHogger {
    // Internal queue to hold incoming messages.
    private final ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    
    // The TaskScheduler to encapsulate our task queue.
    private final TaskScheduler taskScheduler = new TaskScheduler();
    
    // Control flag for the processing loops.
    private volatile boolean running = true;
    
    /**
     * Constructs a MessageHogger, registers it with the global MessageHub,
     * and starts asynchronous loops for processing incoming messages
     * and scheduled tasks at roughly 60 FPS.
     */
    public MessageHogger() {
        // Register this instance with the global MessageHub.
        MessageHub.getInstance().addHogger(this);
        
        // Start processing scheduled tasks asynchronously at ~60 FPS.
        startProcessingCommandsAsync();
        
        // Start an asynchronous loop to poll incoming messages.
        AsyncManager.runLoop(() -> {
            if (!running) {
                Thread.currentThread().interrupt();
                return;
            }
            // Poll for a new message.
            Message msg = messageQueue.poll();
            if (msg != null) {
                // Instead of processing immediately, schedule its processing via TaskScheduler.
                taskScheduler.scheduleTask(() -> processMessage(msg));
            }
            // (Optional) You can include a very brief sleep here if needed.
        });
    }
    
    /**
     * Processes a single message.
     * Subclasses can override this method to implement custom processing logic.
     *
     * @param msg The message to process.
     */
    protected void processMessage(Message msg) {
        // Default processing: log the message.
        System.out.println(Thread.currentThread().getName() + " processed: " + msg);
    }
    
    /**
     * Adds a message to the internal queue.
     *
     * @param msg The message to add.
     */
    public void addMessage(Message msg) {
        messageQueue.offer(msg);
    }
    
    /**
     * Asynchronously processes all scheduled tasks at ~60 FPS.
     * This loop runs on one of the AsyncManager's threads.
     */
    public void startProcessingCommandsAsync() {
        AsyncManager.runLoop(() -> {
            taskScheduler.runAllTasks();
            
            try {
                Thread.sleep(16); // ~16 ms for a 60 FPS update rate.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Stops processing and unregisters this hogger from the MessageHub.
     */
    public void stop() {
        running = false;
        MessageHub.getInstance().removeHogger(this);
    }
}
