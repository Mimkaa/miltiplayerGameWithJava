package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test suite for {@link GameContext}, covering singleton behavior,
 * update() logic for key events, and draw() rendering logic.
 */
public class GameContextTest {

    private GameContext context;

    /**
     * Set up a fresh GameContext instance before each test by resetting
     * the singleton and static ID fields.
     *
     * @throws Exception if reflection fails
     */
    @BeforeEach
    void setup() throws Exception {
        Field instanceField = GameContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field currentField = GameContext.class.getDeclaredField("currentGameId");
        currentField.setAccessible(true);
        AtomicReference<String> currentRef = (AtomicReference<String>) currentField.get(null);
        currentRef.set(null);

        Field selectedField = GameContext.class.getDeclaredField("selectedGameObjectId");
        selectedField.setAccessible(true);
        AtomicReference<String> selectedRef = (AtomicReference<String>) selectedField.get(null);
        selectedRef.set(null);

        context = new GameContext();
    }

    /**
     * Clean up pressed and released keys after each test to avoid state leakage.
     */
    @AfterEach
    void teardown() {
        for (KeyCode key : KeyboardState.getPressedKeys()) {
            KeyboardState.keyReleased(key);
        }
        KeyboardState.getAndClearReleasedKeys();
    }

    /**
     * Test that GameContext.getInstance() always returns the same singleton instance.
     */
    @Test
    void testSingletonInstance() {
        GameContext first = GameContext.getInstance();
        assertSame(first, GameContext.getInstance(), "Instance should remain the same for singleton");
    }

    /**
     * Test that currentGameId and selectedGameObjectId are null when uninitialized.
     */
    @Test
    void testGetCurrentAndSelectedGameIdsInitiallyNull() {
        assertNull(GameContext.getCurrentGameId(), "currentGameId should initially be null");
        assertNull(GameContext.getSelectedGameObjectId(), "selectedGameObjectId should initially be null");
    }

    /**
     * Test that getGameById returns null for an ID that does not exist.
     */
    @Test
    void testGetGameByIdReturnsNullIfNotExists() {
        assertNull(GameContext.getGameById("nonexistent"), "Should be null for unknown ID");
    }

    /**
     * Test that after adding a session, getGameById returns the correct Game object.
     */
    @Test
    void testGetGameByIdAfterAddingSession() {
        context.getGameSessionManager().addGameSession("session1", "TestGame");
        Game game = GameContext.getGameById("session1");
        assertNotNull(game, "Game should not be null after adding session");
        assertEquals("TestGame", game.getGameName(), "Game name should match the one provided");
        assertEquals("session1", game.getGameId(), "Game ID should match the one provided");
    }

    /**
     * Test that update() does not send any messages when no key is pressed.
     */
    @Test
    void testUpdateWithoutKeyPress() {
        try (MockedStatic<Client> clientStatic = Mockito.mockStatic(Client.class)) {
            context.update();
            clientStatic.verify(() -> Client.sendMessageBestEffort(any()), never());
        }
    }

    /**
     * Test that update() sends exactly one best-effort message when a key is pressed,
     * and that the concealed parameters match selectedGameObjectId and currentGameId.
     *
     * @throws Exception if setting static fields via reflection fails
     */
    @Test
    void testUpdateWithKeyPress() throws Exception {
        try (MockedStatic<Client> clientStatic = Mockito.mockStatic(Client.class)) {
            setStaticId("currentGameId", "game1");
            setStaticId("selectedGameObjectId", "obj1");

            KeyboardState.keyPressed(KeyCode.SPACE);
            context.update();

            clientStatic.verify(() -> Client.sendMessageBestEffort(argThat(msg -> {
                String[] concealed = msg.getConcealedParameters();
                return concealed.length >= 2
                        && "obj1".equals(concealed[0])
                        && "game1".equals(concealed[1]);
            })), times(1));
        }
    }

    /**
     * Test that draw() performs no operations when currentGameId is not set.
     *
     * @throws Exception if reflection fails
     */
    @Test
    void testDrawWithNoCurrentGame() throws Exception {
        GraphicsContext gc = mock(GraphicsContext.class);
        Method drawMethod = GameContext.class.getDeclaredMethod("draw", GraphicsContext.class);
        drawMethod.setAccessible(true);
        drawMethod.invoke(context, gc);
        verifyNoInteractions(gc);
    }

    /**
     * Test that draw() clears the canvas and calls Game.draw(gc) when a Game session exists.
     *
     * @throws Exception if reflection or mock setup fails
     */
    @Test
    void testDrawWithExistingGame() throws Exception {
        Game realGame = new Game("sessionX", "TestGame");
        Game spyGame = spy(realGame);
        context.getGameSessionManager().addGameSession("sessionX", spyGame);
        setStaticId("currentGameId", "sessionX");

        Canvas canvas = mock(Canvas.class);
        when(canvas.getWidth()).thenReturn(200.0);
        when(canvas.getHeight()).thenReturn(100.0);
        GraphicsContext gc = mock(GraphicsContext.class);
        when(gc.getCanvas()).thenReturn(canvas);

        Method drawMethod = GameContext.class.getDeclaredMethod("draw", GraphicsContext.class);
        drawMethod.setAccessible(true);
        drawMethod.invoke(context, gc);

        verify(gc).setFill(Color.WHITE);
        verify(gc).fillRect(0, 0, 200.0, 100.0);
        verify(spyGame).draw(gc);
    }

    /**
     * Helper method to set private static AtomicReference fields in GameContext.
     *
     * @param fieldName the name of the static field ("currentGameId" or "selectedGameObjectId")
     * @param value     the value to set
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private void setStaticId(String fieldName, String value) throws Exception {
        Field field = GameContext.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        AtomicReference<String> ref = (AtomicReference<String>) field.get(null);
        ref.set(value);
    }
}
