package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the WhisperCommandHandler class.
 */
public class WhisperCommandHandlerTest {

    private Server mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
    }

    @Test
    public void testValidWhisperIsDeliveredToTarget() {
        // Arrange
        String sender = "Alice";
        String target = "Bob";
        String content = "Hey, it's a secret!";

        Message whisperMessage = new Message("WHISPER", new Object[]{sender, target, content}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(target, new InetSocketAddress("127.0.0.1", 6000));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        WhisperCommandHandler handler = new WhisperCommandHandler();

        // Act
        handler.handle(mockServer, whisperMessage, sender);

        // Assert
        verify(mockServer).enqueueMessage(any(Message.class), eq(clientsMap.get(target).getAddress()), eq(6000));
    }

    @Test
    public void testWhisperFailsIfTargetNotFound() {
        // Arrange
        String sender = "Alice";
        String target = "Bob";
        String content = "Hey, are you there?";

        Message whisperMessage = new Message("WHISPER", new Object[]{sender, target, content}, "REQUEST");

        // Only sender is online
        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(sender, new InetSocketAddress("127.0.0.1", 6001));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        WhisperCommandHandler handler = new WhisperCommandHandler();

        // Act
        handler.handle(mockServer, whisperMessage, sender);

        // Assert: A WHISPER-FAILED message is sent to sender
        verify(mockServer).enqueueMessage(argThat(msg ->
                msg.getMessageType().equals("WHISPER-FAILED")
                        && ((String) msg.getParameters()[0]).contains("not found")
        ), eq(clientsMap.get(sender).getAddress()), eq(6001));
    }

    @Test
    public void testInvalidParametersAreHandledGracefully() {
        // Arrange: missing content
        Message whisperMessage = new Message("WHISPER", new Object[]{}, "REQUEST");

        WhisperCommandHandler handler = new WhisperCommandHandler();

        // Act
        handler.handle(mockServer, whisperMessage, "Alice");

        // Assert: No message should be sent
        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }
}
