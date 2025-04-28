package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StartGameCommandHandler}.
 * <p>
 * Verifies that invoking the STARTGAME command:
 * <ul>
 *   <li>Toggles the game's started flag from false to true, and</li>
 *   <li>Broadcasts the original message with its option switched to RESPONSE.</li>
 * </ul>
 * </p>
 */
class StartGameCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private Game mockGame;
    private StartGameCommandHandler handler;

    /**
     * Prepare mocks and a fresh handler before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(Game.class);
        handler = new StartGameCommandHandler();
        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    /**
     * Ensures that handling a STARTGAME request:
     * <ul>
     *   <li>Sets the game's startedFlag to true (when initially false),</li>
     *   <li>Broadcasts the command message to all clients, and</li>
     *   <li>Marks the message option to "RESPONSE".</li>
     * </ul>
     */
    @Test
    void testHandleTogglesStartedFlagAndBroadcasts() {
        // Arrange
        String gameId = "game123";
        String senderUsername = "testUser";
        Message startGameMessage = new Message("STARTGAME", new Object[]{gameId}, "REQUEST");
        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame);
        when(mockGame.getStartedFlag()).thenReturn(false); // not started yet

        // Act
        handler.handle(mockServer, startGameMessage, senderUsername);

        // Assert
        verify(mockGame, times(1)).setStartedFlag(true);
        verify(mockServer, times(1)).broadcastMessageToAll(startGameMessage);
        assert "RESPONSE".equals(startGameMessage.getOption())
                : "Message option should be set to RESPONSE";
    }
}
