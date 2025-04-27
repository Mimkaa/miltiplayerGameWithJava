package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

class StartGameCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private Game mockGame;
    private StartGameCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(Game.class);
        handler = new StartGameCommandHandler();

        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    @Test
    void testHandleTogglesStartedFlagAndBroadcasts() {
        // Arrange
        String gameId = "game123";
        String senderUsername = "testUser";
        Message startGameMessage = new Message("STARTGAME", new Object[]{gameId}, "REQUEST");

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame);
        when(mockGame.getStartedFlag()).thenReturn(false); // Game not started initially

        // Act
        handler.handle(mockServer, startGameMessage, senderUsername);

        // Assert
        verify(mockGame, times(1)).setStartedFlag(true); // Should toggle to true
        verify(mockServer, times(1)).broadcastMessageToAll(startGameMessage);
        assert "RESPONSE".equals(startGameMessage.getOption()) : "Message option should be set to RESPONSE";
    }
}
