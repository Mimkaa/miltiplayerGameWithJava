package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.BaseTest;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteCommandHandlerTest extends BaseTest {

    private Server mockServer;
    private Game realGameInstance;
    private DeleteCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        realGameInstance = new Game("game123", "TestGame");
        handler = new DeleteCommandHandler();
    }

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

        // Assert
        verify(mockServer, times(1)).broadcastMessageToAll(any(Message.class));
        assertTrue(realGameInstance.getGameObjects().isEmpty(), "GameObjects list should be empty after deletion");
    }

    @Test
    void testHandleWithNoGameInstance() {
        // Arrange
        String senderUsername = "testUser";
        String targetName = "player1";
        Message deleteMessage = new Message("DELETE", new Object[]{targetName}, "REQUEST");

        when(mockServer.getMyGameInstance()).thenReturn(null);

        // Act
        handler.handle(mockServer, deleteMessage, senderUsername);

        // Assert
        verify(mockServer, times(1)).broadcastMessageToAll(any(Message.class));
    }
}
