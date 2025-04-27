package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;

/**
 * A minimal concrete {@code GameObject} implementation for unit tests.
 * <p>
 * All abstract methods are stubbed out so that this object can be added to a {@link Game}
 * without dragging in the full game-loop, rendering, or networking machinery.
 * </p>
 */
public class StubGameObject extends GameObject {
    /** Tracks whether {@link #myUpdateGlobal(Message)} was ever called. */
    boolean globalUpdated = false;

    private float x, y, w, h;

    /**
     * Constructs a stub with the given name and game ID.
     * @param name the object’s display name
     * @param gameId the ID of the game this object belongs to
     */
    StubGameObject(String name, String gameId) {
        super(name, gameId);
    }
    // ─── Boundaries ─────────────────────────────────────────────────────────
    @Override public float getX()               { return x; }
    @Override public float getY()               { return y; }
    @Override public float getWidth()           { return w; }
    @Override public float getHeight()          { return h; }
    @Override public void  setX(float x)        { this.x = x; }
    @Override public void  setY(float y)        { this.y = y; }
    @Override public void  setWidth(float width){ this.w = width; }
    @Override public void  setHeight(float h)   { this.h = h; }

    // ─── Lifecycle & Rendering ─────────────────────────────────────────────
    @Override public void myUpdateLocal() { /* no-op */ }
    @Override public void myUpdateLocal(float dt) { /* no-op */ }


    @Override
    public void draw(GraphicsContext gc) {/* no op */ }
    @Override protected void myUpdateGlobal(Message msg){ globalUpdated = true; }

    // ─── Construction metadata ──────────────────────────────────────────────
    @Override public Object[] getConstructorParamValues(){ return new Object[0]; }
    @Override public Message  createSnapshot(){ return null; }
}
