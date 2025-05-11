package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;


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

    /**
     * Unique identifier for this game object.
     */
    private String id = UUID.randomUUID().toString();

    /**
     * The ID of the game to which this object belongs.
     */
    private final String gameId;

    /**
     * The name of this game object.
     */
    protected String name;

    /**
     * The most recent snapshot for this object (may be {@code null}).
     */
    private final AtomicReference<Message> latestSnapshot = new AtomicReference<>();

    /**
     * The queue of commands to be executed by this object.
     */
    public final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();

    /**
     * The game that owns this object.
     */
    protected Game parentGame;

    /**
     * Flag to indicate if the object is selected.
     */
    private boolean selected = false;

    /**
     * Flag to indicate if this object is collidable.
     */
    private boolean collidable = true;

    /**
     * Flag to indicate if the object is movable by external forces (e.g. collision resolution).
     */
    private boolean movable = true;


    /**
     * The position of the object.
     */
    private Player2.Vector2 pos; // Position of the object

    /**
     * Flag to indicate if the object is currently grabbed.
     */
    private boolean isGrabbed = false;


    /**
     * The ID of the player who grabbed the object.
     */
    private String grabbedById;

    /**
     * Sets the position of the game object.
     *
     * @param p the new position of the object
     */
    public void setPos(Player2.Vector2 p) {
        setX(p.x);
        setY(p.y);
    }

    /**
     * Returns the position of the game object.
     *
     * @return the position of the object
     */
    

    /**
     * Handles the grabbing of the game object by a player.
     *
     * @param playerId the ID of the player grabbing the object
     */
    public void onGrab(String playerId) {
        this.isGrabbed = true;
        this.grabbedById = playerId;
    }


    /**
     * Handles the release of the game object.
     */
    public void onRelease() {
        this.isGrabbed = false;
        this.grabbedById = null;
    }

    /**
     * Returns whether the game object is currently grabbed.
     *
     * @return {@code true} if the object is grabbed, otherwise {@code false}
     */
    public boolean isGrabbed() {
        return isGrabbed;
    }

    /**
     * Constructs a new GameObject with the specified name and game ID.
     *
     * @param name the name of the object
     * @param gameId the ID of the game this object belongs to
     */
    public GameObject(String name, String gameId) {
        this.name = name;
        this.gameId = gameId;
    }

    

    // Abstract bounding box methods for collision detection.

    /**
     * Returns the X-coordinate of the top-left corner of the bounding box.
     *
     * @return the X-coordinate
     */
    public abstract float getX();

    /**
     * Returns the Y-coordinate of the top-left corner of the bounding box.
     *
     * @return the Y-coordinate
     */
    public abstract float getY();

    /**
     * Returns the width of the bounding box.
     *
     * @return the width of the bounding box
     */
    public abstract float getWidth();

    /**
     * Returns the height of the bounding box.
     *
     * @return the height of the bounding box
     */
    public abstract float getHeight();

    /**
     * Sets the X-coordinate of the top-left corner of the bounding box.
     *
     * @param x the new X-coordinate
     */
    public abstract void setX(float x);

    /**
     * Sets the Y-coordinate of the top-left corner of the bounding box.
     *
     * @param y the new Y-coordinate
     */
    public abstract void setY(float y);

    /**
     * Sets the width of the bounding box.
     *
     * @param width the new width of the bounding box
     */
    public abstract void setWidth(float width);


    /**
     * Sets the height of the bounding box.
     *
     * @param height the new height of the bounding box
     */
    public abstract void setHeight(float height);

    /**
     * Returns the parent game of this object.
     *
     * @return the parent game
     */
    public Game getParentGame() {
        return parentGame;
    }

    /**
     * Sets the parent game of this object.
     *
     * @param parentGame the parent game to set
     */
    public void setParentGame(Game parentGame) {
        this.parentGame = parentGame;
    }

    /**
     * Returns whether this game object is selected.
     *
     * @return {@code true} if the object is selected, otherwise {@code false}
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selected status of this game object.
     *
     * @param selected the new selected status
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }


    // Getters and setters for various fields.

    /**
     * Returns the unique identifier of this object.
     *
     * @return the object ID
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier for this object.
     *
     * @param newId the new ID for the object
     */
    public void setId(String newId) { this.id = newId; }

    /**
     * Returns the name of this object.
     *
     * @return the object name
     */
    public String getName() { return name; }

    /**
     * Sets the name of this object.
     *
     * @param newName the new name for the object
     */
    public void setName(String newName) { this.name = newName; }

    /**
     * Returns the game ID this object belongs to.
     *
     * @return the game ID
     */
    public String getGameId() { return gameId; }

    /**
     * Returns whether this object is collidable.
     *
     * @return {@code true} if the object is collidable, otherwise {@code false}
     */
    public boolean isCollidable() { return collidable; }

    /**
     * Sets whether this object is collidable.
     *
     * @param collidable {@code true} to make the object collidable, {@code false} otherwise
     */
    public void setCollidable(boolean collidable) { this.collidable = collidable; }

    /**
     * Returns whether this object is movable.
     *
     * @return {@code true} if the object is movable, otherwise {@code false}
     */
    public boolean isMovable() { return movable; }

    /**
     * Sets whether this object is movable.
     *
     * @param movable {@code true} to make the object movable, {@code false} otherwise
     */
    public void setMovable(boolean movable) { this.movable = movable; }


    // === Messaging ===

    /**
     * Adds an incoming message to this object’s message queue.
     *
     * @param message the message to add
     */
    public void addIncomingMessage(Message message) {
        // Only add the message if the current queue size is less than or equal to 5.
        latestSnapshot.set(message); 
    }


    /**
     * Processes all queued messages for this object.
     */
    public void applyLatestSnapshot() {
        Message snap = latestSnapshot.getAndSet(null);   // atomic swap‑&‑clear
        if (snap != null) {
            myUpdateGlobal(snap);
        }
    }

    /**
     * Updates the object locally.
     */
    public abstract void myUpdateLocal();

    /**
     * Updates the object locally with a given delta time.
     *
     * @param deltaTime the time difference to update the object
     */
    public abstract void myUpdateLocal(float deltaTime);

    /**
     * Processes the incoming message globally.
     *
     * @param msg the message to process
     */
    protected abstract void myUpdateGlobal(Message msg);

    /**
     * Draws the object on the provided GraphicsContext.
     *
     * @param gc the GraphicsContext to draw on
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Returns the constructor parameter values for the object.
     *
     * @return the constructor parameters
     */
    public abstract Object[] getConstructorParamValues();

    /**
     * Creates a snapshot of the current state of the object.
     *
     * @return the snapshot message
     */
    public abstract Message createSnapshot();

    // === Collision ===

    /**
     * Checks whether this object intersects with another based on bounding boxes.
     *
     * @param other the other game object
     * @return true if the objects intersect
     */
    public boolean intersects(GameObject other) {
        return this.getX() < other.getX() + other.getWidth() &&
                this.getX() + this.getWidth() > other.getX() &&
                this.getY() < other.getY() + other.getHeight() &&
                this.getY() + this.getHeight() > other.getY();
    }

    /**
     * @param other        object to test against
     * @param widthFactor  multiplier for {@code other}'s width (1 = no change)
     * @return             {@code true} if the two boxes now overlap
     */
    public boolean intersects(GameObject other, double widthFactor) {

        /* --- scale other around its centre on the X-axis --- */
        double scaledWidth  = other.getWidth() * widthFactor;
        double deltaWidth   = scaledWidth - other.getWidth();     // growth (+) or shrinkage (−)
        double otherXScaled = other.getX() - deltaWidth / 2.0;    // shift left so centre stays put

        /* --- unchanged Y-axis (height) --- */
        double otherY       = other.getY();
        double otherHeight  = other.getHeight();

        return this.getX()               < otherXScaled + scaledWidth &&
                this.getX() + this.getWidth() > otherXScaled          &&
                this.getY()               < otherY + otherHeight      &&
                this.getY() + this.getHeight() > otherY;
    }


    /**
     * Resolves collisions based on whether objects are movable.
     *
     * @param other the other game object
     */
    public void resolveCollision(GameObject other) {
        float overlapX = Math.min(this.getX() + this.getWidth(), other.getX() + other.getWidth()) -
                Math.max(this.getX(), other.getX());
        float overlapY = Math.min(this.getY() + this.getHeight(), other.getY() + other.getHeight()) -
                Math.max(this.getY(), other.getY());
        if (this.isMovable() && other.isMovable()) {
            // Symmetric resolution.
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
            // Only adjust this object.
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
            // Only adjust the other object.
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
        void execute();
    }
    public static class CodeCommand implements Command {
        private final Runnable code;
        public CodeCommand(Runnable code) { this.code = code; }
        public void execute() { code.run(); }
    }

    public void processKeyboardState() 
    {
    }


    /**
     * Sends a message to the client with additional concealed parameters.
     * <p>
     * This method wraps a {@link Message} and adds the ID of the game object and
     * the game ID to the message's concealed parameters. It also sets the message
     * option to "GAME" before sending it through the static {@link Client#sendMessageStatic(Message)} method.
     * </p>
     *
     * @param msg the message to send
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
     * Processes all queued commands.
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
     * @param msg the message
     * @return the game ID or "UnknownGame" if missing
     */
    public String extractGameId(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];
        }
        return "UnknownGame";
    }
}