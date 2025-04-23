package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the ChatGlbCommandHandler class.
 */
public class ChatGlbCommandHandlerTest {

    private Server mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
    }

    @Test
    public void testValidGlobalChatMessageIsBroadcasted() {
        // Arrange
        String sender = "Alice";
        String messageText = "Hello world!";
        Message msg = new Message("CHATGLB", new Object[]{sender, messageText}, "REQUEST");

        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();

        // Act
        handler.handle(mockServer, msg, sender);

        // Assert
        verify(mockServer, times(1)).broadcastMessageToOthers(any(Message.class), eq(sender));
    }

    @Test
    public void testMissingParametersAreHandledGracefully() {
        // Arrange: empty parameters (invalid)
        Message msg = new Message("CHATGLB", new Object[]{}, "REQUEST");

        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();

        // Act
        handler.handle(mockServer, msg, "Alice");

        // Assert: Should not broadcast anything
        verify(mockServer, never()).broadcastMessageToOthers(any(), anyString());
    }

    @Test
    public void testNullParametersAreHandledGracefully() {
        // Arrange: null parameters (invalid)
        Message msg = new Message("CHATGLB", null, "REQUEST");

        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();

        // Act
        handler.handle(mockServer, msg, "Alice");

        // Assert
        verify(mockServer, never()).broadcastMessageToOthers(any(), anyString());
    }
}
