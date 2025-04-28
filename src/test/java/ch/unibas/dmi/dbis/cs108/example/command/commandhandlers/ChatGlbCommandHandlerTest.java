package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChatGlbCommandHandler}.
 * <p>
 * These tests verify that global chat messages are correctly broadcast to
 * all other clients, and that invalid or missing parameters are handled
 * gracefully without any broadcast.
 * </p>
 */
public class ChatGlbCommandHandlerTest {

    private Server mockServer;

    /**
     * Sets up a fresh mock Server before each test.
     */
    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
    }

    /**
     * Tests that a valid global chat message is broadcast to others.
     */
    @Test
    public void testValidGlobalChatMessageIsBroadcasted() {
        // Arrange
        String sender = "Alice";
        String messageText = "Hello world!";
        Message msg = new Message("CHATGLB", new Object[]{sender, messageText}, "REQUEST");
        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();

        // Act
        handler.handle(mockServer, msg, sender);

        // Assert: broadcastMessageToOthers should be called once with the same message and sender
        verify(mockServer, times(1)).broadcastMessageToOthers(any(Message.class), eq(sender));
    }

    /**
     * Tests that missing parameters do not cause any broadcast.
     */
    @Test
    public void testMissingParametersAreHandledGracefully() {
        // Arrange: empty parameter array
        Message msg = new Message("CHATGLB", new Object[]{}, "REQUEST");
        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();

        // Act
        handler.handle(mockServer, msg, "Alice");

        // Assert: no broadcast should occur
        verify(mockServer, never()).broadcastMessageToOthers(any(), anyString());
    }

    /**
     * Tests that null parameters are handled without broadcasting.
     */
    @Test
    public void testNullParametersAreHandledGracefully() {
        // Arrange: null parameters
        Message msg = new Message("CHATGLB", null, "REQUEST");
        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();

        // Act
        handler.handle(mockServer, msg, "Alice");

        // Assert: no broadcast should occur
        verify(mockServer, never()).broadcastMessageToOthers(any(), anyString());
    }
}
