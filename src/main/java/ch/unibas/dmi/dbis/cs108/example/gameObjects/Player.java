package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Player extends GameObject implements IGravityAffected {

    private float x;
    private float y;
    private float width;
    private float height;

    private float speed = 5.0f;
    private float vy = 0.0f;
    private float mass = 70.0f;

    private boolean onGround = false;
    private boolean collisionDetected = false;
    private boolean canJump = true;

    // Jump impulse (tuned for your game)
    private float jumpingImpulse = 900f;

    // For time–based updates.
    private long lastUpdateTime;

    private boolean ePreviouslyPressed = false;

    public Player(String name, double x, double y, double side, String gameId) {
        this(name, (float) x, (float) y, (float) side, (float) side, gameId);
        this.lastUpdateTime = System.nanoTime();
    }

    public Player(String name, float x, float y, float width, float height, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.lastUpdateTime = System.nanoTime();
    }

    // --- Local Gravity & Collision ---

    @Override
    public void applyGravity(float deltaTime) {
        if (!onGround) {
            vy += GravityEngine.GRAVITY * deltaTime;
            setY(getY() + vy * deltaTime);
        }
    }

    private void checkGroundCollision() {
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;

        final float tolerance = 10.0f;
        float bottom = getY() + getHeight();
        onGround = false;

        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;

            float otherTop = other.getY();
            float myLeft = getX();
            float myRight = getX() + getWidth();
            float otherLeft = other.getX();
            float otherRight = other.getX() + other.getWidth();
            boolean horizontalOverlap = !(myRight <= otherLeft || myLeft >= otherRight);

            if (vy >= 0 && horizontalOverlap &&
                    bottom >= otherTop - tolerance && bottom <= otherTop + tolerance) {
                setY(otherTop - getHeight());
                vy = 0;
                onGround = true;
                break;
            }
        }
    }

    private void checkIfSomeoneOnTop() {
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;

        final float tolerance = 10.0f;
        float myTop = getY();
        canJump = onGround;

        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !(other instanceof Player)) continue;

            float otherBottom = other.getY() + other.getHeight();
            if (otherBottom >= myTop - tolerance && otherBottom <= myTop + tolerance) {
                canJump = false;
                break;
            }
        }
    }

    /**
     * Handles keyboard input.
     * <p>
     * If the E key is pressed and the player is colliding with any IGrabbable object,
     * call that object's onGrab(this.getId()). Otherwise, if E is not pressed,
     * for any IGrabbable object that is grabbed by this player, call onRelease().
     * </p>
     */
    private void handleKeyboardInput() {
        // Ensure this is the controlled player.
        if (!this.getId().equals(GameContext.getSelectedGameObjectId())) {
            return;
        }

        // Jumping.
        if (KeyboardState.isKeyPressed(KeyCode.W) && onGround && canJump) {
            vy = -jumpingImpulse;
            onGround = false;
            Message jumpMsg = new Message("JUMP", new Object[]{vy}, null);
            sendMessage(jumpMsg);
        }

        // Horizontal movement.
        if (KeyboardState.isKeyPressed(KeyCode.A)) {
            setX(getX() - speed);
        }
        if (KeyboardState.isKeyPressed(KeyCode.D)) {
            setX(getX() + speed);
        }

        // Determine current state of E key.
        boolean ePressed = KeyboardState.isKeyPressed(KeyCode.E);

        Game currentGame = GameContext.getGameById(this.getGameId());
        if (currentGame == null) return;

        // If E key has just been pressed, attempt to grab nearby grabbable objects.
        if (ePressed && !ePreviouslyPressed) {
            for (GameObject go : currentGame.getGameObjects()) {
                if (go instanceof IGrabbable) {
                    IGrabbable grabbable = (IGrabbable) go;
                    if (!grabbable.isGrabbed() && this.intersects(go)) {
                        grabbable.onGrab(this.getId());
                    }
                }
            }
        }
        // If E key has just been released, release objects grabbed by this player.
        else if (!ePressed && ePreviouslyPressed) {
            for (GameObject go : currentGame.getGameObjects()) {
                if (go instanceof IGrabbable) {
                    IGrabbable grabbable = (IGrabbable) go;
                    if (grabbable.isGrabbed() && this.getId().equals(grabbable.getGrabbedBy())) {
                        grabbable.onRelease();
                    }
                }
            }
        }

        // Update the previous E key state for the next frame.
        ePreviouslyPressed = ePressed;
    }

    // --- Update Method ---

    @Override
    public void myUpdateLocal(float deltaTime) {
        float oldX = getX();
        // Update vertical position based on gravity and collisions.
        applyGravity(deltaTime);
        checkGroundCollision();
        checkIfSomeoneOnTop();

        // Process keyboard input.
        handleKeyboardInput();

        // Send MOVE message only for horizontal movement.
        if (getX() != oldX) {
            long now = System.nanoTime();
            Message moveMsg = new Message("MOVE", new Object[]{getX()}, null);
            sendMessage(moveMsg);
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used; we always use myUpdateLocal(deltaTime)
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 1) {
                float newX = Float.parseFloat(params[0].toString());
                synchronized (this) {
                    setX(newX);
                }
                System.out.println("Processed MOVE for " + getName() + ": new position x=" + newX);
            }
        } else if ("JUMP".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 1) {
                float newVy = Float.parseFloat(params[0].toString());
                synchronized (this) {
                    vy = newVy;
                }
                System.out.println("Processed JUMP for " + getName() + ": new velocity vy=" + newVy);
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(collisionDetected ? Color.GREEN : Color.BLUE);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{getName(), getX(), getY(), getWidth(), getHeight(), getGameId()};
    }

    // --- Bounding Box Methods ---

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

    @Override
    public float getMass() {
        return mass;
    }
}
