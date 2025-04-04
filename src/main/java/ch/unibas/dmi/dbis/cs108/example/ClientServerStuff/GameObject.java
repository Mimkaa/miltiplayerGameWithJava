package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javafx.scene.canvas.GraphicsContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class GameObject {

    private String id = UUID.randomUUID().toString();
    private final String gameId;
    protected String name;
    protected final ConcurrentLinkedQueue<Message> incomingMessages = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();

    // Collidability flag: if true, object participates in collision detection.
    private boolean collidable = true;

    // NEW: Movable flag. If false, the object will not be moved by collision resolution.
    private boolean movable = true;

    public abstract void setWidth(float width);

    public abstract void setHeight(float height);

    // Abstract bounding box methods using float.
    public abstract float getX();
    public abstract float getY();
    public abstract float getWidth();
    public abstract float getHeight();

    public abstract void setX(float x);
    public abstract void setY(float y);

    public GameObject(String name, String gameId) {
        this.name = name;
        this.gameId = gameId;
    }

    // ------------------
    // Getters / Setters
    // ------------------
    public String getId() { return id; }
    public void setId(String newId) { this.id = newId; }
    public void setName(String newName) { this.name = newName; }
    public String getName() { return name; }
    public String getGameId() { return gameId; }
    public boolean isCollidable() { return collidable; }
    public void setCollidable(boolean collidable) { this.collidable = collidable; }

    // NEW: Movable getter and setter.
    public boolean isMovable() { return movable; }
    public void setMovable(boolean movable) { this.movable = movable; }

    // ------------------
    // Message Handling
    // ------------------
    public void addIncomingMessage(Message message) {
        incomingMessages.offer(message);
    }
    public void processIncomingMessages() {
        Message msg;
        while ((msg = incomingMessages.poll()) != null) {
            myUpdateGlobal(msg);
        }
    }
    public abstract void myUpdateLocal();
    protected abstract void myUpdateGlobal(Message msg);
    public abstract void draw(GraphicsContext gc);
    public abstract Object[] getConstructorParamValues();

    // ------------------
    // Collision Methods
    // ------------------
    public boolean intersects(GameObject other) {
        return this.getX() < other.getX() + other.getWidth() &&
                this.getX() + this.getWidth() > other.getX() &&
                this.getY() < other.getY() + other.getHeight() &&
                this.getY() + this.getHeight() > other.getY();
    }

    /**
     * Resolves a collision between this object and another.
     * If both objects are movable, they are moved equally.
     * If one object is immovable, only the movable object is adjusted.
     */
    public void resolveCollision(GameObject other) {
        float overlapX = Math.min(this.getX() + this.getWidth(), other.getX() + other.getWidth()) -
                Math.max(this.getX(), other.getX());
        float overlapY = Math.min(this.getY() + this.getHeight(), other.getY() + other.getHeight()) -
                Math.max(this.getY(), other.getY());

        // If both are movable, do symmetric resolution.
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
        }
        // If only this object is movable.
        else if (this.isMovable() && !other.isMovable()) {
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
        }
        // If only the other object is movable.
        else if (!this.isMovable() && other.isMovable()) {
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
        // If neither is movable, do nothing.
    }

    // ------------------
    // Command Interface
    // ------------------
    public interface Command {
        void execute();
    }
    public static class CodeCommand implements Command {
        private final Runnable code;
        public CodeCommand(Runnable code) { this.code = code; }
        public void execute() { code.run(); }
    }

    /**
     * Sends a message by attaching this object's unique ID and game session ID.
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

    public void processCommands() {
        Command cmd;
        while ((cmd = commandQueue.poll()) != null) {
            cmd.execute();
        }
    }

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
}
