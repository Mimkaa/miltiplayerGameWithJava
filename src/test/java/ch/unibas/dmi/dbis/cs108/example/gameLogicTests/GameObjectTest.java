package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Player2.Vector2;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the abstract {@link GameObject} base class.
 * <p>
 * We use a {@link StubGameObject} to exercise all the concrete behavior
 * implemented in GameObject:
 * <ul>
 *   <li>Positioning (setPos/getPos)</li>
 *   <li>Grab/release state</li>
 *   <li>Selected flag</li>
 *   <li>Collidable / movable flags</li>
 *   <li>Incoming message queue limit & processing</li>
 *   <li>Command queue processing</li>
 *   <li>Collision detection & resolution</li>
 *   <li>extractGameId helper</li>
 * </ul>
 * </p>
 */
class GameObjectTest {

    private StubGameObject stub;

    @BeforeEach
    void setUp() {
        stub = new StubGameObject("Alice", "game-42");
    }

    /**
     * Minimal concrete subclass of GameObject for testing.
     */
    static class StubGameObject extends GameObject {
        float x, y, w, h;
        boolean globalUpdated = false;
        boolean commandRan = false;

        StubGameObject(String name, String gameId) {
            super(name, gameId);
        }

        @Override public float getX()                   { return x; }
        @Override public float getY()                   { return y; }
        @Override public float getWidth()               { return w; }
        @Override public float getHeight()              { return h; }
        @Override public void setX(float x)             { this.x = x; }
        @Override public void setY(float y)             { this.y = y; }
        @Override public void setWidth(float width)     { this.w = width; }
        @Override public void setHeight(float height)   { this.h = height; }

        @Override protected void myUpdateGlobal(Message msg) { globalUpdated = true; }
        @Override public void myUpdateLocal()                  { /* no-op */ }
        @Override public void myUpdateLocal(float deltaTime)  { /* no-op */ }
        @Override public void draw(GraphicsContext gc)         { /* no-op */ }
        @Override public Object[] getConstructorParamValues()  { return new Object[0]; }
        @Override public Message createSnapshot()              { return null; }
    }

    
  
    @Test
    void testGrabRelease() {
        assertFalse(stub.isGrabbed(), "New object should not be grabbed");
        stub.onGrab("player123");
        assertTrue(stub.isGrabbed(), "onGrab() should set grabbed flag");
        stub.onRelease();
        assertFalse(stub.isGrabbed(), "onRelease() should clear grabbed flag");
    }

    @Test
    void testSelectedFlag() {
        assertFalse(stub.isSelected(), "Default selected should be false");
        stub.setSelected(true);
        assertTrue(stub.isSelected(), "Selected flag should be true after setSelected(true)");
    }

    @Test
    void testCollidableAndMovableFlags() {
        assertTrue(stub.isCollidable(), "Default collidable should be true");
        stub.setCollidable(false);
        assertFalse(stub.isCollidable(), "Collidable should reflect setter");

        assertTrue(stub.isMovable(), "Default movable should be true");
        stub.setMovable(false);
        assertFalse(stub.isMovable(), "Movable should reflect setter");
    }

    /**
     * After adding incoming messages (beyond any limit),
     * applyLatestSnapshot should invoke myUpdateGlobal at least once.
     */
    @Test
    void testAddIncomingMessageQueueLimit() {
        // enqueue several messages (the implementation only keeps the latest snapshot)
        for (int i = 0; i < 7; i++) {
            stub.addIncomingMessage(new Message("M" + i, new Object[0], "OPT"));
        }
        // now process the latest snapshot
        stub.applyLatestSnapshot();

        assertTrue(stub.globalUpdated,
                "applyLatestSnapshot should invoke myUpdateGlobal at least once");
    }

    @Test
    void testProcessCommands() {
        stub.commandQueue.offer(() -> stub.commandRan = true);
        assertFalse(stub.commandRan, "commandRan should start false");
        stub.processCommands();
        assertTrue(stub.commandRan, "processCommands should execute queued commands");
    }

    @Test
    void testIntersectAndNonIntersect() {
        StubGameObject a = new StubGameObject("A", stub.getGameId());
        StubGameObject b = new StubGameObject("B", stub.getGameId());

        a.setX(0); a.setY(0); a.setWidth(5); a.setHeight(5);
        b.setX(4); b.setY(4); b.setWidth(5); b.setHeight(5);
        assertTrue(a.intersects(b), "Overlapping boxes should intersect");

        b.setX(10); b.setY(10);
        assertFalse(a.intersects(b), "Separated boxes should not intersect");
    }

    @Test
    void testResolveCollisionSymmetric() {
        StubGameObject a = new StubGameObject("A", stub.getGameId());
        StubGameObject b = new StubGameObject("B", stub.getGameId());
        a.setX(0); a.setY(0); a.setWidth(4); a.setHeight(4);
        b.setX(3); b.setY(0); b.setWidth(4); b.setHeight(4);

        a.resolveCollision(b);
        assertEquals(-0.5f, a.getX(), 0.0001, "A should move left by half overlap");
        assertEquals( 3.5f, b.getX(), 0.0001, "B should move right by half overlap");
    }

    @Test
    void testResolveCollisionOneMovableOneStatic() {
        StubGameObject moving = new StubGameObject("Mov", stub.getGameId());
        StubGameObject fixed  = new StubGameObject("Fix",  stub.getGameId());
        moving.setX(0); moving.setY(0); moving.setWidth(4); moving.setHeight(4);
        fixed .setX(3); fixed .setY(0); fixed .setWidth(4); fixed .setHeight(4);
        fixed.setMovable(false);

        moving.resolveCollision(fixed);
        assertEquals(-1f, moving.getX(), 0.0001,
                "Movable object should be pushed out of overlap");
        assertEquals( 3f, fixed .getX(), 0.0001,
                "Fixed object should remain in place");
    }

    @Test
    void testExtractGameId() {
        Message withTwo = new Message("X", new Object[0], "OPT");
        withTwo.setConcealedParameters(new String[]{"obj", "gameZ"});
        assertEquals("gameZ", stub.extractGameId(withTwo));

        Message tooShort = new Message("Y", new Object[0], "OPT");
        tooShort.setConcealedParameters(new String[]{"onlyOne"});
        assertEquals("UnknownGame", stub.extractGameId(tooShort));
    }
}
