package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExitCommandHandler}.
 * <p>
 * Verifies that the handler correctly processes EXIT commands by:
 * <ul>
 *   <li>Removing the user from the server's clients map</li>
 *   <li>Enqueuing an EXIT message back to the client's socket</li>
 * </ul>
 * </p>
 */
class ExitCommandHandlerTest {

    private Server mockServer;
    private ExitCommandHandler handler;
    private ConcurrentHashMap<String, InetSocketAddress> clientsMap;

    /**
     * Sets up mocks and test instance before each test.
     */
    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new ExitCommandHandler();
        clientsMap = new ConcurrentHashMap<>();

        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
    }

    /**
     * Tests that a valid EXIT command removes the user from the clients map
     * and sends an EXIT message to the former client's address.
     *
     * @throws Exception if address resolution fails
     */
    @Test
    void testHandle_RemovesUserAndSendsExitMessage() throws Exception {
        // Arrange
        String username = "testUser";
        Message incomingMessage = new Message("EXIT", new Object[]{}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        clientsMap.put(username, socketAddress);

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert: ensure EXIT message is enqueued and user removed
        verify(mockServer).enqueueMessage(any(Message.class), eq(address), eq(port));
        assertFalse(clientsMap.containsKey(username), "User should be removed from clients map");
    }
}
