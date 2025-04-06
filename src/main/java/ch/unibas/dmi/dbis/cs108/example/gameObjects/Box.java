package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Box extends GameObject implements IGravityAffected {

    // Bounding box and physics fields.
    private float x;
    private float y;
    private float width;
    private float height;

    // Physics properties.
    private float mass;
    private float vy = 0.0f;                   // Vertical velocity in pixels/second
    private float terminalVelocity = 600.0f;     // Maximum falling speed

    // Time tracking.
    private long lastUpdateNano = System.nanoTime();

    // Throttle time for sending sync updates.
    private long lastSyncTime = System.nanoTime();

    // Ground state.
    private boolean onGround = false;

    // Ownership: the ID of the player who touched this object.
    private String ownerId = null;

    private double synchInterval = 1000000000; // every 2000 ms objects are synchronized based on the ownerId's client authority.

    /**
     * Constructs a Box.
     *
     * @param name   The object's name.
     * @param x      Starting x coordinate.
     * @param y      Starting y coordinate.
     * @param width  Object width.
     * @param height Object height.
     * @param mass   Mass of the object.
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

    /**
     * Applies gravity if the object is not on the ground.
     *
     * @param deltaTime Time in seconds since the last update.
     */
    @Override
    public void applyGravity(float deltaTime) {
        if (!onGround) {
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
     * For every collidable object in the game session that intersects this falling object,
     * we call the inherited resolveCollision method.
     * We also check if the object is landing (its bottom is near another object's top).
     */
    private void resolveCollisionsUsingParent() {
        // Get the current game session.
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;

        final float tolerance = 5.0f;
        // Iterate over all game objects.
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;

            if (this.intersects(other)) {
                // Use the common collision resolving method from GameObject.
                this.resolveCollision(other);

                // If the colliding object is a Player, assign ownership if not already set.
                if (other instanceof Player && ownerId == null) {
                    ownerId = other.getId();
                    System.out.println("Falling object " + getName() + " is now owned by player " + ownerId);
                }

                // Check if the falling object's bottom is near the other object's top.
                if (Math.abs((this.getY() + this.getHeight()) - other.getY()) < tolerance && vy >= 0) {
                    onGround = true;
                    vy = 0;
                }
            }
        }
    }

    /**
     * Local update method called every frame.
     * It resets the ground state, applies gravity, resolves collisions using the parent method,
     * and sends a SYNC message with the current position every 1000 ms if this client is authoritative.
     *
     * @param deltaTime Time in seconds since the last update.
     */
    @Override
    public void myUpdateLocal(float deltaTime) {
        long now = System.nanoTime();

        // Reset ground state at the beginning of each frame.
        onGround = false;

        // Apply gravity if not on the ground.
        if (!onGround) {
            applyGravity(deltaTime);
        }

        // Resolve collisions.
        resolveCollisionsUsingParent();

        // Only the authoritative client (the one whose player ID matches ownerId) sends SYNC updates.
        if (ownerId != null && ownerId.equals(GameContext.getSelectedGameObjectId())) {
            if (now - lastSyncTime >= synchInterval) {
                Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                sendMessage(syncMsg);
                lastSyncTime = now;
            }
        }

    }

    @Override
    public void myUpdateLocal() {
        // Not used; update method with deltaTime is preferred.
    }

    /**
     * Processes global messages.
     * For falling objects, we process "SYNC" messages that carry the exact position.
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
}
