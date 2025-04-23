package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the ChatLobbyCommandHandler class.
 */
public class ChatLobbyCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private Game mockGame;

    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(Game.class);

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    @Test
    public void testValidLobbyChatMessageIsBroadcasted() {
        // Arrange
        String sender = "Alice";
        String gameId = "game123";
        String messageText = "Hello lobby!";
        Set<String> users = new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie"));

        Message message = new Message("CHATLOBBY", new String[]{sender, gameId, messageText}, "REQUEST");

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame);
        when(mockGame.getUsers()).thenReturn(users);

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put("Bob", new InetSocketAddress("127.0.0.1", 5000));
        clientsMap.put("Charlie", new InetSocketAddress("127.0.0.1", 5001));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        ChatLobbyCommandHandler handler = new ChatLobbyCommandHandler();

        // Act
        handler.handle(mockServer, message, sender);

        // Assert: Bob and Charlie should receive the message
        verify(mockServer).enqueueMessage(any(Message.class), eq(clientsMap.get("Bob").getAddress()), eq(5000));
        verify(mockServer).enqueueMessage(any(Message.class), eq(clientsMap.get("Charlie").getAddress()), eq(5001));
    }

    @Test
    public void testMissingParametersAreHandledGracefully() {
        // Arrange
        Message message = new Message("CHATLOBBY", new String[]{}, "REQUEST");

        ChatLobbyCommandHandler handler = new ChatLobbyCommandHandler();

        // Act
        handler.handle(mockServer, message, "Alice");

        // Assert
        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }

    @Test
    public void testGameSessionNotFound() {
        // Arrange
        String sender = "Alice";
        String gameId = "nonexistent";
        String messageText = "Anyone here?";
        Message message = new Message("CHATLOBBY", new String[]{sender, gameId, messageText}, "REQUEST");

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(null);

        ChatLobbyCommandHandler handler = new ChatLobbyCommandHandler();

        // Act
        handler.handle(mockServer, message, sender);

        // Assert
        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }
}
