package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class ExitCommandHandlerTest {

    private Server mockServer;
    private ExitCommandHandler handler;
    private Map<String, InetSocketAddress> clientsMap;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new ExitCommandHandler();
        clientsMap = new HashMap<>();

        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
    }

    @Test
    void testHandle_RemovesUserAndSendsExitMessage() throws Exception {
        // Arrange
        String username = "testUser";
        Message incomingMessage = new Message("EXIT", new String[]{}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        clientsMap.put(username, socketAddress);

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert
        verify(mockServer).enqueueMessage(any(Message.class), eq(address), eq(port));
        assert !clientsMap.containsKey(username);
    }
}
