package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Game} class.
 * <p>
 * Uses an inner {@link StubGameObject} to exercise
 * collision, routing and selection logic without
 * dragging in any real rendering or networking.
 * </p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameTest {

    private Game game;

    @BeforeAll
    void beforeAll() {
        // no-op
    }

    @AfterAll
    void afterAll() {
        // shut down the shared async pool once all tests have run
        AsyncManager.shutdown();
    }

    @BeforeEach
    void setUp() {
        game = new Game("game-123", "TestGame");
    }

    /**
     * A minimal concrete {@code GameObject} for testing.
     *
     * Implements all abstract methods as no-ops or simple state
     * so we can add it to a {@link Game} and drive collision,
     * selection and message-routing logic.
     */
    private static class StubGameObject extends GameObject {
        boolean globalUpdated = false;
        float x, y, w, h;

        StubGameObject(String name, String gameId) {
            super(name, gameId);
        }

        @Override public float getX()                { return x; }
        @Override public float getY()                { return y; }
        @Override public float getWidth()            { return w; }
        @Override public float getHeight()           { return h; }
        @Override public void setX(float x)          { this.x = x; }
        @Override public void setY(float y)          { this.y = y; }
        @Override public void setWidth(float width)  { this.w = width; }
        @Override public void setHeight(float height){ this.h = height; }

        @Override public void myUpdateLocal()                     { /* no-op */ }
        @Override public void myUpdateLocal(float deltaTime)      { /* no-op */ }
        @Override protected void myUpdateGlobal(Message msg)      { globalUpdated = true; }
        @Override public void draw(GraphicsContext gc)            { /* no-op */ }
        @Override public Object[] getConstructorParamValues()     { return new Object[0]; }
        @Override public Message createSnapshot()                 { return null; }
    }

    @Test
    void testConstructorAndDefaults() {
        assertEquals("game-123", game.getGameId());
        assertEquals("TestGame", game.getGameName());
        assertFalse(game.getStartedFlag());
        assertTrue(game.getUsers().isEmpty());
        assertTrue(game.getGameObjects().isEmpty());
    }

    @Test
    void testContainsObjectByName() {
        StubGameObject obj = new StubGameObject("Alice", game.getGameId());
        game.getGameObjects().add(obj);
        assertTrue(game.containsObjectByName("Alice"));
        assertFalse(game.containsObjectByName("Bob"));
    }

    @Test
    void testGetSelectedGameObjectIdWhenNone() {
        assertNull(game.getSelectedGameObjectId());
    }

    @Test
    void testGetSelectedGameObjectIdWhenSelected() {
        StubGameObject obj = new StubGameObject("X", game.getGameId());
        game.getGameObjects().add(obj);
        obj.setSelected(true);
        assertEquals(obj.getId(), game.getSelectedGameObjectId());
    }

    @Test
    void testIntersectsAndNonIntersects() {
        StubGameObject a = new StubGameObject("A", game.getGameId());
        StubGameObject b = new StubGameObject("B", game.getGameId());
        a.setX(0);  a.setY(0);  a.setWidth(10); a.setHeight(10);
        b.setX(5);  b.setY(5);  b.setWidth(10); b.setHeight(10);
        assertTrue(a.intersects(b));
        b.setX(20); b.setY(20);
        assertFalse(a.intersects(b));
    }

    @Test
    void testResolveCollisionSymmetric() {
        StubGameObject a = new StubGameObject("A", game.getGameId());
        StubGameObject b = new StubGameObject("B", game.getGameId());
        a.setX(0);  a.setY(0);  a.setWidth(10); a.setHeight(10);
        b.setX(8);  b.setY(0);  b.setWidth(10); b.setHeight(10);
        game.getGameObjects().add(a);
        game.getGameObjects().add(b);

        // overlapX = 2, overlapY = 10 → resolve along X
        a.resolveCollision(b);

        assertEquals(-1f, a.getX(), 0.0001);
        assertEquals(9f,  b.getX(), 0.0001);
    }

    @Test
    void testRouteMessageToGameObjectViaReflection() throws Exception {
        StubGameObject obj = new StubGameObject("O", game.getGameId());
        game.getGameObjects().add(obj);

        Message msg = new Message("M", new Object[0], "GAME");
        msg.setConcealedParameters(new String[]{obj.getId(), game.getGameId()});

        Method route = Game.class.getDeclaredMethod("routeMessageToGameObject", Message.class);
        route.setAccessible(true);
        route.invoke(game, msg);

        // now drain the stub's incoming queue so myUpdateGlobal(...) runs
        //obj.processIncomingMessages();

        assertTrue(obj.globalUpdated, "After routing + processing, stub should have been updated");
    }
}
