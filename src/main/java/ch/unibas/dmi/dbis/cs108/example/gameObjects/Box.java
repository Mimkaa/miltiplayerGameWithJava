package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Box extends GameObject implements IGravityAffected, IGrabbable {

    // Bounding box and physics fields.
    private float x;
    private float y;
    private float width;
    private float height;

    // Physics properties.
    private float mass;
    private float vy = 0.0f;                   // Vertical velocity (pixels/second)
    private float terminalVelocity = 600.0f;     // Maximum falling speed

    // Time tracking.
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
        // Stop falling when grabbed.
        vy = 0;
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

    /**
     * Applies gravity if the box is not on the ground and not grabbed.
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
            setY(newY);
            y = newY;
        }
    }

    /**
     * Uses the collision resolving provided by GameObject.
     * Iterates over collidable objects and, if an intersection is detected,
     * resolves the collision. Also assigns ownership if colliding with a player and
     * sets onGround if the box is landing.
     */
    private void resolveCollisionsUsingParent() {
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;

        final float tolerance = 5.0f;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;
            if (this.intersects(other)) {
                // Use the standard collision resolution.
                this.resolveCollision(other);

                // If colliding with a Player and not already owned, assign ownership.
                if (other instanceof Player && ownerId == null) {
                    ownerId = other.getId();
                    System.out.println("Box " + getName() + " is now owned by player " + ownerId);
                }
                // Check if the bottom of the box is near the top of the other object.
                if (Math.abs((this.getY() + this.getHeight()) - other.getY()) < tolerance && vy >= 0) {
                    onGround = true;
                    vy = 0;
                }
            }
        }
    }

    /**
     * Local update method called every frame.
     * If the box is grabbed, it sticks to the carrying player's position.
     * Otherwise, it resets the ground state, applies gravity, resolves collisions,
     * and sends a SYNC message with the current position every 2000 ms if this client is authoritative.
     *
     * @param deltaTime Time in seconds since the last update.
     */
    @Override
    public void myUpdateLocal(float deltaTime) {
        long now = System.nanoTime();

        // If grabbed, stick to the player.
        if (isGrabbed) {
            Game currentGame = GameContext.getGameById(getGameId());
            if (currentGame != null) {
                for (GameObject obj : currentGame.getGameObjects()) {
                    if (obj.getId().equals(grabbedBy) && obj instanceof Player) {
                        Player p = (Player) obj;
                        // Position the box above the player, centered horizontally.
                        setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                        setY(p.getY() - getHeight());
                        // Still send a SYNC update so other clients know the new position.
                        if (ownerId != null && ownerId.equals(GameContext.getSelectedGameObjectId())) {
                            if (now - lastSyncTime >= 2_000_000_000) { // every 2 seconds
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

        // Apply gravity if not on the ground.
        if (!onGround) {
            applyGravity(deltaTime);
        }

        // Resolve collisions.
        resolveCollisionsUsingParent();

        // Only the authoritative client (the one whose player ID matches ownerId) sends SYNC updates.
        if (ownerId != null && ownerId.equals(GameContext.getSelectedGameObjectId())) {
            if (now - lastSyncTime >= 2_000_000_000) { // every 2 seconds
                Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                sendMessage(syncMsg);
                lastSyncTime = now;
            }
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used; we prefer the deltaTime version.
    }

    /**
     * Processes global messages.
     * For boxes, we process "SYNC" messages that carry the exact position.
     *
     * @param msg The incoming message.
     */
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
