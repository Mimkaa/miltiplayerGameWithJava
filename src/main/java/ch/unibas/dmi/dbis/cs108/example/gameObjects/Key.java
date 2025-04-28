package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Arrays;

public class Key extends GameObject implements IGravityAffected, IGrabbable, IThrowable {

    // Bounding box and physics fields.
    private float x, y, width, height;
    // Physics properties.
    private float mass;
    private float vx = 0.0f, vy = 0.0f;
    private float terminalVelocity = 200.0f;
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
        this.isGrabbed = true;
        this.grabbedBy = playerId;
        vx = 0;
        vy = 0;
        Game currentGame = getParentGame();
        if (currentGame != null) {
            for (GameObject obj : currentGame.getGameObjects()) {
                if (obj.getId().equals(playerId) && obj instanceof Player2) {
                    Player2 p = (Player2) obj;
                    setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                    setY(p.getY() - getHeight());
                    Message syncMsg = new Message("SYNC", new Object[]{getX(), getY()}, null);
                    sendMessage(syncMsg);
                    break;
                }
            }
        }
    }

    @Override
    public void setVelocity(float vx, float vy) {
        this.vx += vx * 4;
        this.vy += vy * 2;
    }

    public void setVelocityY(float vy) {
        this.vy += vy;
    }

    public float getVelocityX() {
        return this.vx;
    }

    public float getVelocityY() {
        return this.vy;
    }

    @Override
    public void onRelease() {
        this.isGrabbed = false;
        this.grabbedBy = null;
    }

    @Override
    public boolean isGrabbed() {
        return isGrabbed;
    }

    @Override
    public String getGrabbedBy() {
        return grabbedBy;
    }

    @Override
    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // -------------------------
    // IThrowable Implementation
    // -------------------------
    @Override
    public void throwObject(float throwVx, float throwVy) {
        vx = throwVx;
        vy = throwVy;
        onRelease();
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
            setY(y + vy * deltaTime);
            setX(x + vx * deltaTime);
        }
    }

    private void resolveCollisions() {
        Game currentGame = getParentGame();
        if (currentGame == null) return;
        final float tolerance = 5.0f;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;
            if (this.intersects(other)) {
                this.resolveCollision(other);
                if (other instanceof Player2 && ownerId == null) {
                    ownerId = other.getId();
                }
                if (Math.abs((getY() + getHeight()) - other.getY()) < tolerance && vy >= 0) {
                    onGround = true;
                }
            }
        }
    }

    @Override
    public void myUpdateLocal(float deltaTime) {
        long now = System.nanoTime();
        if (isGrabbed) {
            Game currentGame = getParentGame();
            if (currentGame != null) {
                for (GameObject obj : currentGame.getGameObjects()) {
                    if (obj.getId().equals(grabbedBy) && obj instanceof Player2) {
                        Player2 p = (Player2) obj;
                        setX(p.getX() + p.getWidth() / 2 - getWidth() / 2);
                        setY(p.getY() - getHeight());
                        return;
                    }
                }
            }
        }
        onGround = false;
        applyGravity(deltaTime);
        resolveCollisions();
        if (onGround) {
            vx *= frictionFactor;
            if (Math.abs(vx) < 1.0f) vx = 0;
        }
        if (ownerId != null && ownerId.equals(getParentGame().getSelectedGameObjectId())) {
            if (now - lastSyncTime >= 1_000_000_000L) {
                Message snapshot = createSnapshot();
                Server.getInstance().sendMessageBestEffort(snapshot);
                lastSyncTime = now;
            }
        }
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        switch (msg.getMessageType()) {
            case "SYNC":
                Object[] p = msg.getParameters();
                if (p.length >= 2) {
                    setX(Float.parseFloat(p[0].toString()));
                    setY(Float.parseFloat(p[1].toString()));
                }
                break;
            case "THROW":
                Object[] tp = msg.getParameters();
                if (tp.length >= 2) {
                    vx = Float.parseFloat(tp[0].toString());
                    vy = Float.parseFloat(tp[1].toString());
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.GOLD);
        gc.fillRect(x, y, width, height);
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), x + width / 2 - textWidth / 2, y - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{getName(), x, y, width, height, mass, getGameId()};
    }

    @Override
    public Message createSnapshot() {
        Object[] params = new Object[]{ x, y, vx, vy };
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        return snapshotMsg;
    }

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
    public void myUpdateLocal() {

    }
}
