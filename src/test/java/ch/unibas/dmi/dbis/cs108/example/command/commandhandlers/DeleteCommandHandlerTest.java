package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.BaseTest;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteCommandHandler}.
 * <p>
 * These tests verify that DeleteCommandHandler properly removes
 * game objects matching a given name from the current game instance
 * and broadcasts deletion messages, as well as handling cases where
 * the game instance is not available.
 * </p>
 */
class DeleteCommandHandlerTest extends BaseTest {

    private Server mockServer;
    private Game realGameInstance;
    private DeleteCommandHandler handler;

    /**
     * Initializes a real Game instance and mocks before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        realGameInstance = new Game("game123", "TestGame");
        handler = new DeleteCommandHandler();
    }

    /**
     * Tests that DeleteCommandHandler removes the matching GameObject
     * from the game's object list and broadcasts a deletion message.
     */
    @Test
    void testHandleDeletesMatchingObject() {
        // Arrange
        String senderUsername = "testUser";
        String targetName = "player1";
        Message deleteMessage = new Message("DELETE", new Object[]{targetName}, "REQUEST");

        GameObject mockGameObject = mock(GameObject.class);
        when(mockGameObject.getName()).thenReturn(targetName);

        realGameInstance.getGameObjects().add(mockGameObject);
        when(mockServer.getMyGameInstance()).thenReturn(realGameInstance);

        // Act
        handler.handle(mockServer, deleteMessage, senderUsername);

        // Assert: object list is empty and a broadcast occurred
        verify(mockServer, times(1)).broadcastMessageToAll(any(Message.class));
        assertTrue(realGameInstance.getGameObjects().isEmpty(),
                "GameObjects list should be empty after deletion");
    }

    /**
     * Tests that when no game instance exists, the handler still broadcasts
     * a default response without attempting deletion.
     */
    @Test
    void testHandleWithNoGameInstance() {
        // Arrange
        String senderUsername = "testUser";
        String targetName = "player1";
        Message deleteMessage = new Message("DELETE", new Object[]{targetName}, "REQUEST");

        when(mockServer.getMyGameInstance()).thenReturn(null);

        // Act
        handler.handle(mockServer, deleteMessage, senderUsername);

        // Assert: broadcast only, no exceptions
        verify(mockServer, times(1)).broadcastMessageToAll(any(Message.class));
    }
}
