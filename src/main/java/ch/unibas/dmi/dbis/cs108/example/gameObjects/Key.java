package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
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
    // Time tracking for synchronisation.
    private long lastSyncTime = System.nanoTime();
    // Ground state.
    private boolean onGround = false;
    private String ownerId = null;
    // Grab/Carry fields.
    private boolean isGrabbed = false;
    private String grabbedBy = null;

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

    // IGrabbable interface implementation.
    @Override
    public void onGrab(String playerId) {
        isGrabbed = true;
        grabbedBy = playerId;
        vx = 0;
        vy = 0;
        // Immediately attach the key to the grabbing player.
        Game currentGame = GameContext.getGameById(getGameId());
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

    // IThrowable interface implementation.
    @Override
    public void throwObject(float throwVx, float throwVy) {
        vx = throwVx;
        vy = throwVy;
        onRelease();
        System.out.println("Key " + getName() + " thrown with velocity (" + vx + ", " + vy + ")");
        Message throwMsg = new Message("THROW", new Object[]{vx, vy}, null);
        sendMessage(throwMsg);
    }

    // IGravityAffected implementation.
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
     * Uses the collision resolution method provided by GameObject.
     * Also, if the key intersects with a Door, a win condition is triggered.
     */
    private void resolveCollisions() {
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;
        final float tolerance = 5.0f;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;
            if (this.intersects(other)) {
                this.resolveCollision(other);
                // If colliding with a Player and not yet owned, assign ownership.
                if (other instanceof Player && ownerId == null) {
                    ownerId = other.getId();
                    System.out.println("Key " + getName() + " now owned by player " + ownerId);
                }
                // Check if the key touches a door to trigger win.
                if (other instanceof Door) {
                    System.out.println("Key " + getName() + " touched Door. You won the game!");
                    Message winMsg = new Message("WIN", new Object[]{"You won the game!"}, null);
                    sendMessage(winMsg);
                }
                // Simple landing check.
                if (Math.abs((this.getY() + this.getHeight()) - other.getY()) < tolerance && vy >= 0) {
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
            // While grabbed, follow the player's position.
            Game currentGame = GameContext.getGameById(getGameId());
            if (currentGame != null) {
                for (GameObject obj : currentGame.getGameObjects()) {
                    if (obj.getId().equals(grabbedBy) && obj instanceof Player) {
                        Player p = (Player) obj;
                        setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                        setY(p.getY() - getHeight());
                        // Send frequent SYNC updates (every 0.1 second).
                        if (ownerId != null && ownerId.equals(GameContext.getSelectedGameObjectId())) {
                            if (now - lastSyncTime >= 100_000_000) {
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
        if (ownerId != null && ownerId.equals(GameContext.getSelectedGameObjectId())) {
            if (now - lastSyncTime >= 1_000_000_000) { // every 1 second when not grabbed
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

    // Bounding box methods.
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
