package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;

/**
 * A minimal concrete {@code GameObject} implementation for unit tests.
 * <p>
 * All abstract methods are stubbed out so that this object can be added to a {@link ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game}
 * without dragging in the full game-loop, rendering, or networking machinery.
 * </p>
 */
public class StubGameObject extends GameObject {
    /** Tracks whether {@link #myUpdateGlobal(Message)} was ever called. */
    boolean globalUpdated = false;

    private float x, y, w, h;

    /**
     * Constructs a stub with the given name and game ID.
     * This is used primarily for unit testing purposes.
     *
     * @param name the object’s display name
     * @param gameId the ID of the game this object belongs to
     */
    StubGameObject(String name, String gameId) {
        super(name, gameId);
    }
    // ─── Boundaries ─────────────────────────────────────────────────────────

    /**
     * Returns the X position of the object.
     *
     * @return the X position of the object
     */
    @Override public float getX()               { return x; }

    /**
     * Returns the Y position of the object.
     *
     * @return the Y position of the object
     */
    @Override public float getY()               { return y; }

    /**
     * Returns the width of the object.
     *
     * @return the width of the object
     */
    @Override public float getWidth()           { return w; }

    /**
     * Returns the height of the object.
     *
     * @return the height of the object
     */
    @Override public float getHeight()          { return h; }

    /**
     * Sets the X position of the object.
     *
     * @param x the new X position of the object
     */
    @Override public void  setX(float x)        { this.x = x; }

    /**
     * Sets the Y position of the object.
     *
     * @param y the new Y position of the object
     */
    @Override public void  setY(float y)        { this.y = y; }

    /**
     * Sets the width of the object.
     *
     * @param width the new width of the object
     */
    @Override public void  setWidth(float width){ this.w = width; }

    /**
     * Sets the height of the object.
     *
     * @param h the new height of the object
     */
    @Override public void  setHeight(float h)   { this.h = h; }

    // ─── Lifecycle & Rendering ─────────────────────────────────────────────

    /**
     * Updates the object locally (no-op in this stub).
     */
    @Override public void myUpdateLocal() { /* no-op */ }

    /**
     * Updates the object locally with a delta time (no-op in this stub).
     *
     * @param dt the delta time
     */
    @Override public void myUpdateLocal(float dt) { /* no-op */ }


    /**
     * Draws the object on the given {@code GraphicsContext} (no-op in this stub).
     *
     * @param gc the GraphicsContext used to draw the object
     */
    @Override
    public void draw(GraphicsContext gc) {/* no op */ }

    /**
     * Updates the object globally based on the given message. Sets the {@code globalUpdated} flag to true.
     *
     * @param msg the message that triggers the global update
     */
    @Override protected void myUpdateGlobal(Message msg){ globalUpdated = true; }

    // ─── Construction metadata ──────────────────────────────────────────────

    /**
     * Returns the constructor parameters used to create this object.
     * In this case, it returns an empty array.
     *
     * @return an empty array of parameters
     */
    @Override public Object[] getConstructorParamValues(){ return new Object[0]; }

    /**
     * Creates a snapshot of the object.
     *
     * @return null since this is a stub and doesn't require a snapshot
     */
    @Override public Message  createSnapshot(){ return null; }
}
