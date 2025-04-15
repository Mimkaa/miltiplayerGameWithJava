package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;

/**
 * A Player that has no collision handling and uses Vector2 for position and velocity.
 * The update follows basic physics:
 *   velocity += acceleration * dt   (gravity)
 *   position += velocity * dt
 */
@Getter
@Setter
public class Player extends GameObject implements IGravityAffected {

    // Position & velocity in 2D
    private Vector2 position;
    private Vector2 velocity;

    // Dimensions of the Player (still stored as floats)
    private float width;
    private float height;

    // Horizontal speed when pressing A/D
    private float speed = 5.0f;
    // Mass (unused except for interface reference)
    private float mass = 70.0f;

    // Gravity constant (downward)
    private static final float GRAVITY = 50f;

    // Jump impulse
    private float jumpingImpulse = 40f;

    // Throwing / Grabbing
    private boolean isThrowing = false;
    private float throwAngle = 90f;    
    private boolean fPreviouslyPressed = false; // for edge detection on 'F'
    private boolean ePreviouslyPressed = false; // for edge detection on 'E'

    // For time-based updates if needed
    private long lastUpdateTime;

    /**
     * Constructs a Player at (x, y) with a default square size (side x side).
     */
    public Player(String name, double x, double y, double side, String gameId) {
        this(name, (float) x, (float) y, (float) side, (float) side, gameId);
    }

    /**
     * Constructs a Player at (x, y) with custom width & height.
     */
    public Player(String name, float x, float y, float width, float height, String gameId) {
        super(name, gameId);
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(0, 0);
        this.width = width;
        this.height = height;
        this.lastUpdateTime = System.nanoTime();
        setCollidable(true);
        setMovable(true);
    }

    // ---------------------------------
    // Gravity & Movement (no collisions)
    // ---------------------------------

    /**
     * Physics step for gravity:
     *   velocity += (gravity * dt)
     *   position += (velocity * dt)
     */
    @Override
    public void applyGravity(float deltaTime) {
        // Add gravity to velocity (gravity is downward, so +y)
        velocity.y += GRAVITY * deltaTime;
        // Then add velocity to position
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;
    }

    /**
     * Jump anytime W is pressed (no ground checks).
     */
    private void handleJumpInput() {
        if (KeyboardState.isKeyPressed(KeyCode.W)) {
            // Impulse: upward = negative y direction
            velocity.y = -jumpingImpulse;
            // Send a "JUMP" message for synchronization
            Message jumpMsg = new Message("JUMP", new Object[]{ velocity.y }, null);
            sendMessage(jumpMsg);
        }
    }

    /**
     * Move left/right with A/D, send a "MOVE" message if position changes.
     * In this example, we directly alter the player's x-position by speed each frame,
     * rather than adjusting velocity.x. (You can do it either way.)
     */
    private void handleMovementInput() {
        float oldX = position.x;

        if (KeyboardState.isKeyPressed(KeyCode.A)) {
            position.x -= speed;
        }
        if (KeyboardState.isKeyPressed(KeyCode.D)) {
            position.x += speed;
        }

        // If we moved horizontally, send a MOVE message
        if (position.x != oldX) {
            Message moveMsg = new Message("MOVE", new Object[]{ position.x }, null);
            sendMessage(moveMsg);
        }
    }

    /**
     * Grab or release objects with E (edge-detect).
     * (No collision checks here.)
     */
    private void handleGrabInput() {
        // Use the parent game instead of GameContext.
        Game currentGame = getParentGame();
        if (currentGame == null) return;

        boolean ePressed = KeyboardState.isKeyPressed(KeyCode.E);
        if (ePressed && !ePreviouslyPressed) {
            // On press: attempt to grab
            for (GameObject go : currentGame.getGameObjects()) {
                if (go instanceof IGrabbable) {
                    IGrabbable grabbable = (IGrabbable) go;
                    if (!grabbable.isGrabbed()) {
                        grabbable.onGrab(this.getId());
                    }
                }
            }
        }
        else if (!ePressed && ePreviouslyPressed) {
            // On release, if we are the grabber
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

    /**
     * Throw objects in "throwing mode" with F.
     * (No collision checks here.)
     */
    private void handleThrowInput() {
        boolean fPressed = KeyboardState.isKeyPressed(KeyCode.F);
        if (fPressed && !fPreviouslyPressed) {
            // Enter throwing mode
            isThrowing = true;
            throwAngle = 90f;
        }
        if (isThrowing) {
            // Adjust angle with LEFT/RIGHT
            if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
                throwAngle -= 2;
            }
            if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
                throwAngle += 2;
            }
            // Optional clamp
            if (throwAngle < 30) throwAngle = 30;
            if (throwAngle > 150) throwAngle = 150;
        }
        if (!fPressed && fPreviouslyPressed && isThrowing) {
            // Perform the throw
            float magnitude = 400f;
            double rad = Math.toRadians(throwAngle);
            float throwVx = (float) (magnitude * Math.cos(rad));
            float throwVy = (float) (magnitude * Math.sin(rad));
            // Flip vertical to account for downward positive
            throwVy = -throwVy;

            // Use the parent game reference instead of GameContext.
            Game currentGame = getParentGame();
            if (currentGame != null) {
                // Throw any object we are grabbing.
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
     * Consolidates all keyboard inputs in one call.
     */
    private void handleKeyboardInput() {
        handleMovementInput();
        handleJumpInput();
        handleGrabInput();
        handleThrowInput();
    }

    // ---------------------------------
    // Update Methods
    // ---------------------------------

    @Override
    public void myUpdateLocal(float deltaTime) {
        // Use parent game to determine selected object instead of GameContext.
        if (!this.getId().equals(getParentGame().getSelectedGameObjectId())) {
            // Not the locally controlled player: only apply gravity.
            applyGravity(deltaTime);
            return;
        }
        // Local player: apply gravity, then handle inputs.
        applyGravity(deltaTime);
        handleKeyboardInput();
    }

    @Override
    public void myUpdateLocal() {
        // Not used; we always do time-based update.
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 1) {
                float newX = Float.parseFloat(params[0].toString());
                position.x = newX;
                System.out.println("Processed MOVE for " + getName() + ": x=" + newX);
            }
        } 
        else if ("JUMP".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 1) {
                float newVy = Float.parseFloat(params[0].toString());
                velocity.y = newVy;
                System.out.println("Processed JUMP for " + getName() + ": vy=" + newVy);
            }
        }
    }

    // ---------------------------------
    // Rendering
    // ---------------------------------
    @Override
    public void draw(GraphicsContext gc) {
        // Always draw in blue (no collision feedback here)
        gc.setFill(Color.BLUE);
        gc.fillRect(position.x, position.y, width, height);

        // Draw player name above
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), position.x + width / 2 - textWidth / 2, position.y - 5);

        // If in throwing mode and this is the local player, draw the aim arrow.
        if (isThrowing && this.getId().equals(getParentGame().getSelectedGameObjectId())) {
            double centerX = position.x + width / 2.0;
            double centerY = position.y + height / 2.0;
            double arrowLength = 50;
            double rad = Math.toRadians(throwAngle);
            double endX = centerX + arrowLength * Math.cos(rad);
            double endY = centerY - arrowLength * Math.sin(rad);

            gc.setStroke(Color.RED);
            gc.strokeLine(centerX, centerY, endX, endY);
        }
    }

    // ---------------------------------
    // Object Construction
    // ---------------------------------
    @Override
    public Object[] getConstructorParamValues() {
        // Must match: (String name, float x, float y, float width, float height, String gameId)
        return new Object[]{
            getName(), position.x, position.y, width, height, getGameId()
        };
    }

    // ---------------------------------
    // IGravityAffected
    // ---------------------------------
    @Override
    public float getMass() {
        return mass;
    }

    /**
     * A simple 2D vector class for position, velocity, etc.
     */
    public static class Vector2 {
        public float x;
        public float y;

        public Vector2(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Vector2() {
            this(0, 0);
        }

        /** Adds another Vector2 to this Vector2. */
        public void add(Vector2 other) {
            this.x += other.x;
            this.y += other.y;
        }

        /** Adds scalar multiples of x and y to this Vector2. */
        public void add(float dx, float dy) {
            this.x += dx;
            this.y += dy;
        }

        /** Multiplies both components of this Vector2 by a scalar. */
        public void scl(float scalar) {
            this.x *= scalar;
            this.y *= scalar;
        }

        /** Sets this vector to the given x and y. */
        public void set(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
        
    }
    @Override
    public float getX() {
        return position.x;
    }

    @Override
    public float getY() {
        return position.y;
    }

    @Override
    public void setX(float x) {
        position.x = x;
    }

    @Override
    public void setY(float y) {
        position.y = y;
    }
    @Override
    public Message createSnapshot() {
        // Pack the position, velocity, and acceleration into an Object array.
        Object[] params = new Object[]{
            position.x, position.y,   // Position
            velocity.x, velocity.y,   // Velocity
            
        };
        // Create a new message with type "SNAPSHOT" and an appropriate option (e.g., "UPDATE").
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        
        // Set the concealed parameters so receivers know the source of the snapshot.
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        
        return snapshotMsg;
    }
    
}
