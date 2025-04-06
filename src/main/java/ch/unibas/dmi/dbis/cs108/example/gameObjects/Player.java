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

import java.util.ArrayList;
import java.util.List;

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


    // --- Control Splitting ---
    // A list of client IDs that are controlling this player.
    // When only one client controls it, that client handles all keys.
    // When two are controlling it, the first handles movement/throwing and the second handles jump/grab.
    private List<String> controllingClients = new ArrayList<>();

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
    // Modular Keyboard Input Methods for Control Splitting
    // -------------------------
    // In the following methods, we check the list of controllingClients.
    // We'll assume GameContext.getLocalClientId() returns the local client’s identifier.
    private void handleJumpInput() {
        // Only process jump if this client is either the sole controller or is assigned the "jump/grab" role.
        if (controllingClients.size() == 0 ||
                (controllingClients.size() == 1 && controllingClients.get(0).equals(GameContext.getLocalClientId())) ||
                (controllingClients.size() >= 2 && controllingClients.get(1).equals(GameContext.getLocalClientId()))) {
            if (KeyboardState.isKeyPressed(KeyCode.W) && onGround && canJump) {
                vy = -jumpingImpulse;
                onGround = false;
                Message jumpMsg = new Message("JUMP", new Object[]{vy}, null);
                sendMessage(jumpMsg);
            }
        }
    }

    private void handleMovementInput() {
        // Process horizontal movement if this client is the sole controller or the "movement/throw" role.
        if (controllingClients.size() == 0 ||
                (controllingClients.size() == 1 && controllingClients.get(0).equals(GameContext.getLocalClientId())) ||
                (controllingClients.size() >= 2 && controllingClients.get(0).equals(GameContext.getLocalClientId()))) {
            if (KeyboardState.isKeyPressed(KeyCode.A)) {
                setX(getX() - speed);
            }
            if (KeyboardState.isKeyPressed(KeyCode.D)) {
                setX(getX() + speed);
            }
        }
    }


    private void handleGrabInput() {
        // Grab/release is handled by the "jump/grab" role.
        if (controllingClients.size() == 0 ||
                (controllingClients.size() == 1 && controllingClients.get(0).equals(GameContext.getLocalClientId())) ||
                (controllingClients.size() >= 2 && controllingClients.get(1).equals(GameContext.getLocalClientId()))) {
            Game currentGame = GameContext.getGameById(getGameId());
            if (currentGame == null) return;
            // We'll use the E key to grab/release.
            if (KeyboardState.isKeyPressed(KeyCode.E)) {
                for (GameObject go : currentGame.getGameObjects()) {
                    if (go instanceof IGrabbable) {
                        IGrabbable grabbable = (IGrabbable) go;
                        if (!grabbable.isGrabbed() && this.intersects(go)) {
                            grabbable.onGrab(this.getId());
                        }
                    }
                }
            } else {
                for (GameObject go : currentGame.getGameObjects()) {
                    if (go instanceof IGrabbable) {
                        IGrabbable grabbable = (IGrabbable) go;
                        if (grabbable.isGrabbed() && this.getId().equals(grabbable.getGrabbedBy())) {
                            grabbable.onRelease();
                        }
                    }
                }
            }
        }
    }

    private void handleThrowInput() {
        // The "movement/throw" role handles throw input.
        if (controllingClients.size() == 0 ||
                (controllingClients.size() == 1 && controllingClients.get(0).equals(GameContext.getLocalClientId())) ||
                (controllingClients.size() >= 2 && controllingClients.get(0).equals(GameContext.getLocalClientId()))) {
            // When F is pressed, enter throwing mode.
            // (Assume we have fields like isThrowing, throwAngle, etc.)
            // The code here remains as in your previous implementation.
            // For brevity, see the earlier modular implementation of handleThrowInput().
            // ...
        }
    }

    private void handleThrowInputDetailed() {
        // (This method is similar to our previous handleThrowInput, using edge detection.)
        boolean fPressed = KeyboardState.isKeyPressed(KeyCode.F);
        // For simplicity, assume we always use throwAngle and adjust with LEFT/RIGHT.
        if (fPressed) {
            isThrowing = true;
            // Adjust throwAngle.
            if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
                throwAngle -= 2;
            }
            if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
                throwAngle += 2;
            }
            if (throwAngle < 30) throwAngle = 30;
            if (throwAngle > 150) throwAngle = 150;
        }
        if (!fPressed && isThrowing) {
            // On F release, determine throw vector.
            float magnitude = 400f; // adjust as needed
            double rad = Math.toRadians(throwAngle);
            float throwVx = (float) (magnitude * Math.cos(rad));
            float throwVy = (float) (magnitude * Math.sin(rad));
            // Adjust for coordinate system (if needed)
            throwVy = -throwVy;
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
    }

    private void handleKeyboardInput() {
        // Depending on the number of controllers and our local client ID, we call the appropriate methods.
        handleJumpInput();
        handleMovementInput();
        handleGrabInput();
        handleThrowInputDetailed();
    }

    // --- Update Method ---
    @Override
    public void myUpdateLocal(float deltaTime) {
        float oldX = getX();
        applyGravity(deltaTime);
        checkGroundCollision();
        checkIfSomeoneOnTop();
        handleKeyboardInput();
        if (getX() != oldX) {
            Message moveMsg = new Message("MOVE", new Object[]{getX()}, null);
            sendMessage(moveMsg);
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used.
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
    public float getMass() { return mass; }
}
