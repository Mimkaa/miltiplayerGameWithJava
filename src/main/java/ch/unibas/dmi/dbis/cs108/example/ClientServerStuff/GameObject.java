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
 * It maintains a unique identifier, the unique game session ID that it belongs to, and provides
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
     * The unique identifier of the game/session that this object belongs to.
     */
    private final String myGameId;

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
     * Constructs a {@code GameObject} with a specified display name and game session ID.
     *
     * @param name    A display name or identifier for this object.
     * @param myGameId The unique identifier of the game/session this object belongs to.
     */
    public GameObject(String name, String myGameId) {
        this.name = name;
        this.myGameId = myGameId;
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
     * Sets a new display name for this game object.
     *
     * @param newName a {@code String} for the object's name.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Returns the display name of this game object.
     *
     * @return the current name as a {@code String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unique game session ID to which this object belongs.
     *
     * @return the game session ID as a {@code String}.
     */
    public String getGameId() {
        return myGameId;
    }

    /**
     * Sends a message by attaching this object's unique ID and the game session's unique ID as concealed parameters,
     * then delegating the sending to the Client's static sendMessage method.
     *
     * @param msg the {@link Message} to be sent.
     */
    protected void sendMessage(Message msg) {
        // Attach concealed parameters.
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null || concealed.length < 2) {
            concealed = new String[2];
        }
        concealed[0] = getId();
        concealed[1] = getGameId();  // Use the game session's unique ID.
        msg.setConcealedParameters(concealed);
        msg.setOption("GAME");
        Client.sendMessageStatic(msg);
    }

    /**
     * Schedules an asynchronous update command that calls {@link #myUpdateLocal()}.
     */
    public void updateAsync() {
        AsyncManager.run(() -> {
            Command updateCommand = new CodeCommand(() -> myUpdateLocal());
            commandQueue.offer(updateCommand);
        });
    }

    /**
     * Abstract method for local update logic, to be implemented by subclasses.
     */
    protected abstract void myUpdateLocal();

    /**
     * Abstract method for handling global updates from incoming messages.
     *
     * @param msg a {@link Message} containing information to process.
     */
    protected abstract void myUpdateGlobal(Message msg);

    /**
     * Schedules an operation to add an incoming message to this object's queue.
     *
     * @param message the {@link Message} to be added.
     */
    public void addIncomingMessageAsync(Message message) {
        AsyncManager.run(() -> incomingMessages.offer(message));
    }

    /**
     * Schedules a one-time collector command to poll the incomingMessages queue.
     */
    public void collectMessageUpdatesOnce() {
        AsyncManager.run(() -> commandQueue.offer(createIncomingMessageCommand()));
    }

    /**
     * Creates a command that polls the incomingMessages queue and processes any found message.
     *
     * @return a {@link Command} instance.
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
     * Processes all commands currently in the commandQueue non-blockingly.
     */
    public void processCommandsNonBlocking() {
        Command cmd;
        while ((cmd = commandQueue.poll()) != null) {
            cmd.execute();
        }
        collectMessageUpdatesOnce();
    }

    /**
     * Shuts down asynchronous execution. Should be called when the application is ending.
     */
    public static void shutdownAsync() {
        AsyncManager.shutdown();
    }

    /**
     * Extracts the game session ID (the second concealed parameter) from the message.
     *
     * @param msg the {@link Message} from which to extract the game session ID.
     * @return the game session ID, or "UnknownGame" if not found.
     */
    protected String extractGameId(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];
        }
        return "UnknownGame";
    }

    /**
     * Must be implemented by each subclass to return constructor parameter values.
     *
     * @return an array of Objects.
     */
    public abstract Object[] getConstructorParamValues();

    /**
     * Abstract method for drawing this object using the provided Graphics context.
     *
     * @param g the Graphics context.
     */
    public abstract void draw(Graphics g);

    /**
     * Returns a default KeyAdapter that does nothing.
     *
     * @return a no-op KeyAdapter.
     */
    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {}
        };
    }

    /**
     * Command interface representing an executable action.
     */
    public interface Command {
        void execute();
    }

    /**
     * A simple implementation of Command that wraps a Runnable.
     */
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
