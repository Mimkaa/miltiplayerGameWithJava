package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * A Box that can be grabbed, thrown, and affected by gravity.
 */
public class Box extends GameObject implements IGravityAffected, IGrabbable, IThrowable {

    // Bounding box and physics fields.
    private float x;
    private float y;
    private float width;
    private float height;

    // Physics properties.
    private float mass;
    private float vy = 0.0f;                   // Vertical velocity (pixels/second)
    private float vx = 0.0f;                   // Horizontal velocity (pixels/second)
    private float terminalVelocity = 600.0f;   // Maximum falling speed

    // Time tracking for updates.
    private long lastUpdateNano = System.nanoTime();

    // Throttle time for sending sync updates.
    private long lastSyncTime = System.nanoTime();

    // Ground state.
    private boolean onGround = false;

    // Ownership: the ID of the player who touched this box.
    private String ownerId = null;

    // -------------------------
    // Grab / Carry Fields
    // -------------------------
    // Indicates if the box is currently grabbed.
    private boolean isGrabbed = false;
    // The ID of the player carrying this box.
    private String grabbedBy = null;

    /**
     * Constructs a Box.
     *
     * @param name   The box's name.
     * @param x      Starting x coordinate.
     * @param y      Starting y coordinate.
     * @param width  Box width.
     * @param height Box height.
     * @param mass   Mass of the box.
     * @param gameId The game session ID.
     */
    public Box(String name, float x, float y, float width, float height, float mass, String gameId) {
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
    // IGrabbable Interface Methods
    // -------------------------
    @Override
    public void onGrab(String playerId) {
        isGrabbed = true;
        grabbedBy = playerId;
        // Stop any movement.
        vx = 0;
        vy = 0;

        // Immediately update the position to the player's position.
        Game currentGame = getParentGame(); // Now using the parent game reference.
        if (currentGame != null) {
            for (GameObject obj : currentGame.getGameObjects()) {
                if (obj.getId().equals(playerId) && obj instanceof Player) {
                    Player p = (Player) obj;
                    setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                    setY(p.getY() - getHeight());
                    // Immediately send a SYNC message.
                    Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                    sendMessage(syncMsg);
                    break;
                }
            }
        }
        System.out.println("Box " + getName() + " grabbed by player " + playerId);
    }

    @Override
    public void onRelease() {
        isGrabbed = false;
        grabbedBy = null;
        System.out.println("Box " + getName() + " released");
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
    // IThrowable Interface Method
    // -------------------------
    @Override
    public void throwObject(float throwVx, float throwVy) {
        // Set the velocities from the throw vector.
        vx = throwVx;
        vy = throwVy;
        // Release the box.
        onRelease();
        System.out.println("Box " + getName() + " thrown with velocity (" + vx + ", " + vy + ")");
        // Send a THROW message so other clients update the velocity.
        Message throwMsg = new Message("THROW", new Object[]{vx, vy}, null);
        sendMessage(throwMsg);
    }

    /**
     * Applies gravity if the box is not on the ground and not grabbed.
     * Also updates horizontal position based on vx.
     *
     * @param deltaTime Time in seconds since the last update.
     */
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
     * Uses the collision resolving provided by GameObject.
     * Iterates over collidable objects and, if an intersection is detected,
     * resolves the collision. Also assigns ownership if colliding with a player and
     * sets onGround if the box is landing.
     */
    private void resolveCollisionsUsingParent() {
        Game currentGame = getParentGame();  // Using the parent game reference.
        if (currentGame == null) return;

        final float tolerance = 5.0f;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;

            if (this.intersects(other)) {
                this.resolveCollision(other);

                // If we collide with a Player & don't yet have an owner, set owner.
                if (other instanceof Player && ownerId == null) {
                    ownerId = other.getId();
                    System.out.println("Box " + getName() + " is now owned by player " + ownerId);
                }

                // Check if we landed on top of 'other'
                // (if the difference between the bottom of the box and the top of other is within tolerance and velocity is downward)
                if (Math.abs((this.getY() + this.getHeight()) - other.getY()) < tolerance && vy >= 0) {
                    onGround = true;
                    vy = 0;
                }
            }
        }
    }

    /**
     * Local update method called every frame.
     * If the box is grabbed, it "sticks" to the carrying player's position and sends frequent SYNC updates (every 0.1 second).
     * Otherwise, it applies gravity, resolves collisions, applies friction when on the ground, and sends SYNC updates less frequently.
     *
     * @param deltaTime Time in seconds since the last update.
     */
    @Override
    public void myUpdateLocal(float deltaTime) {
        long now = System.nanoTime();

        // If grabbed, follow the player's position.
        if (isGrabbed) {
            Game currentGame = getParentGame(); // Using parent reference.
            if (currentGame != null) {
                for (GameObject obj : currentGame.getGameObjects()) {
                    if (obj.getId().equals(grabbedBy) && obj instanceof Player) {
                        Player p = (Player) obj;
                        setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                        setY(p.getY() - getHeight());
                        // Send frequent SYNC updates (every 0.1 second) so others see the carried position.
                        // Replace GameContext.getSelectedGameObjectId() with parent's selected object if available.
                        if (ownerId != null && ownerId.equals(getParentGame().getSelectedGameObjectId())) {
                            if (now - lastSyncTime >= 100_000_000L) { // 0.1 second in nanoseconds.
                                Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                                sendMessage(syncMsg);
                                lastSyncTime = now;
                            }
                        }
                        return; // Skip further updates while grabbed.
                    }
                }
            }
        }

        // Not grabbed: reset ground state.
        onGround = false;

        // Apply gravity.
        if (!onGround) {
            applyGravity(deltaTime);
        }

        // Resolve collisions with other objects.
        resolveCollisionsUsingParent();

        // If on ground, apply friction to horizontal velocity.
        if (onGround) {
            float frictionFactor = 0.8f; // Adjust friction as needed.
            vx *= frictionFactor;
            if (Math.abs(vx) < 1.0f) {
                vx = 0;
            }
        }

        // Send less frequent SYNC updates when not grabbed (every 1 second).
        if (ownerId != null && ownerId.equals(getParentGame().getSelectedGameObjectId())) {
            if (now - lastSyncTime >= 1_000_000_000L) { // 1 second.
                Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                sendMessage(syncMsg);
                lastSyncTime = now;
            }
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used; the deltaTime version is preferred.
    }

    /**
     * Processes global messages.
     * For boxes, we process "SYNC" messages to update position and "THROW" messages to update velocities.
     * If the box is grabbed by the controlling client, global SYNC messages are ignored.
     *
     * @param msg The incoming message.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        // If the box is grabbed by the controlling client, skip global SYNC updates.
        if (isGrabbed && grabbedBy != null &&
            grabbedBy.equals(getParentGame().getSelectedGameObjectId())) {
            return;
        }
        if ("SYNC".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newX = Float.parseFloat(params[0].toString());
                float newY = Float.parseFloat(params[1].toString());
                synchronized (this) {
                    setX(newX);
                    setY(newY);
                }
                System.out.println("Processed SYNC for " + getName() +
                        ": new position x=" + newX + ", y=" + newY);
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
                System.out.println("Processed THROW for " + getName() +
                        ": new velocity (" + vx + ", " + vy + ")");
            }
        } else {
            System.out.println("Unhandled message type: " + msg.getMessageType());
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.RED);
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
    public float getX() {
        return x;
    }
    @Override
    public float getY() {
        return y;
    }
    @Override
    public float getWidth() {
        return width;
    }
    @Override
    public float getHeight() {
        return height;
    }
    @Override
    public void setX(float x) {
        this.x = x;
    }
    @Override
    public void setY(float y) {
        this.y = y;
    }
    @Override
    public void setWidth(float width) {
        this.width = width;
    }
    @Override
    public void setHeight(float height) {
        this.height = height;
    }
}
