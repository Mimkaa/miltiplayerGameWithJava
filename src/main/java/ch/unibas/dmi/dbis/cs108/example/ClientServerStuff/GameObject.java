package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * The {@code GameObject} class serves as an abstract base for game entities that can manage and
 * process incoming messages, schedule local updates, and synchronize state across different
 * game instances or clients.
 * <p>
 * It maintains a unique identifier, a reference to the game session it belongs to, and provides
 * an internal queue for incoming messages and commands. Subclasses must implement drawing logic
 * and local/global update methods.
 * </p>
 */
public abstract class GameObject {

    /**
     * A unique identifier for this game object, generated via {@link UUID#randomUUID()}.
     */
    private String id = UUID.randomUUID().toString();

    /**
     * The name of the game or session that this object belongs to.
     */
    private final String myGameName;

    /**
     * A display-friendly name for this game object (e.g., player name).
     */
    protected String name;

    /**
     * Holds messages incoming from external sources, to be processed asynchronously.
     */
    protected final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();

    /**
     * A queue for storing commands (tasks) that are processed in a non-blocking manner.
     */
    protected final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();

    /**
     * A reference to the outgoing message queue used by this object. It is initially {@code null}
     * and may be set by {@link #updateAsync(ConcurrentLinkedQueue)} if it is not already set.
     */
    protected ConcurrentLinkedQueue<Message> messageQueue = null;

    /**
     * Constructs a {@code GameObject} with a specified name and game name.
     *
     * @param name       A display name or identifier for this object.
     * @param myGameName The name of the game or session this object belongs to.
     */
    public GameObject(String name, String myGameName) {
        this.name = name;
        this.myGameName = myGameName;
    }

    /**
     * Returns the unique identifier of this game object.
     *
     * @return the unique ID as a {@code String}.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets a new unique identifier for this game object.
     *
     * @param newId a {@code String} representing the new unique ID.
     */
    public void setId(String newId) {
        this.id = newId;
    }

    /**
     * Sets a new display name or identifier for this game object.
     *
     * @param newName a {@code String} for the object's name.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Returns the display name or identifier for this game object.
     *
     * @return the current name as a {@code String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the game/session to which this object belongs.
     *
     * @return the game name as a {@code String}.
     */
    public String getGameName() {
        return myGameName;
    }

    /**
     * Schedules an asynchronous update command that calls {@link #myUpdateLocal()}.
     * If the internal message queue is not set, it will be initialized with the provided queue.
     *
     * @param providedQueue a queue for outgoing messages, used if the internal queue is {@code null}.
     */
    public void updateAsync(ConcurrentLinkedQueue<Message> providedQueue) {
        if (this.messageQueue == null && providedQueue != null) {
            this.messageQueue = providedQueue;
        }
        AsyncManager.run(() -> {
            Command updateCommand = new CodeCommand(() -> {
                myUpdateLocal();
            });
            commandQueue.offer(updateCommand);
        });
    }

    /**
     * Abstract method for local update logic, to be implemented by subclasses.
     * This is called by the asynchronous update mechanism.
     */
    protected abstract void myUpdateLocal();

    /**
     * Abstract method for handling global updates from incoming messages.
     * Subclasses should implement logic for processing specific message types.
     *
     * @param msg a {@link Message} containing information to process.
     */
    protected abstract void myUpdateGlobal(Message msg);

    /**
     * Sends a message by placing it into the internal {@link #messageQueue}, attaching this
     * object's name and game name as concealed parameters.
     *
     * @param msg the {@link Message} to be sent to the queue.
     */
    protected void sendMessage(Message msg) {
        if (messageQueue != null) {
            String[] concealed = msg.getConcealedParameters();
            if (concealed == null || concealed.length < 2) {
                concealed = new String[2];
            }
            concealed[0] = getName();
            concealed[1] = getGameName();
            msg.setConcealedParameters(concealed);
            msg.setOption("GAME");
            messageQueue.offer(msg);
        }
    }

    /**
     * Schedules an operation to add an incoming message to this object's
     * {@link #incomingMessages} queue. This is done asynchronously.
     *
     * @param message the {@link Message} to be added to the queue.
     */
    public void addIncomingMessageAsync(Message message) {
        AsyncManager.run(() -> incomingMessages.offer(message));
    }

    /**
     * Schedules a one-time collector command to poll the {@link #incomingMessages} queue.
     * This should be called once during initialization, and then re-scheduled after commands
     * are processed in the game loop.
     */
    public void collectMessageUpdatesOnce() {
        AsyncManager.run(() -> commandQueue.offer(createIncomingMessageCommand()));
    }

    /**
     * Creates a command that checks for an incoming message in the {@link #incomingMessages} queue.
     * If a message is found, {@link #myUpdateGlobal(Message)} is invoked to process it.
     *
     * @return a {@link Command} instance that performs one poll operation on the message queue.
     */
    protected Command createIncomingMessageCommand() {
        return new CodeCommand(() -> {
            Message msg = incomingMessages.poll();
            if (msg != null) {
                myUpdateGlobal(msg);
            }
        });
    }

    /**
     * Processes all commands currently in the {@link #commandQueue} without blocking.
     * After processing, it re-schedules the collector command to be run again asynchronously.
     */
    public void processCommandsNonBlocking() {
        Command cmd;
        while ((cmd = commandQueue.poll()) != null) {
            cmd.execute();
        }
        collectMessageUpdatesOnce();
    }

    /**
     * Shuts down the underlying asynchronous mechanism. Should only be called once
     * when the application is ending.
     */
    public static void shutdownAsync() {
        AsyncManager.shutdown();
    }

    /**
     * Extracts and returns the game name (the second element) from a message's concealed parameters,
     * or returns a default value if unavailable.
     *
     * @param msg the {@link Message} from which to extract game information.
     * @return the extracted game name, or {@code "UnknownGame"} if not found.
     */
    protected String extractGameName(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];
        }
        return "UnknownGame";
    }

    /**
     * An abstract draw method that must be implemented by subclasses to
     * render this object using the provided {@link Graphics} context.
     *
     * @param g the {@code Graphics} context used for drawing.
     */
    public abstract void draw(Graphics g);

    /**
     * Returns a default {@link KeyAdapter} that does nothing on key events.
     * Subclasses can override this method to provide custom controls.
     *
     * @return a no-op {@link KeyAdapter}.
     */
    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };
    }

    /**
     * A command in the Command pattern, representing an action to be executed.
     */
    public interface Command {
        void execute();
    }

    /**
     * A {@link Command} implementation that wraps a {@link Runnable} task.
     */
    public static class CodeCommand implements Command {
        private final Runnable code;

        /**
         * Constructs a {@code CodeCommand} with the given {@link Runnable}.
         *
         * @param code a {@code Runnable} representing the action to be executed.
         */
        public CodeCommand(Runnable code) {
            this.code = code;
        }

        /**
         * Executes the wrapped {@link Runnable} task.
         */
        public void execute() {
            code.run();
        }
    }
}
