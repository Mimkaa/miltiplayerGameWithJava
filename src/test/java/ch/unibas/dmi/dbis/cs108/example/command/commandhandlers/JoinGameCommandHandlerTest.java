package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JoinGameCommandHandler}.
 * <p>
 * Verifies that JOINGAME commands:
 * <ul>
 *   <li>Broadcast to all clients and enqueue individual messages when joining an existing game</li>
 *   <li>Send error responses when attempting to join non-existent games</li>
 *   <li>Handle missing parameters gracefully without broadcasting or enqueuing messages</li>
 * </ul>
 * </p>
 */
class JoinGameCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private Game mockGame;
    private JoinGameCommandHandler handler;

    /**
     * Initializes the mocks and handler before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(Game.class);
        handler = new JoinGameCommandHandler();

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    /**
     * Tests successful join of an existing game: broadcasts to all and enqueues to the joining client.
     */
    @Test
    void testJoinExistingGameSuccessfully() {
        String senderUsername = "testUser";
        String requestedGameName = "TestGame";
        String prevGameId = "default";
        Message joinGameMessage = new Message("JOINGAME", new Object[]{requestedGameName, senderUsername, prevGameId}, "REQUEST");

        when(mockGame.getGameName()).thenReturn(requestedGameName);
        when(mockGame.getGameId()).thenReturn("game123");

        ConcurrentHashMap<String, Game> sessions = new ConcurrentHashMap<>();
        sessions.put("game123", mockGame);
        when(mockGameSessionManager.getAllGameSessions()).thenReturn(sessions);

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        handler.handle(mockServer, joinGameMessage, senderUsername);

        verify(mockServer, times(1)).broadcastMessageToAll(any(Message.class));
        verify(mockServer, times(1)).enqueueMessage(any(Message.class), any(), anyInt());
    }

    /**
     * Tests joining a non-existent game: should enqueue an error message and not broadcast.
     */
    @Test
    void testJoinNonExistingGameSendsError() {
        String senderUsername = "testUser";
        String requestedGameName = "NonExistentGame";
        String prevGameId = "default";
        Message joinGameMessage = new Message("JOINGAME", new Object[]{requestedGameName, senderUsername, prevGameId}, "REQUEST");

        ConcurrentHashMap<String, Game> sessions = new ConcurrentHashMap<>();
        when(mockGameSessionManager.getAllGameSessions()).thenReturn(sessions);

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        handler.handle(mockServer, joinGameMessage, senderUsername);

        verify(mockServer, times(1)).enqueueMessage(any(Message.class), any(), anyInt());
        verify(mockServer, never()).broadcastMessageToAll(any());
    }

    /**
     * Tests handling of missing parameters: no broadcasting or enqueuing should occur.
     */
    @Test
    void testHandleWithMissingParams() {
        String senderUsername = "testUser";
        Message joinGameMessage = new Message("JOINGAME", new Object[]{}, "REQUEST");

        handler.handle(mockServer, joinGameMessage, senderUsername);

        verify(mockServer, never()).broadcastMessageToAll(any());
        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }
}
