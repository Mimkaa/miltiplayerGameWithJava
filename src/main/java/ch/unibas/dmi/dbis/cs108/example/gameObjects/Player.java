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
    private float jumpingImpulse = 600f;

    // For time–based updates.
    private long lastUpdateTime;

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
        // Only apply gravity if not on the ground.
        if (!onGround) {
            // Use our local GravityEngine constant.
            vy += GravityEngine.GRAVITY * deltaTime;
            setY(getY() + vy * deltaTime);
        }
    }

    private void checkGroundCollision() {
        // Retrieve the current game session using the static helper.
        Game currentGame = GameContext.getGameById(getGameId());
        if (currentGame == null) return;

        final float tolerance = 10.0f;
        float bottom = getY() + getHeight();
        onGround = false; // Reset onGround flag

        // Loop over game objects to find a valid ground candidate.
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;

            float otherTop = other.getY();

            // Check for horizontal overlap.
            float myLeft = getX();
            float myRight = getX() + getWidth();
            float otherLeft = other.getX();
            float otherRight = other.getX() + other.getWidth();
            boolean horizontalOverlap = !(myRight <= otherLeft || myLeft >= otherRight);

            // Adjusted condition: we allow a tolerance both above and below the platform top.
            if (vy >= 0 && horizontalOverlap &&
                    bottom >= otherTop - tolerance && bottom <= otherTop + tolerance) {
                // Snap the player's bottom to the platform's top.
                setY(otherTop - getHeight());
                vy = 0;
                onGround = true;
                break; // Stop after finding a valid ground candidate.
            }
        }
    }
    // --- Update Method ---

    @Override
    public void myUpdateLocal(float deltaTime) {
        float oldX = getX();
        float oldY = getY();

        // Apply gravity locally.
        applyGravity(deltaTime);

        // Check ground collisions.
        checkGroundCollision();

        // Handle input if this is the selected object.
        if (this.getId().equals(GameContext.getSelectedGameObjectId())) {
            if (KeyboardState.isKeyPressed(KeyCode.W) && onGround && canJump) {
                vy = -jumpingImpulse;
                onGround = false;
            }
            if (KeyboardState.isKeyPressed(KeyCode.A)) {
                setX(getX() - speed);
            }
            if (KeyboardState.isKeyPressed(KeyCode.D)) {
                setX(getX() + speed);
            }
        }

        // If position changed, send a MOVE message.
        if (getX() != oldX || getY() != oldY) {
            Message moveMsg = new Message("MOVE", new Object[]{getX(), getY()}, null);
            sendMessage(moveMsg);
        }
    }

    @Override
    public void myUpdateLocal() {
        // Not used (we always use myUpdateLocal(deltaTime))
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("Player MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                float newX = Float.parseFloat(params[0].toString());
                float newY = Float.parseFloat(params[1].toString());
                synchronized (this) {
                    setX(newX);
                    setY(newY);
                }
                System.out.println("Processed MOVE for " + getName() + ": new position x=" + newX + ", y=" + newY);
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw the player rectangle.
        gc.setFill(collisionDetected ? Color.GREEN : Color.BLUE);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());

        // Draw the player's name above.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth()/2 - textWidth/2, getY() - 5);
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
    public float getMass() {
        return mass;
    }
}
