package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class FallingObject extends GameObject implements IGravityAffected {

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

    // Ground state and attached platform.
    private boolean onGround = false;
    private GameObject attachedPlatform = null;
    private float attachedPlatformPrevX = 0;
    private float attachedPlatformPrevY = 0;

    /**
     * Constructs a FallingObject.
     *
     * @param name   The object's name.
     * @param x      Starting x coordinate.
     * @param y      Starting y coordinate.
     * @param width  Object width.
     * @param height Object height.
     * @param mass   Mass of the object.
     * @param gameId The game session ID.
     */
    public FallingObject(String name, float x, float y, float width, float height, float mass, String gameId) {
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
     * Applies gravity by updating vertical velocity and position.
     * Gravity is applied only if the object is not already on the ground.
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
     * Resolves collisions with collidable objects in the current game.
     * If the falling object’s bottom is within a small tolerance of another object’s top (and is falling),
     * it becomes attached to that platform.
     */
    private void resolveCollisions() {
        // Retrieve the current game session using this object's gameId.
        Game currentGame = GameContext.getGameById(this.getGameId());
        if (currentGame == null) return;

        boolean foundGround = false;
        final float tolerance = 5.0f;
        GameObject groundCandidate = null;

        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;

            if (this.intersects(other)) {
                float thisBottom = this.getY() + this.getHeight();
                float otherTop = other.getY();

                // Check if collision is from above (within tolerance) and object is falling
                if (vy >= 0 && (thisBottom >= otherTop && thisBottom <= otherTop + tolerance)) {
                    foundGround = true;
                    groundCandidate = other;
                    break;
                }
            }
        }

        if (foundGround && groundCandidate != null) {
            // Snap this object so that its bottom aligns with the top of the ground candidate.
            float resolvedY = groundCandidate.getY() - this.getHeight();
            setY(resolvedY);
            y = resolvedY;
            vy = 0;
            onGround = true;

            // If attaching to a new platform, store its current x position.
            if (attachedPlatform != groundCandidate) {
                attachedPlatform = groundCandidate;
                attachedPlatformPrevX = groundCandidate.getX();
                attachedPlatformPrevY = groundCandidate.getY();
            }
        } else {
            // No valid ground detected. Detach from any platform.
            onGround = false;
            attachedPlatform = null;
        }
    }

    /**
     * If attached to a moving platform, update the falling object's position to follow it.
     * We check if the falling object's bottom is still close enough to the platform's top.
     * If yes, we move the object horizontally by the platform's movement delta.
     * If not, we detach the object.
     */
    private void updateWithAttachedPlatform() {
        if (onGround && attachedPlatform != null) {
            // Berechne den horizontalen Versatz der Plattform seit dem letzten Frame.
            float deltaX = attachedPlatform.getX() - attachedPlatformPrevX;
            // Aktualisiere die x-Position des FallingObjects.
            setX(getX() + deltaX);
            // Optional: Falls gewünscht, kann man auch die y-Position anpassen, sodass das Objekt immer
            // direkt auf der Plattform „klebt“. Für viele Plattformer ist das sinnvoll:
            setY(attachedPlatform.getY() - getHeight());
            // Aktualisiere den gespeicherten Wert der Plattformposition für den nächsten Frame.
            attachedPlatformPrevX = attachedPlatform.getX();
            attachedPlatformPrevY = attachedPlatform.getY();
        }
    }

    /**
     * Local update method called every frame.
     * It computes deltaTime, applies gravity (if not attached), resolves collisions,
     * and, if attached, updates the object's position with the moving platform.
     */
    @Override
    public void myUpdateLocal() {
        long now = System.nanoTime();
        float deltaTime = (now - lastUpdateNano) / 1_000_000_000f;
        lastUpdateNano = now;

        // If not attached to a platform, apply gravity.
        if (!onGround) {
            applyGravity(deltaTime);
        }

        // Check collisions and update ground state (which may update attachedPlatform).
        resolveCollisions();

        // If attached, follow the platform's movement.
        updateWithAttachedPlatform();
    }

    @Override
    public void myUpdateLocal(float deltaTime) {

    }

    /**
     * Local update method called every frame.
     * It computes deltaTime, applies gravity (if not grounded), resolves collisions,
     * and if grounded to a moving platform, updates position accordingly.
     */

    @Override
    protected void myUpdateGlobal(Message msg) {
        // Process remote updates if needed.
    }

    /**
     * Draws the falling object as a red rectangle with its name above it.
     */
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
        return new Object[]{ getName(), x, y, width, height, mass, getGameId() };
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
