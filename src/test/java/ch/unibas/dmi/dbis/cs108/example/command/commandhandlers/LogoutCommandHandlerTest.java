package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LogoutCommandHandler}.
 * <p>
 * Verifies that LOGOUT commands:
 * <ul>
 *   <li>Remove the user from the server's client map and enqueue a logout response when the user exists.</li>
 *   <li>Handle missing client addresses gracefully without enqueuing messages.</li>
 * </ul>
 * </p>
 */
class LogoutCommandHandlerTest {

    private Server mockServer;
    private LogoutCommandHandler handler;

    /**
     * Initializes the mock server and handler before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new LogoutCommandHandler();
    }

    /**
     * Tests that a successful logout:
     * - Enqueues a logout message back to the client's address.
     * - Removes the user from the clients map.
     */
    @Test
    void testHandleSuccessfulLogout() {
        String senderUsername = "testUser";
        Message logoutMessage = new Message("LOGOUT", new Object[]{}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        handler.handle(mockServer, logoutMessage, senderUsername);

        verify(mockServer, times(1))
                .enqueueMessage(any(Message.class), any(), anyInt());
        assert !clientsMap.containsKey(senderUsername);
    }

    /**
     * Tests that a logout attempt with no registered client address:
     * - Does not attempt to enqueue any message.
     * - Leaves the clients map unchanged.
     */
    @Test
    void testHandleLogoutWithoutClientAddress() {
        String senderUsername = "testUser";
        Message logoutMessage = new Message("LOGOUT", new Object[]{}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        handler.handle(mockServer, logoutMessage, senderUsername);

        verify(mockServer, never())
                .enqueueMessage(any(), any(), anyInt());
        assert !clientsMap.containsKey(senderUsername);
    }
}
