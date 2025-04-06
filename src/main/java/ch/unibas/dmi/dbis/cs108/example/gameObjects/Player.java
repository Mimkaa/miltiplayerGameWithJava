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

    // For time-based updates.
    private long lastUpdateTime;

    // Fields for throwing mode.
    private boolean isThrowing = false;  // Are we in throwing mode?
    private float throwAngle = 90f;        // In degrees; default is upward.
    private boolean fPreviouslyPressed = false; // For edge detection on F key.

    // For edge detection on grab key E.
    private boolean ePreviouslyPressed = false;

    // Throw vector fields (will be computed when throwing)
    // (We compute these on F key release.)

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

    // -------------------------
    // Modular Keyboard Input Methods
    // -------------------------
    private void handleJumpInput() {
        if (KeyboardState.isKeyPressed(KeyCode.W) && onGround && canJump) {
            vy = -jumpingImpulse;
            onGround = false;
            Message jumpMsg = new Message("JUMP", new Object[]{vy}, null);
            sendMessage(jumpMsg);
        }
    }

    private void handleMovementInput() {
        if (KeyboardState.isKeyPressed(KeyCode.A)) {
            setX(getX() - speed);
        }
        if (KeyboardState.isKeyPressed(KeyCode.D)) {
            setX(getX() + speed);
        }
    }

    private void handleGrabInput() {
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;
        boolean ePressed = KeyboardState.isKeyPressed(KeyCode.E);
        if (ePressed && !ePreviouslyPressed) {
            // On edge: attempt to grab any nearby grabbable object.
            for (GameObject go : currentGame.getGameObjects()) {
                if (go instanceof IGrabbable) {
                    IGrabbable grabbable = (IGrabbable) go;
                    if (!grabbable.isGrabbed() && this.intersects(go)) {
                        grabbable.onGrab(this.getId());
                    }
                }
            }
        } else if (!ePressed && ePreviouslyPressed) {
            // On release of E: release any object grabbed by this player.
            for (GameObject go : currentGame.getGameObjects()) {
                if (go instanceof IGrabbable) {
                    IGrabbable grabbable = (IGrabbable) go;
                    if (grabbable.isGrabbed() && this.getId().equals(grabbable.getGrabbedBy())) {
                        grabbable.onRelease();
                    }
                }
            }
        }
        ePreviouslyPressed = ePressed;
    }

    private void handleThrowInput() {
        // When F is pressed, enter throwing mode.
        boolean fPressed = KeyboardState.isKeyPressed(KeyCode.F);
        if (fPressed && !fPreviouslyPressed) {
            // Start throwing mode.
            isThrowing = true;
            throwAngle = 90f; // default upward.
        }
        if (isThrowing) {
            // Adjust the throw angle with left/right arrow keys.
            if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
                throwAngle -= 2; // adjust by 2 degrees per frame (tweak as needed)
            }
            if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
                throwAngle += 2;
            }
            // Optional: clamp the angle (for example, between 30 and 150 degrees).
            if (throwAngle < 30) throwAngle = 30;
            if (throwAngle > 150) throwAngle = 150;
        }
        // When F is released, perform the throw.
        if (!fPressed && fPreviouslyPressed && isThrowing) {
            // Determine the throw vector.
            float magnitude = 400f; // adjust force magnitude as needed
            double rad = Math.toRadians(throwAngle);
            float throwVx = (float) (magnitude * Math.cos(rad));
            float throwVy = (float) (magnitude * Math.sin(rad));
            // Adjust for coordinate system (if y increases downward, a throw upward is negative vy)
            throwVy = -throwVy;
            // Now, throw any grabbed throwable object.
            Game currentGame = GameContext.getGameById(getGameId());
            if (currentGame != null) {
                for (GameObject go : currentGame.getGameObjects()) {
                    if (go instanceof IThrowable) {
                        IThrowable throwable = (IThrowable) go;
                        if (throwable.isGrabbed() && this.getId().equals(throwable.getGrabbedBy())) {
                            throwable.throwObject(throwVx, throwVy);
                        }
                    }
                }
            }
            isThrowing = false;
        }
        fPreviouslyPressed = fPressed;
    }

    /**
     * Combines all keyboard input handling.
     */
    private void handleKeyboardInput() {
        handleJumpInput();
        handleMovementInput();
        handleGrabInput();
        handleThrowInput();
    }

    // --- Update Method ---
    @Override
    public void myUpdateLocal(float deltaTime) {
        // Only process local input if this player is the selected (controlling) object.
        if (!this.getId().equals(GameContext.getSelectedGameObjectId())) {
            // Not controlled locally – only update physics and sync position.
            applyGravity(deltaTime);
            checkGroundCollision();
            checkIfSomeoneOnTop();
            return;
        }

        // Controlled player: process input normally.
        float oldX = getX();
        applyGravity(deltaTime);
        checkGroundCollision();
        checkIfSomeoneOnTop();
        handleKeyboardInput();  // your modular input methods (jump, move, grab, throw, etc.)

        if (getX() != oldX) {
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
        // Draw the player rectangle.
        gc.setFill(collisionDetected ? Color.GREEN : Color.BLUE);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());
        // Draw the player name.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
        // If in throwing mode and this is the controlled player, draw the throw arrow.
        if (isThrowing && this.getId().equals(GameContext.getSelectedGameObjectId())) {
            double centerX = getX() + getWidth() / 2.0;
            double centerY = getY() + getHeight() / 2.0;
            double arrowLength = 50; // length of arrow in pixels
            double rad = Math.toRadians(throwAngle);
            double endX = centerX + arrowLength * Math.cos(rad);
            double endY = centerY - arrowLength * Math.sin(rad);
            gc.setStroke(Color.RED);
            gc.strokeLine(centerX, centerY, endX, endY);
        }
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
