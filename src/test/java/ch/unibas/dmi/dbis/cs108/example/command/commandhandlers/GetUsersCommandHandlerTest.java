package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

class GetUsersCommandHandlerTest {

    private Server mockServer;
    private GetUsersCommandHandler handler;
    private ConcurrentHashMap<String, InetSocketAddress> clientsMap;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new GetUsersCommandHandler();
        clientsMap = new ConcurrentHashMap<>();

        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
    }

    @Test
    void testHandle_UserExists_ShouldSendUserList() throws Exception {
        // Arrange
        String username = "testUser";
        Message incomingMessage = new Message("GETUSERS", new Object[]{}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        clientsMap.put(username, socketAddress);
        clientsMap.put("anotherUser", new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12346));

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert
        verify(mockServer).enqueueMessage(
                argThat(msg ->
                        msg.getMessageType().equals("GETUSERS")
                                && msg.getOption().equals("RESPONSE")
                                && msg.getParameters().length == 2 // We expect two usernames in the response
                ),
                eq(address),
                eq(port)
        );
    }

    @Test
    void testHandle_UserNotFound_ShouldDoNothing() throws Exception {
        // Arrange
        String username = "nonexistentUser";
        Message incomingMessage = new Message("GETUSERS", new Object[]{}, "COMMAND");

        // The clientsMap is empty or does not contain the user

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert
        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }
}
