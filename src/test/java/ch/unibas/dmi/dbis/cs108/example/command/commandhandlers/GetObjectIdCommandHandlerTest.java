package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetObjectIdCommandHandler}.
 * <p>
 * These tests verify that GETOBJECTID commands:
 * <ul>
 *   <li>Are ignored when no game instance exists</li>
 *   <li>Return the correct object ID when a matching object is found</li>
 *   <li>Return an empty string when no matching object is present</li>
 * </ul>
 * </p>
 */
class GetObjectIdCommandHandlerTest {

    private Server mockServer;
    private Game mockGame;
    private GetObjectIdCommandHandler handler;

    /**
     * Sets up mocks and handler before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGame = mock(Game.class);
        handler = new GetObjectIdCommandHandler();
    }

    /**
     * Tests that when no game instance exists, the handler does not broadcast any message.
     */
    @Test
    void testHandle_NoGameInstance_ShouldNotBroadcast() {
        Message incomingMessage = new Message("GETOBJECTID", new Object[]{"TestObject"}, "COMMAND");

        when(mockServer.getMyGameInstance()).thenReturn(null);

        handler.handle(mockServer, incomingMessage, "testUser");

        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }

    /**
     * Tests that when a matching object exists in the game, the handler broadcasts its ID.
     */
    @Test
    void testHandle_GameInstanceWithMatchingObject_ShouldBroadcastCorrectId() {
        GameObject mockGameObject = mock(GameObject.class);
        when(mockGameObject.getName()).thenReturn("TestObject");
        when(mockGameObject.getId()).thenReturn("ObjectId123");

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject));
        Message incomingMessage = new Message("GETOBJECTID", new Object[]{"TestObject"}, "COMMAND");

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(gameObjects);

        handler.handle(mockServer, incomingMessage, "testUser");

        verify(mockServer).broadcastMessageToAll(argThat(msg ->
                msg.getMessageType().equals("GETOBJECTID")
                        && msg.getParameters()[0].equals("ObjectId123")
                        && msg.getOption().equals("RESPONSE")
        ));
    }

    /**
     * Tests that when no matching object is found, the handler broadcasts an empty ID.
     */
    @Test
    void testHandle_GameInstanceWithoutMatchingObject_ShouldBroadcastEmptyId() {
        GameObject mockGameObject = mock(GameObject.class);
        when(mockGameObject.getName()).thenReturn("AnotherObject");

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject));
        Message incomingMessage = new Message("GETOBJECTID", new Object[]{"TestObject"}, "COMMAND");

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(gameObjects);

        handler.handle(mockServer, incomingMessage, "testUser");

        verify(mockServer).broadcastMessageToAll(argThat(msg ->
                msg.getMessageType().equals("GETOBJECTID")
                        && msg.getParameters()[0].equals("")
                        && msg.getOption().equals("RESPONSE")
        ));
    }
}
