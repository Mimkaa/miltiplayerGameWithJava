package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.UUID; // Added import for UUID
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public abstract class GameObject {

    // Unique identifier for this game object.
    private final String id = UUID.randomUUID().toString();

    // The "game name" or session ID this player belongs to.
    private final String myGameName;

    private final String name;

    // Holds incoming messages from external sources.
    protected final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();
    
    // Command queue for update logic and message collection.
    protected final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    
    // Internal reference for the outgoing message queue.
    // Initially null; will be set from updateAsync() if not provided yet.
    protected ConcurrentLinkedQueue<Message> messageQueue = null;

    // A shared static executor for all tasks, with a fixed pool of 20 threads.
    protected static final ExecutorService executor = Executors.newFixedThreadPool(20);

    public GameObject(String name, String myGameName) {
        this.name = name;
        this.myGameName = myGameName;
    }

    // Getter for the unique ID
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGameName() {
        return myGameName;
    }

    /**
     * Schedules an asynchronous update command. If the internal messageQueue is not yet set,
     * it is initialized with the provided queue.
     *
     * @param providedQueue the queue to use for sending messages if the internal reference is empty.
     */
    public void updateAsync(ConcurrentLinkedQueue<Message> providedQueue) {
        if (this.messageQueue == null && providedQueue != null) {
            this.messageQueue = providedQueue;
        }
        executor.submit(() -> {
            Command updateCommand = new CodeCommand(() -> {
                myUpdateLocal();
            });
            commandQueue.offer(updateCommand);
        });
    }

    /**
     * Abstract method for local update logic.
     * Subclasses (like Player) should override this method.
     */
    protected abstract void myUpdateLocal();

    /**
     * Abstract method for global update logic.
     * Subclasses should override this to process an incoming message.
     *
     * @param msg the message to process.
     */
    protected abstract void myUpdateGlobal(Message msg);

    /**
     * Helper method to enqueue a message into the internal messageQueue.
     */
    protected void sendMessage(Message msg) {
        if (messageQueue != null) {
            // Retrieve the current concealed parameters.
            String[] concealed = msg.getConcealedParameters();
            // Ensure there is enough space for two parameters.
            if (concealed == null || concealed.length < 2) {
                concealed = new String[2];
            }
            // Set the unique ID and game name.
            concealed[0] = getName();       // Unique identifier from the GameObject.
            concealed[1] = getGameName(); // Game name or session ID.
            msg.setConcealedParameters(concealed);
            messageQueue.offer(msg);
        }
    }

    /**
     * Schedules the addition of a new incoming message to this object's incomingMessages queue.
     */
    public void addIncomingMessageAsync(Message message) {
        executor.submit(() -> incomingMessages.offer(message));
    }

    /**
     * Schedules a one‐time collector command to poll for incoming messages.
     * (This should be called once during initialization and then re‑scheduled
     * after processing commands in the game loop.)
     */
    public void collectMessageUpdatesOnce() {
        commandQueue.offer(createIncomingMessageCommand());
    }
    
    /**
     * Creates a non-blocking command that polls for an incoming message.
     * If a message is found, it is processed immediately.
     */
    protected Command createIncomingMessageCommand() {
        return new CodeCommand(() -> {
            Message msg = incomingMessages.poll(); // Non-blocking poll.
            if (msg != null) {
                myUpdateGlobal(msg);
            }
        });
    }

    /**
     * Processes all commands from the command queue without blocking.
     * This should be called from the game loop (e.g., via a Swing Timer).
     */
    public void processCommandsNonBlocking() {
        Command cmd;
        while ((cmd = commandQueue.poll()) != null) {
            cmd.execute();
        }
        // After processing, re-schedule the collector command once.
        collectMessageUpdatesOnce();
    }

    /**
     * Shuts down the shared executor service.
     * Call this only once when the application ends.
     */
    public static void shutdownExecutor() {
        executor.shutdownNow();
    }

    /**
     * Helper method to extract the game name from a Message's concealed parameters.
     */
    protected String extractGameName(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];  // [0] is player name, [1] is game name.
        }
        return "UnknownGame";
    }

    public abstract void draw(Graphics g);
    
    // --- Command Pattern Definitions ---

    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Default does nothing.
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Default does nothing.
            }
        };
    }
    
    public interface Command {
        void execute();
    }
    
    public static class CodeCommand implements Command {
        private final Runnable code;
        public CodeCommand(Runnable code) {
            this.code = code;
        }
        public void execute() {
            code.run();
        }
    }
}
