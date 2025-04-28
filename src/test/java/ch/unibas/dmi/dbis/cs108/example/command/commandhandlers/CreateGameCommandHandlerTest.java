package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.BaseTest;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateGameCommandHandlerTest extends BaseTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private CreateGameCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        handler = new CreateGameCommandHandler();

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    @Test
    void testHandleCreatesGameSessionAndBroadcasts() {
        // Arrange
        String senderUsername = "testUser";
        String requestedGameName = "TestGame";
        Message createGameMessage = new Message("CREATEGAME", new Object[]{requestedGameName}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        // Act
        handler.handle(mockServer, createGameMessage, senderUsername);

        // Assert
        verify(mockGameSessionManager, times(1)).addGameSession(anyString(), any(Game.class));
        verify(mockServer, times(1)).broadcastMessageToAll(argThat(message ->
                "CREATEGAME".equals(message.getMessageType())
        ));
    }

    @Test
    void testHandleMissingParametersDoesNothing() {
        // Arrange
        String senderUsername = "testUser";
        Message createGameMessage = new Message("CREATEGAME", new Object[]{}, "REQUEST");

        // Act
        handler.handle(mockServer, createGameMessage, senderUsername);

        // Assert
        verify(mockGameSessionManager, never()).addGameSession(anyString(), any(Game.class));
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }

    @Test
    void testHandleSenderAddressNotFound() {
        // Arrange
        String senderUsername = "testUser";
        String requestedGameName = "TestGame";
        Message createGameMessage = new Message("CREATEGAME", new Object[]{requestedGameName}, "REQUEST");

        // Sender is not in clientsMap
        when(mockServer.getClientsMap()).thenReturn(new ConcurrentHashMap<>());

        // Act
        handler.handle(mockServer, createGameMessage, senderUsername);

        // Assert
        verify(mockGameSessionManager, times(1)).addGameSession(anyString(), any(Game.class));
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }
}
