package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class RegisterCommandHandlerTest {

    private Server mockServer;
    private RegisterCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new RegisterCommandHandler();
    }

    @Test
    void testHandleBroadcastsMessageWithResponseOption() {
        // Arrange
        String senderUsername = "testUser";
        Message registerMessage = new Message("REGISTER", new Object[]{"testParam"}, "REQUEST");

        // Act
        handler.handle(mockServer, registerMessage, senderUsername);

        // Assert
        verify(mockServer, times(1)).broadcastMessageToAll(registerMessage);
        assert "RESPONSE".equals(registerMessage.getOption()) : "Message option should be set to RESPONSE";
    }
}
