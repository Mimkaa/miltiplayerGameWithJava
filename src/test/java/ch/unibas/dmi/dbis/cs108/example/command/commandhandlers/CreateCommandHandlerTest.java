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

class CreateCommandHandlerTest {

    private Server mockServer;
    private Game mockGame;
    private CreateCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGame = mock(Game.class);
        handler = new CreateCommandHandler();

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
    }

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
