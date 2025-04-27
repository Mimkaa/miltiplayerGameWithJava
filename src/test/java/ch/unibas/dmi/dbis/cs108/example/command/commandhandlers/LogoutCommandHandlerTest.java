package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

class LogoutCommandHandlerTest {

    private Server mockServer;
    private LogoutCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new LogoutCommandHandler();
    }

    @Test
    void testHandleSuccessfulLogout() {
        String senderUsername = "testUser";
        Message logoutMessage = new Message("LOGOUT", new Object[]{}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        handler.handle(mockServer, logoutMessage, senderUsername);

        verify(mockServer, times(1)).enqueueMessage(any(Message.class), any(), anyInt());
        assert !clientsMap.containsKey(senderUsername);
    }

    @Test
    void testHandleLogoutWithoutClientAddress() {
        String senderUsername = "testUser";
        Message logoutMessage = new Message("LOGOUT", new Object[]{}, "REQUEST");

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        handler.handle(mockServer, logoutMessage, senderUsername);

        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
        assert !clientsMap.containsKey(senderUsername);
    }
}