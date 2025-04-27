package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

class UserJoinedCommandHandlerTest {

    private Server mockServer;
    private UserJoinedCommandHandler handler;
    private ConcurrentHashMap<String, InetSocketAddress> clientsMap;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        handler = new UserJoinedCommandHandler();
        clientsMap = new ConcurrentHashMap<>();

        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
    }

    @Test
    void testHandle_NicknameAlreadyTaken_ShouldSuggestNewNickname() throws Exception {
        // Arrange
        String existingNickname = "testUser";
        Message incomingMessage = new Message("USERJOINED", new Object[]{existingNickname}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        // Simulate that the nickname is already taken
        clientsMap.put(existingNickname, socketAddress);

        // Mock Nickname_Generator to return a specific nickname suggestion
        try (MockedStatic<Nickname_Generator> mockedGenerator = mockStatic(Nickname_Generator.class)) {
            mockedGenerator.when(Nickname_Generator::generateNickname).thenReturn("suggestedUser123");

            // Act
            handler.handle(mockServer, incomingMessage, "testUser");

            // Assert
            verify(mockServer).enqueueMessage(
                    argThat(msg ->
                            msg.getMessageType().equals("USERJOINED")
                                    && msg.getOption().equals("RESPONSE")
                                    && msg.getParameters()[0].equals("suggestedUser123")
                    ),
                    eq(address),
                    eq(port)
            );
        }
    }

    @Test
    void testHandle_NicknameAvailable_ShouldDoNothing() throws Exception {
        // Arrange
        String newNickname = "availableUser";
        Message incomingMessage = new Message("USERJOINED", new Object[]{newNickname}, "COMMAND");

        // The clientsMap is empty, meaning the nickname is available

        // Act
        handler.handle(mockServer, incomingMessage, "availableUser");

        // Assert
        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }
}
