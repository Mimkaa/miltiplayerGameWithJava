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

class ChangeNameCommandHandlerTest {

    private Server mockServer;
    private Game mockGame;
    private GameObject mockGameObject;
    private GameSessionManager mockGameSessionManager;
    private ChangeNameCommandHandler handler;

    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
        mockGame = mock(Game.class);
        mockGameObject = mock(GameObject.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        handler = new ChangeNameCommandHandler();

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

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

        // Assert
        verifyNoInteractions(mockServer);
    }

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

        // Assert
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }

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

        // Empty game object list (so no object will be found)
        CopyOnWriteArrayList<GameObject> emptyGameObjects = new CopyOnWriteArrayList<>();
        when(mockGame.getGameObjects()).thenReturn(emptyGameObjects);

        // Act
        handler.handle(mockServer, message, "Alice");

        // Assert
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }
}
