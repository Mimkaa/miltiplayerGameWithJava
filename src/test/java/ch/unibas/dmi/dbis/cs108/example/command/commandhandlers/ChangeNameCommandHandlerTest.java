package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChangeNameCommandHandler}.
 * <p>
 * These tests verify that the ChangeNameCommandHandler correctly handles
 * valid name-change requests, gracefully ignores messages with missing
 * parameters, handles non-existent game sessions, and avoids broadcasting
 * when the target object is not found.
 * </p>
 */
class ChangeNameCommandHandlerTest {

    private Server mockServer;
    private Game mockGame;
    private GameObject mockGameObject;
    private GameSessionManager mockGameSessionManager;
    private ChangeNameCommandHandler handler;

    /**
     * Sets up common mocks and the handler before each test.
     */
    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
        mockGame = mock(Game.class);
        mockGameObject = mock(GameObject.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        handler = new ChangeNameCommandHandler();

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    /**
     * Tests that a valid CHANGENAME request updates the target object's name
     * and broadcasts the change to all clients.
     */
    @Test
    public void testValidChangeName() {
        // Arrange
        String gameSessionId = "session123";
        String objectId = UUID.randomUUID().toString();
        String requestedName = "NewName";

        Message message = new Message(
                "CHANGENAME",
                new Object[]{gameSessionId, objectId, requestedName},
                "REQUEST"
        );

        when(mockGameSessionManager.getGameSession(gameSessionId)).thenReturn(mockGame);
        when(mockServer.findUniqueName(requestedName)).thenReturn(requestedName);

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject));
        when(mockGame.getGameObjects()).thenReturn(gameObjects);
        when(mockGameObject.getId()).thenReturn(objectId);

        // Act
        handler.handle(mockServer, message, "Alice");

        // Assert
        verify(mockGameObject).setName(requestedName);
        verify(mockServer).broadcastMessageToAll(any(Message.class));
    }

    /**
     * Tests that the handler does nothing if parameters are missing.
     */
    @Test
    public void testMissingParametersHandledGracefully() {
        // Arrange
        Message message = new Message(
                "CHANGENAME",
                new Object[]{}, // empty parameters
                "REQUEST"
        );

        // Act
        handler.handle(mockServer, message, "Alice");

        // Assert: no interactions with the server should occur
        verifyNoInteractions(mockServer);
    }

    /**
     * Tests that no broadcast occurs when the specified game session does not exist.
     */
    @Test
    public void testGameSessionNotFound() {
        // Arrange
        String gameSessionId = "nonexistentSession";
        String objectId = UUID.randomUUID().toString();
        String requestedName = "NewName";

        Message message = new Message(
                "CHANGENAME",
                new Object[]{gameSessionId, objectId, requestedName},
                "REQUEST"
        );

        when(mockGameSessionManager.getGameSession(gameSessionId)).thenReturn(null);

        // Act
        handler.handle(mockServer, message, "Alice");

        // Assert: should never broadcast
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }

    /**
     * Tests that no broadcast occurs when the target object ID is not found
     * in the game session's object list.
     */
    @Test
    public void testObjectNotFound() {
        // Arrange
        String gameSessionId = "session123";
        String objectId = UUID.randomUUID().toString();
        String requestedName = "NewName";

        Message message = new Message(
                "CHANGENAME",
                new Object[]{gameSessionId, objectId, requestedName},
                "REQUEST"
        );

        when(mockGameSessionManager.getGameSession(gameSessionId)).thenReturn(mockGame);
        when(mockServer.findUniqueName(requestedName)).thenReturn(requestedName);

        // Empty game object list to simulate missing object
        when(mockGame.getGameObjects()).thenReturn(new CopyOnWriteArrayList<>());

        // Act
        handler.handle(mockServer, message, "Alice");

        // Assert: should never broadcast
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }
}
