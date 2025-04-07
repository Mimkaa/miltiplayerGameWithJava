package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
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
    private String id = UUID.randomUUID().toString();
    private final String gameId;
    protected String name;
    protected final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();

    // Collision properties.
    private boolean collidable = true;
    // Whether the object is movable by external forces (e.g. collision resolution).
    private boolean movable = true;

    public GameObject(String name, String gameId) {
        this.name = name;
        this.gameId = gameId;
    }

    // Abstract bounding box methods for collision detection.
    public abstract float getX();
    public abstract float getY();
    public abstract float getWidth();
    public abstract float getHeight();
    public abstract void setX(float x);
    public abstract void setY(float y);
    public abstract void setWidth(float width);
    public abstract void setHeight(float height);

    // Getters and setters.
    public String getId() { return id; }
    public void setId(String newId) { this.id = newId; }
    public String getName() { return name; }
    public void setName(String newName) { this.name = newName; }
    public String getGameId() { return gameId; }
    public boolean isCollidable() { return collidable; }
    public void setCollidable(boolean collidable) { this.collidable = collidable; }
    public boolean isMovable() { return movable; }
    public void setMovable(boolean movable) { this.movable = movable; }


    // === Messaging ===

    /**
     * Adds an incoming message to this object’s message queue.
     *
     * @param message the message to add
     */
    public void addIncomingMessage(Message message) {
        incomingMessages.offer(message);
    }

    /**
     * Processes all queued messages for this object.
     */
    public void processIncomingMessages() {
        Message msg;
        while ((msg = incomingMessages.poll()) != null) {
            myUpdateGlobal(msg);
        }
    }

    public abstract void myUpdateLocal();
    public abstract void myUpdateLocal(float deltaTime);

    protected abstract void myUpdateGlobal(Message msg);
    public abstract void draw(GraphicsContext gc);
    public abstract Object[] getConstructorParamValues();

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
     * Resolves collisions based on whether objects are movable.
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


    /**
     * Simple command wrapper that runs arbitrary {@link Runnable} logic.
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
    protected String extractGameId(Message msg) {
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length > 1) {
            return concealed[1];
        }
        return "UnknownGame";
    }
}
