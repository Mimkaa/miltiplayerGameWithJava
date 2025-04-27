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

class JoinGameCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private Game mockGame;
    private JoinGameCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(Game.class);
        handler = new JoinGameCommandHandler();

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

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

    @Test
    void testHandleWithMissingParams() {
        String senderUsername = "testUser";
        Message joinGameMessage = new Message("JOINGAME", new Object[]{}, "REQUEST");

        handler.handle(mockServer, joinGameMessage, senderUsername);

        verify(mockServer, never()).broadcastMessageToAll(any());
        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }
}
