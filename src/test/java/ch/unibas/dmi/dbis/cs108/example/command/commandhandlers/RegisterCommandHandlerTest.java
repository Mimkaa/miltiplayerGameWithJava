package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RegisterCommandHandler}.
 * <p>
 * Ensures that REGISTER commands are broadcast to all connected clients
 * and that the message option is updated to "RESPONSE".
 * </p>
 */
class RegisterCommandHandlerTest {

    private Server mockServer;
    private RegisterCommandHandler handler;

    /**
     * Initializes a mocked server and the handler before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new RegisterCommandHandler();
    }

    /**
     * Tests that a REGISTER message is broadcast to all clients
     * and that its option is set to RESPONSE.
     */
    @Test
    void testHandleBroadcastsMessageWithResponseOption() {
        // Arrange
        String senderUsername = "testUser";
        Message registerMessage = new Message("REGISTER", new Object[]{"testParam"}, "REQUEST");

        // Act
        handler.handle(mockServer, registerMessage, senderUsername);

        // Assert: broadcast and option update
        verify(mockServer, times(1)).broadcastMessageToAll(registerMessage);
        assert "RESPONSE".equals(registerMessage.getOption()) :
                "Message option should be set to RESPONSE";
    }
}
