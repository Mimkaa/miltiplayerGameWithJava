package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.BestEffortBroadcastManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegisterCommandHandlerTest {

    private Server mockServer;
    private RegisterCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new RegisterCommandHandler();

        when(mockServer.getClientsMap())
                .thenReturn(new ConcurrentHashMap<String, InetSocketAddress>());
        when(mockServer.getLastSender())
                .thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.findUniqueName(anyString()))
                .thenReturn("assignedUser");
        when(mockServer.getBestEffortBroadcastManager())
                .thenReturn(mock(BestEffortBroadcastManager.class));
    }

    @Test
    void testHandleEnqueuesResponseWithResponseOption() {
        // Arrange
        String originalUsername = "testUser";
        Message registerMessage = new Message(
                "REGISTER",
                new Object[]{originalUsername},
                "REQUEST"
        );

        // Stub Server.makeResponse to supply at least 3 concealed params
        try (MockedStatic<Server> serverStatic = mockStatic(Server.class)) {
            serverStatic
                    .when(() -> Server.makeResponse(any(Message.class), any(Object[].class)))
                    .thenAnswer(invocation -> {
                        Object[] params = invocation.getArgument(1);
                        // Create a fake response with three concealed entries
                        Message fakeResponse = new Message(
                                /* type: */ "REGISTER",
                                /* params: */ invocation.getArgument(1),
                                /* option: */ invocation.getArgument(0, Message.class).getOption()
                        );
                        fakeResponse.setConcealedParameters(new String[]{
                                params[0].toString(),
                                "dummy2",
                                "dummy3"
                        });
                        return fakeResponse;
                    });

            // Act
            handler.handle(mockServer, registerMessage, originalUsername);
        }

        // Assert
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<InetAddress> addrCaptor = ArgumentCaptor.forClass(InetAddress.class);
        ArgumentCaptor<Integer> portCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(mockServer, times(1))
                .enqueueMessage(msgCaptor.capture(), addrCaptor.capture(), portCaptor.capture());

        Message responseMsg = msgCaptor.getValue();
        assertEquals("RESPONSE", responseMsg.getOption(),
                "Enqueued message should have option RESPONSE");
        assertArrayEquals(new String[]{"assignedUser"}, responseMsg.getConcealedParameters(),
                "Enqueued message should contain only the assigned username");

        InetSocketAddress expected = mockServer.getLastSender();
        assertEquals(expected.getAddress(), addrCaptor.getValue());
        assertEquals(expected.getPort(), portCaptor.getValue());
    }
}
