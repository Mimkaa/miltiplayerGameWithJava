package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import java.util.Arrays;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

/**
 * A JavaFX-based translation of the Pygame Player snippet:
 *   - position, velocity, acceleration
 *   - friction
 *   - horizontal movement with left/right
 *   - screen-wrapping on X-axis
 */
@Getter
@Setter
public class Player2 extends GameObject {

    // ---------------------------------
    // Constants matching the Python snippet
    // ---------------------------------
    private static final float PLAYER_ACC = 1.5f;      // Acceleration magnitude when pressing left/right
    private static final float PLAYER_FRICTION = -0.12f; // Negative for friction (slowing down)
    private static final float JUMP_FORCE = -15;
    private static final float SCREEN_WIDTH = 800;
    private static final float SCREEN_HEIGHT = 600; // Height is stored even though vertical wrap isn't used

    // ---------------------------------
    // Position, Velocity, Acceleration
    // ---------------------------------
    private Vector2 pos;      // Current position
    private Vector2 vel;      // Velocity
    private Vector2 acc;      // Acceleration

    // (Optional) Store previous position if you want to compare changes later.
    private Vector2 prevPos;

    // ---------------------------------
    // Rendering dimensions
    // ---------------------------------
    private float width;
    private float height;

    // ---------------------------------
    // Jump management flags
    // ---------------------------------
    private boolean jumped = false;
    // This flag can be used later if you need to detect key state changes.
    private boolean jumpKeyPressed = false;

    // ---------------------------------
    // Timer for sending MOVE messages at fixed intervals.
    // ---------------------------------
    private float moveMsgTimer = 0;

    // Ground collision flag.
    private boolean onGround = false;

    /**
     * Simple constructor: place player in the middle of the screen with a fixed size.
     */
    public Player2(String name, String gameId) {
        this(name, SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 30, 40, gameId);
    }

    /**
     * More flexible constructor if you want to pass different positions or sizes.
     */
    public Player2(String name, float x, float y, float w, float h, String gameId) {
        super(name, gameId);
        this.pos = new Vector2(x, y);
        this.vel = new Vector2(0, 0);
        this.acc = new Vector2(0, 0);
        this.prevPos = new Vector2(x, y);  // Initialize previous position to the starting position.
        this.width = w;
        this.height = h;
        setCollidable(true);
        setMovable(true);
    }

    // ---------------------------------
    // Process keyboard input (update acceleration) but do NOT send MOVE message here.
    // ---------------------------------
    private void updateFromKeyboardInput() {
        // If left arrow is pressed:
        if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
            acc.x = -PLAYER_ACC;
        }

        // If right arrow is pressed:
        if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
            acc.x = PLAYER_ACC;
        }

        // Process jump (up arrow) input:
        if (KeyboardState.isKeyPressed(KeyCode.UP)) {
            if (!jumped && onGround) {  // Allow jump as long as the player is on ground.
                vel.y += JUMP_FORCE;
                jumped = true;
            }
        }
    }

    @Override
    public void myUpdateLocal(float deltaTime) {
        if (isSelected()) {
            // Optionally process keyboard input when selected.
            // updateFromKeyboardInput();
        }
        // 1) Reset acceleration each frame to (0,0) and apply gravity on the y-axis.
        acc.y = 0.5f;

        // 2) Apply friction: acceleration is modified by the x component of velocity.
        acc.x += vel.x * PLAYER_FRICTION;

        // 3) Equations of motion: update velocity then update position.
        vel.x += acc.x;
        vel.y += acc.y;
        pos.x += vel.x + 0.5f * acc.x;
        pos.y += vel.y + 0.5f * acc.y;

        // 4) (Optional) Wrap around the screen horizontally.
        // if (pos.x > SCREEN_WIDTH) { pos.x = 0; }
        // else if (pos.x < 0) { pos.x = SCREEN_WIDTH; }

        // 5) Check for collision with a Platform (or any collidable object) to reset the jump flag.
        if (jumped && getParentGame() != null) {
            for (GameObject obj : getParentGame().getGameObjects()) {
                // Here we only check for collision with, say, Platforms (or any desired objects)
                // that allow you to reset the jump flag.
                if ((obj instanceof Platform || obj instanceof Player2) && this.intersects(obj)) {
                    jumped = false;
                    break;
                }
            }
        }
        acc.x = 0;

        // Update ground collision status.
        checkGroundCollision();
        // We no longer need to check for someone on top.
    }

    /**
     * We are not using this method in this snippet.
     */
    @Override
    public void myUpdateLocal() { }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("KEY_PRESS".equals(msg.getMessageType())) {
            // Expecting one parameter: the name of the key (e.g., "LEFT", "RIGHT", "UP")
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 1) {
                String keyString = params[0].toString();
                // React exactly as in updateFromKeyboardInput()
                if (KeyCode.LEFT.toString().equals(keyString)) {
                    acc.x += -PLAYER_ACC;
                } else if (KeyCode.RIGHT.toString().equals(keyString)) {
                    acc.x += PLAYER_ACC;
                } else if (KeyCode.UP.toString().equals(keyString)) {
                    if (!jumped && onGround) {
                        vel.y += JUMP_FORCE;
                        jumped = true;
                    }
                }
                System.out.println("Processed KEY_PRESS for " + getName() + ": " + keyString);
            }
        } else if ("MOVE".equals(msg.getMessageType())) {
            // Expecting two parameters: the new position (e.g., x and y coordinates).
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 2) {
                try {
                    float newX = Float.parseFloat(params[0].toString());
                    float newY = Float.parseFloat(params[1].toString());
                    // Set the player's position.
                    pos.x = newX;
                    pos.y = newY;
                    System.out.println("Processed MOVE for " + getName() + ": pos=(" + newX + ", " + newY + ")");
                } catch (NumberFormatException ex) {
                    System.out.println("Error processing MOVE message parameters: " + Arrays.toString(params));
                }
            }
        }
    }

    /**
     * Checks if this player is "on the ground" by testing collision below (with a tolerance).
     * In this version, any collidable object (platforms, players, etc.) supporting the player
     * will set the onGround flag to true.
     */
    private void checkGroundCollision() {
        Game currentGame = getParentGame();
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
            if (vel.y >= 0 && horizontalOverlap && bottom >= otherTop - tolerance && bottom <= otherTop + tolerance) {
                // Align this player's bottom with the supporting object's top.
                setY(otherTop - getHeight());
                vel.y = 0;
                onGround = true;
                break;
            }
        }
    }

    // ---------------------------------
    // Drawing the rectangle & name.
    // ---------------------------------
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        gc.fillRect(pos.x, pos.y, width, height);

        // Draw player name above the rectangle.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), pos.x + width / 2 - textWidth / 2, pos.y - 5);
    }

    // ---------------------------------
    // Required methods from GameObject.
    // ---------------------------------
    @Override
    public float getX() {
        return pos.x;
    }

    @Override
    public float getY() {
        return pos.y;
    }

    @Override
    public void setX(float x) {
        pos.x = x;
    }

    @Override
    public void setY(float y) {
        pos.y = y;
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
    public void setWidth(float width) {
        this.width = width;
    }

    @Override
    public void setHeight(float height) {
        this.height = height;
    }

    // For reflection-based object creation:
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), pos.x, pos.y, width, height, getGameId() };
    }

    // ---------------------------------
    // Simple Vector2 class.
    // ---------------------------------
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

        public void set(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
