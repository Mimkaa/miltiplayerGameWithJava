package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CreateCommandHandler}.
 * <p>
 * These tests verify that the CreateCommandHandler correctly handles
 * the creation of new game objects in the current game instance and
 * broadcasts the appropriate messages both on success and on failure.
 * </p>
 */
class CreateCommandHandlerTest {

    private Server mockServer;
    private Game mockGame;
    private CreateCommandHandler handler;

    /**
     * Initializes mocks and handler before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGame = mock(Game.class);
        handler = new CreateCommandHandler();

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
    }

    /**
     * Tests that when an object is successfully created, the handler
     * broadcasts a message to all clients.
     *
     * @throws Exception if the asynchronous creation fails unexpectedly
     */
    @Test
    void testHandleCreatesGameObjectAndBroadcastsMessage() throws Exception {
        // Arrange
        String objectType = "Player2";
        String additionalData = "SomeData";
        Message createMessage = new Message("CREATE", new Object[]{objectType, additionalData}, "REQUEST");

        GameObject mockGameObject = mock(GameObject.class);
        when(mockGame.addGameObjectAsync(eq(objectType), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockGameObject));

        when(mockGameObject.getName()).thenReturn("CreatedObject");

        // Act
        handler.handle(mockServer, createMessage, "TestUser");

        // Assert
        verify(mockServer, atLeastOnce()).broadcastMessageToAll(any(Message.class));
    }

    /**
     * Tests that when object creation fails, the handler still broadcasts
     * an error response message to all clients.
     *
     * @throws Exception if the test setup fails unexpectedly
     */
    @Test
    void testHandleWithFailedObjectCreation() throws Exception {
        // Arrange
        String objectType = "Player2";
        Message createMessage = new Message("CREATE", new Object[]{objectType}, "REQUEST");

        when(mockGame.addGameObjectAsync(eq(objectType), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Creation failed")));

        // Act
        handler.handle(mockServer, createMessage, "TestUser");

        // Assert
        verify(mockServer, atLeastOnce()).broadcastMessageToAll(any(Message.class));
    }
}
