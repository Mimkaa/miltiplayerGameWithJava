package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Abstract base class for all game objects within a session.
 * <p>
 * Each {@code GameObject} has a unique ID, a reference to its game session,
 * and supports rendering, updating, and communication via messages.
 * It also includes basic bounding box methods for collision detection
 * and command execution via a simple command queue.
 * </p>
 */
public abstract class GameObject {
    /** Unique identifier for this object. */
    private String id = UUID.randomUUID().toString();
    /** Identifier of the game session this object belongs to. */
    private final String gameId;
    /** Display name of this object. */
    protected String name;
    /** Queue of incoming messages to be processed. */
    protected final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();
    /** Queue of commands to execute in the update loop. */
    public final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    /** Reference to the parent game instance. */
    protected Game parentGame;

    /** Whether this object is currently selected. */
    private boolean selected = false;

    /** Whether this object participates in collision detection. */
    private boolean collidable = true;
    /** Whether this object can be moved by collision resolution or other forces. */
    private boolean movable = true;

    /** Position vector of this object. */
    private Player2.Vector2 pos;
    /** Whether this object is currently grabbed by a player. */
    private boolean isGrabbed = false;
    /** ID of the player who grabbed this object, if any. */
    private String grabbedById;

    /**
     * Constructs a new GameObject with the given name and game session ID.
     *
     * @param name   display name for this object
     * @param gameId identifier of the game session
     */
    public GameObject(String name, String gameId) {
        this.name = name;
        this.gameId = gameId;
    }

    /**
     * Sets the position vector of this object.
     *
     * @param pos new position as a Vector2
     */
    public void setPos(Player2.Vector2 pos) {
        this.pos = pos;
    }

    /**
     * Returns the current position vector of this object.
     *
     * @return current position as a Vector2
     */
    public Player2.Vector2 getPos() {
        return pos;
    }

    /**
     * Marks this object as grabbed by the specified player.
     *
     * @param playerId identifier of the grabbing player
     */
    public void onGrab(String playerId) {
        this.isGrabbed = true;
        this.grabbedById = playerId;
    }

    /**
     * Releases this object from being grabbed.
     */
    public void onRelease() {
        this.isGrabbed = false;
        this.grabbedById = null;
    }

    /**
     * Checks if this object is currently grabbed.
     *
     * @return true if grabbed, false otherwise
     */
    public boolean isGrabbed() {
        return isGrabbed;
    }

    // === Bounding Box Methods ===

    /**
     * Gets the x-coordinate of this object's bounding box.
     *
     * @return x position as float
     */
    public abstract float getX();

    /**
     * Gets the y-coordinate of this object's bounding box.
     *
     * @return y position as float
     */
    public abstract float getY();

    /**
     * Gets the width of this object's bounding box.
     *
     * @return width as float
     */
    public abstract float getWidth();

    /**
     * Gets the height of this object's bounding box.
     *
     * @return height as float
     */
    public abstract float getHeight();

    /**
     * Sets the x-coordinate of this object's bounding box.
     *
     * @param x new x position
     */
    public abstract void setX(float x);

    /**
     * Sets the y-coordinate of this object's bounding box.
     *
     * @param y new y position
     */
    public abstract void setY(float y);

    /**
     * Sets the width of this object's bounding box.
     *
     * @param width new width
     */
    public abstract void setWidth(float width);

    /**
     * Sets the height of this object's bounding box.
     *
     * @param height new height
     */
    public abstract void setHeight(float height);

    // === Parent Game Access ===

    /**
     * Gets the parent Game instance for this object.
     *
     * @return parent Game or null if not set
     */
    public Game getParentGame() {
        return parentGame;
    }

    /**
     * Sets the parent Game instance for this object.
     *
     * @param parentGame the Game to associate with this object
     */
    public void setParentGame(Game parentGame) {
        this.parentGame = parentGame;
    }

    // === Selection ===

    /**
     * Checks if this object is selected.
     *
     * @return true if selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Marks this object as selected or unselected.
     *
     * @param selected true to select, false to deselect
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    // === Getters and Setters ===

    /**
     * Gets the unique ID of this object.
     *
     * @return object ID as String
     */
    public String getId() { return id; }

    /**
     * Sets a new unique ID for this object.
     *
     * @param newId new identifier string
     */
    public void setId(String newId) { this.id = newId; }

    /**
     * Gets the display name of this object.
     *
     * @return name as String
     */
    public String getName() { return name; }

    /**
     * Sets the display name of this object.
     *
     * @param newName new name string
     */
    public void setName(String newName) { this.name = newName; }

    /**
     * Gets the game session ID this object belongs to.
     *
     * @return game session ID
     */
    public String getGameId() { return gameId; }

    /**
     * Checks whether this object is collidable.
     *
     * @return true if collidable
     */
    public boolean isCollidable() { return collidable; }

    /**
     * Enables or disables collision for this object.
     *
     * @param collidable true to enable collisions
     */
    public void setCollidable(boolean collidable) { this.collidable = collidable; }

    /**
     * Checks whether this object is movable.
     *
     * @return true if movable
     */
    public boolean isMovable() { return movable; }

    /**
     * Enables or disables movement of this object by external forces.
     *
     * @param movable true to allow movement
     */
    public void setMovable(boolean movable) { this.movable = movable; }

    // === Messaging ===

    /**
     * Adds an incoming message to this object’s message queue.
     * If the queue is full (>5), the message is discarded.
     *
     * @param message the message to enqueue
     */
    public void addIncomingMessage(Message message) {
        if (incomingMessages.size() <= 5) {
            incomingMessages.offer(message);
        } else {
            System.out.println("Incoming message queue full for object " + getId() + "; discarding message " + message.getMessageType());
        }
    }

    /**
     * Processes all queued incoming messages by invoking myUpdateGlobal.
     */
    public void processIncomingMessages() {
        Message msg;
        while ((msg = incomingMessages.poll()) != null) {
            myUpdateGlobal(msg);
        }
    }

    /**
     * Local update logic, called each frame or tick.
     */
    public abstract void myUpdateLocal();

    /**
     * Local update logic with fixed time step.
     *
     * @param deltaTime time since last update in seconds
     */
    public abstract void myUpdateLocal(float deltaTime);

    /**
     * Applies an update based on a received Message.
     *
     * @param msg the Message to handle
     */
    protected abstract void myUpdateGlobal(Message msg);

    /**
     * Renders this object using JavaFX graphics.
     *
     * @param gc GraphicsContext to draw on
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Returns constructor parameters used for serialization or replication.
     *
     * @return array of constructor arguments
     */
    public abstract Object[] getConstructorParamValues();

    /**
     * Creates a snapshot message containing the object's state.
     *
     * @return SNAPSHOT Message with state parameters
     */
    public abstract Message createSnapshot();

    // === Collision ===

    /**
     * Checks if this object intersects another using axis-aligned bounding boxes.
     *
     * @param other other GameObject to test against
     * @return true if the bounding boxes overlap
     */
    public boolean intersects(GameObject other) {
        return this.getX() < other.getX() + other.getWidth() &&
                this.getX() + this.getWidth() > other.getX() &&
                this.getY() < other.getY() + other.getHeight() &&
                this.getY() + this.getHeight() > other.getY();
    }

    /**
     * Checks if this object intersects another after scaling other's width.
     *
     * @param other       other GameObject to test against
     * @param widthFactor factor to scale other's width (1 = no change)
     * @return true if the scaled bounding boxes overlap
     */
    public boolean intersects(GameObject other, double widthFactor) {
        double scaledWidth  = other.getWidth() * widthFactor;
        double deltaWidth   = scaledWidth - other.getWidth();
        double otherXScaled = other.getX() - deltaWidth / 2.0;
        double otherY       = other.getY();
        double otherHeight  = other.getHeight();

        return this.getX()               < otherXScaled + scaledWidth &&
                this.getX() + this.getWidth() > otherXScaled          &&
                this.getY()               < otherY + otherHeight      &&
                this.getY() + this.getHeight() > otherY;
    }

    /**
     * Resolves collisions by adjusting positions based on movability.
     *
     * @param other the other GameObject involved in collision
     */
    public void resolveCollision(GameObject other) {
        float overlapX = Math.min(this.getX() + this.getWidth(), other.getX() + other.getWidth()) -
                Math.max(this.getX(), other.getX());
        float overlapY = Math.min(this.getY() + this.getHeight(), other.getY() + other.getHeight()) -
                Math.max(this.getY(), other.getY());
        if (this.isMovable() && other.isMovable()) {
            if (overlapX < overlapY) {
                if (this.getX() < other.getX()) {
                    this.setX(this.getX() - overlapX / 2);
                    other.setX(other.getX() + overlapX / 2);
                } else {
                    this.setX(this.getX() + overlapX / 2);
                    other.setX(other.getX() - overlapX / 2);
                }
            } else {
                if (this.getY() < other.getY()) {
                    this.setY(this.getY() - overlapY / 2);
                    other.setY(other.getY() + overlapY / 2);
                } else {
                    this.setY(this.getY() + overlapY / 2);
                    other.setY(other.getY() - overlapY / 2);
                }
            }
        } else if (this.isMovable() && !other.isMovable()) {
            if (overlapX < overlapY) {
                if (this.getX() < other.getX()) {
                    this.setX(this.getX() - overlapX);
                } else {
                    this.setX(this.getX() + overlapX);
                }
            } else {
                if (this.getY() < other.getY()) {
                    this.setY(this.getY() - overlapY);
                } else {
                    this.setY(this.getY() + overlapY);
                }
            }
        } else if (!this.isMovable() && other.isMovable()) {
            if (overlapX < overlapY) {
                if (this.getX() < other.getX()) {
                    other.setX(other.getX() + overlapX);
                } else {
                    other.setX(other.getX() - overlapX);
                }
            } else {
                if (this.getY() < other.getY()) {
                    other.setY(other.getY() + overlapY);
                } else {
                    other.setY(other.getY() - overlapY);
                }
            }
        }
    }

    // === Commands ===

    /**
     * Interface for deferred or queued commands related to game logic.
     */
    public interface Command {
        /** Execute the command logic. */
        void execute();
    }

    /**
     * Simple command implementation that wraps a Runnable.
     */
    public static class CodeCommand implements Command {
        private final Runnable code;
        /**
         * Creates a new CodeCommand with the given runnable.
         *
         * @param code runnable to execute when command runs
         */
        public CodeCommand(Runnable code) { this.code = code; }
        /**
         * Executes the wrapped code.
         */
        public void execute() { code.run(); }
    }

    /**
     * Sends a message via the static client, setting concealed parameters and option.
     *
     * @param msg Message to send
     */
    protected void sendMessage(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null || concealed.length < 2) {
            concealed = new String[2];
        }
        concealed[0] = getId();
        concealed[1] = getGameId();
        msg.setConcealedParameters(concealed);
        msg.setOption("GAME");
        Client.sendMessageStatic(msg);
    }

    /**
     * Processes and executes all queued commands.
     */
    public void processCommands() {
        Command cmd;
        while ((cmd = commandQueue.poll()) != null) {
            cmd.execute();
        }
    }

    /**
     * Extracts the game ID from a message’s concealed parameters.
     *
     * @param msg the message to inspect
     * @return the game ID or "UnknownGame" if not available
     */
    public String extractGameId(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];
        }
        return "UnknownGame";
    }
}
