package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WhisperCommandHandler}.
 * <p>
 * These tests verify that:
 * <ul>
 *   <li>A valid whisper message is delivered only to the intended recipient.</li>
 *   <li>If the target user is not found, a "WHISPER-FAILED" response is sent back to the sender.</li>
 *   <li>Invalid or incomplete parameters are handled gracefully (no action).</li>
 * </ul>
 * </p>
 */
public class WhisperCommandHandlerTest {

    private Server mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = mock(Server.class);
    }

    /**
     * Verifies that a properly formed WHISPER message
     * is enqueued only to the target user's address and port.
     */
    @Test
    public void testValidWhisperIsDeliveredToTarget() {
        String sender = "Alice";
        String target = "Bob";
        String content = "Hey, it's a secret!";

        Message whisperMessage = new Message("WHISPER", new Object[]{sender, target, content}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(target, new InetSocketAddress("127.0.0.1", 6000));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        WhisperCommandHandler handler = new WhisperCommandHandler();
        handler.handle(mockServer, whisperMessage, sender);

        verify(mockServer).enqueueMessage(
                any(Message.class),
                eq(clientsMap.get(target).getAddress()),
                eq(6000)
        );
    }

    /**
     * Verifies that if the target user is not online,
     * the handler sends back a WHISPER-FAILED response to the sender.
     */
    @Test
    public void testWhisperFailsIfTargetNotFound() {
        String sender = "Alice";
        String target = "Bob";
        String content = "Hey, are you there?";

        Message whisperMessage = new Message("WHISPER", new Object[]{sender, target, content}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(sender, new InetSocketAddress("127.0.0.1", 6001));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        WhisperCommandHandler handler = new WhisperCommandHandler();
        handler.handle(mockServer, whisperMessage, sender);

        verify(mockServer).enqueueMessage(
                argThat(msg ->
                        msg.getMessageType().equals("WHISPER-FAILED")
                                && ((String) msg.getParameters()[0]).contains("not found")
                ),
                eq(clientsMap.get(sender).getAddress()),
                eq(6001)
        );
    }

    /**
     * Verifies that messages with missing or invalid parameters
     * do not cause any enqueueing of messages.
     */
    @Test
    public void testInvalidParametersAreHandledGracefully() {
        Message whisperMessage = new Message("WHISPER", new Object[]{}, "REQUEST");

        WhisperCommandHandler handler = new WhisperCommandHandler();
        handler.handle(mockServer, whisperMessage, "Alice");

        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }
}
