package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Key extends GameObject implements IGravityAffected, IGrabbable, IThrowable {

    // Bounding box and physics fields.
    private float x, y, width, height;
    // Physics properties.
    private float mass;
    private float vx = 0.0f, vy = 0.0f;
    private float terminalVelocity = 600.0f;
    // Time tracking for sync.
    private long lastSyncTime = System.nanoTime();
    // Ground state.
    private boolean onGround = false;
    // Ownership: ID of the player that "owns" the key.
    private String ownerId = null;
    // Grab/Carry fields.
    private boolean isGrabbed = false;
    private String grabbedBy = null;
    // Friction factor to reduce horizontal sliding when on ground.
    private final float frictionFactor = 0.8f;

    /**
     * Constructs a Key.
     *
     * @param name   The key's name.
     * @param x      Starting x coordinate.
     * @param y      Starting y coordinate.
     * @param width  Key width.
     * @param height Key height.
     * @param mass   Mass of the key.
     * @param gameId The game session ID.
     */
    public Key(String name, float x, float y, float width, float height, float mass, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.mass = mass;
    }

    @Override
    public float getMass() {
        return mass;
    }

    // -------------------------
    // IGrabbable Implementation
    // -------------------------
    @Override
    public void onGrab(String playerId) {
        isGrabbed = true;
        grabbedBy = playerId;
        vx = 0;
        vy = 0;
        // Attach key to the grabbing player's position.
        Game currentGame = getParentGame(); // Use the parent game reference.
        if (currentGame != null) {
            for (GameObject obj : currentGame.getGameObjects()) {
                if (obj.getId().equals(playerId) && obj instanceof Player) {
                    Player p = (Player) obj;
                    setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                    setY(p.getY() - getHeight());
                    Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                    sendMessage(syncMsg);
                    break;
                }
            }
        }
        System.out.println("Key " + getName() + " grabbed by player " + playerId);
    }

    @Override
    public void onRelease() {
        isGrabbed = false;
        grabbedBy = null;
        System.out.println("Key " + getName() + " released");
    }

    @Override
    public boolean isGrabbed() {
        return isGrabbed;
    }

    @Override
    public String getGrabbedBy() {
        return grabbedBy;
    }

    // -------------------------
    // IThrowable Implementation
    // -------------------------
    @Override
    public void throwObject(float throwVx, float throwVy) {
        vx = throwVx;
        vy = throwVy;
        onRelease();
        System.out.println("Key " + getName() + " thrown with velocity (" + vx + ", " + vy + ")");
        Message throwMsg = new Message("THROW", new Object[]{vx, vy}, null);
        sendMessage(throwMsg);
    }

    // -------------------------
    // IGravityAffected Implementation
    // -------------------------
    @Override
    public void applyGravity(float deltaTime) {
        if (!onGround && !isGrabbed) {
            vy += GravityEngine.GRAVITY * deltaTime;
            if (vy > terminalVelocity) {
                vy = terminalVelocity;
            }
            float newY = y + vy * deltaTime;
            float newX = x + vx * deltaTime;
            setY(newY);
            setX(newX);
            y = newY;
            x = newX;
        }
    }

    /**
     * Resolves collisions with other objects.
     * Also checks if the key touches a Door, which triggers the win condition.
     */
    private void resolveCollisions() {
        // Use the parent game reference instead of GameContext.
        Game currentGame = getParentGame();
        if (currentGame == null) return;
        final float tolerance = 5.0f;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;
            if (this.intersects(other)) {
                this.resolveCollision(other);
                // Assign ownership if colliding with a player and not yet owned.
                if (other instanceof Player && ownerId == null) {
                    ownerId = other.getId();
                    System.out.println("Key " + getName() + " now owned by player " + ownerId);
                }
                // Check win condition: if key touches a Door.
                if (other instanceof Door) {
                    System.out.println("Key " + getName() + " touched a Door. You won the game!");
                    Message winMsg = new Message("WIN", new Object[]{"You won the game!"}, null);
                    sendMessage(winMsg);
                    Platform.runLater(() -> {
                        // Optionally, add a win overlay message (e.g., display a label).
                    });
                }
                // Simple landing check.
                if (Math.abs((getY() + getHeight()) - other.getY()) < tolerance && vy >= 0) {
                    onGround = true;
                    vy = 0;
                }
            }
        }
    }

    @Override
    public void myUpdateLocal(float deltaTime) {
        long now = System.nanoTime();
        if (isGrabbed) {
            // Follow the grabbing player's position.
            Game currentGame = getParentGame();
            if (currentGame != null) {
                for (GameObject obj : currentGame.getGameObjects()) {
                    if (obj.getId().equals(grabbedBy) && obj instanceof Player) {
                        Player p = (Player) obj;
                        setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                        setY(p.getY() - getHeight());
                        // Send frequent sync updates (every 0.1 sec).
                        if (ownerId != null && ownerId.equals(currentGame.getSelectedGameObjectId())) {
                            if (now - lastSyncTime >= 100_000_000L) {
                                Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                                sendMessage(syncMsg);
                                lastSyncTime = now;
                            }
                        }
                        return;
                    }
                }
            }
        }
        // Not grabbed: update physics normally.
        onGround = false;
        applyGravity(deltaTime);
        resolveCollisions();
        // Apply friction when on ground.
        if (onGround) {
            vx *= frictionFactor;
            if (Math.abs(vx) < 1.0f) {
                vx = 0;
            }
        }
        // Send periodic sync updates when not grabbed.
        if (ownerId != null && ownerId.equals(getParentGame().getSelectedGameObjectId())) {
            if (now - lastSyncTime >= 1_000_000_000L) {
                Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                sendMessage(syncMsg);
                lastSyncTime = now;
            }
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used; use myUpdateLocal(deltaTime) instead.
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("SYNC".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newX = Float.parseFloat(params[0].toString());
                float newY = Float.parseFloat(params[1].toString());
                synchronized (this) {
                    setX(newX);
                    setY(newY);
                }
                System.out.println("Processed SYNC for " + getName() + ": new position x=" + newX + ", y=" + newY);
            } else {
                System.out.println("SYNC message received with insufficient parameters.");
            }
        } else if ("THROW".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newVx = Float.parseFloat(params[0].toString());
                float newVy = Float.parseFloat(params[1].toString());
                synchronized (this) {
                    vx = newVx;
                    vy = newVy;
                }
                System.out.println("Processed THROW for " + getName() + ": new velocity (" + vx + ", " + vy + ")");
            }
        } else {
            System.out.println("Unhandled message type: " + msg.getMessageType());
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.GOLD);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{getName(), x, y, width, height, mass, getGameId()};
    }

    // --------------------
    // Bounding Box Methods
    // --------------------
    @Override
    public float getX() { return x; }
    @Override
    public float getY() { return y; }
    @Override
    public float getWidth() { return width; }
    @Override
    public float getHeight() { return height; }
    @Override
    public void setX(float x) { this.x = x; }
    @Override
    public void setY(float y) { this.y = y; }
    @Override
    public void setWidth(float width) { this.width = width; }
    @Override
    public void setHeight(float height) { this.height = height; }
    @Override
    public Message createSnapshot() {
        // Pack the position, velocity, and acceleration into an Object array.
        Object[] params = new Object[]{
            x, y,   // Positionvelocity.x, velocity.y,   // Velocity
            
        };
        // Create a new message with type "SNAPSHOT" and an appropriate option (e.g., "UPDATE").
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        
        // Set the concealed parameters so receivers know the source of the snapshot.
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        
        return snapshotMsg;
    }
}
