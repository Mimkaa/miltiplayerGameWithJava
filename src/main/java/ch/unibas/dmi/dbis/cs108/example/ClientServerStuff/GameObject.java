package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javafx.scene.canvas.GraphicsContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@code GameObject} class serves as an abstract base for game entities that can manage and
 * process incoming messages and commands.
 */
public abstract class GameObject {

    /**
     * A unique identifier for this game object, generated via {@link UUID#randomUUID()} by default.
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
     * Holds messages incoming from external sources.
     */
    protected final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();

    /**
     * A queue for storing commands (tasks) that are processed in a non-blocking manner.
     */
    protected final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a {@code GameObject} with a specified display name and game session ID.
     *
     * @param name     A display name or identifier for this object.
     * @param myGameId The unique identifier of the game/session this object belongs to.
     */
    public GameObject(String name, String myGameId) {
        this.name = name;
        this.myGameId = myGameId;
    }

    // ------------------
    // Getters / Setters
    // ------------------

    public String getId() {
        return id;
    }

    public void setId(String newId) {
        this.id = newId;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public String getName() {
        return name;
    }

    public String getGameId() {
        return myGameId;
    }

    // ------------------
    // Message Handling
    // ------------------

    /**
     * Adds an incoming message directly to the object's queue (no extra async).
     */
    public void addIncomingMessage(Message message) {
        incomingMessages.offer(message);
    }

    /**
     * Called by the Game loop to process all messages waiting in this object's queue.
     */
    public void processIncomingMessages() {
        Message msg;
        while ((msg = incomingMessages.poll()) != null) {
            myUpdateGlobal(msg);
        }
    }

    /**
     * Sends a message by attaching this object's unique ID and the game session's unique ID
     * as concealed parameters. Typically used for broadcasting local updates (e.g. position).
     *
     * @param msg the {@link Message} to be sent.
     */
    protected void sendMessage(Message msg) {
        // Attach concealed parameters for this object.
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null || concealed.length < 2) {
            concealed = new String[2];
        }
        concealed[0] = getId();
        concealed[1] = getGameId();
        msg.setConcealedParameters(concealed);

        // Mark this message as a "GAME" message so the server knows to handle it accordingly.
        msg.setOption("GAME");

        // Now send via a static client method or however your client code is set up.
        Client.sendMessageStatic(msg);
    }

    // ------------------
    // Command Handling
    // ------------------

    public ConcurrentLinkedQueue<Command> getCommandQueue() {
        return commandQueue;
    }

    /**
     * Called by the Game loop to process all commands in this object's commandQueue.
     */
    public void processCommands() {
        Command cmd;
        while ((cmd = commandQueue.poll()) != null) {
            cmd.execute();
        }
    }

    // ------------------
    // Updates
    // ------------------

    /**
     * Called by the Game loop for local update logic (e.g. movement, AI, etc.).
     */
    public abstract void myUpdateLocal();

    /**
     * Called for handling global (network-driven) updates from an incoming message.
     */
    protected abstract void myUpdateGlobal(Message msg);

    // ------------------
    // Drawing
    // ------------------

    /**
     * Called to render this GameObject onto a JavaFX {@link GraphicsContext}.
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Must be implemented by each subclass to return constructor parameter values.
     * Typically used for re-creating the object on the client or server.
     */
    public abstract Object[] getConstructorParamValues();

    // ------------------
    // Utility
    // ------------------

    /**
     * Extracts the game session ID (the second concealed parameter) from a message.
     */
    protected String extractGameId(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];
        }
        return "UnknownGame";
    }

    // ------------------
    // Command Interface
    // ------------------

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

