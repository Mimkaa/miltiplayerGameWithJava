package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetUsersCommandHandler}.
 * <p>
 * These tests verify the behavior of GETUSERS commands:
 * <ul>
 *   <li>When the requesting user exists in the clients map, a RESPONSE containing the list of all usernames is sent back.</li>
 *   <li>When the requesting user is not found, no message is enqueued.</li>
 * </ul>
 * </p>
 */
class GetUsersCommandHandlerTest {

    private Server mockServer;
    private GetUsersCommandHandler handler;
    private ConcurrentHashMap<String, InetSocketAddress> clientsMap;

    /**
     * Initializes the mocked server, handler, and clients map before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new GetUsersCommandHandler();
        clientsMap = new ConcurrentHashMap<>();
        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
    }

    /**
     * Tests that when the requesting user is present, a RESPONSE message
     * with the full list of usernames is enqueued to that user's socket.
     *
     * @throws Exception if address lookup fails
     */
    @Test
    void testHandle_UserExists_ShouldSendUserList() throws Exception {
        String username = "testUser";
        Message incomingMessage = new Message("GETUSERS", new Object[]{}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        // Populate clients map with two users
        clientsMap.put(username, socketAddress);
        clientsMap.put("anotherUser", new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12346));

        handler.handle(mockServer, incomingMessage, username);

        // Verify that a RESPONSE containing two usernames was enqueued to testUser
        verify(mockServer).enqueueMessage(
                argThat(msg ->
                        msg.getMessageType().equals("GETUSERS")
                                && msg.getOption().equals("RESPONSE")
                                && msg.getParameters().length == 2
                ),
                eq(address),
                eq(port)
        );
    }

    /**
     * Tests that when the requesting user is not found in the clients map,
     * the handler does nothing (no message enqueued).
     *
     * @throws Exception never thrown in this test
     */
    @Test
    void testHandle_UserNotFound_ShouldDoNothing() throws Exception {
        String username = "nonexistentUser";
        Message incomingMessage = new Message("GETUSERS", new Object[]{}, "COMMAND");

        handler.handle(mockServer, incomingMessage, username);

        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }
}
