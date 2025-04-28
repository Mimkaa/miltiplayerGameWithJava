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

/**
 * Unit tests for {@link UserJoinedCommandHandler}.
 * <p>
 * These tests verify that when a new user attempts to join:
 * <ul>
 *   <li>If the chosen nickname is already taken, the handler suggests a new one.</li>
 *   <li>If the nickname is available, no response is sent.</li>
 * </ul>
 * </p>
 */
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

    /**
     * Verifies that when a user attempts to join with a nickname
     * that's already in use, the handler uses {@link Nickname_Generator}
     * to generate and return a suggested alternative.
     */
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

    /**
     * Verifies that when a user joins with a nickname that's not yet taken,
     * the handler does nothing (no response is enqueued).
     */
    @Test
    void testHandle_NicknameAvailable_ShouldDoNothing() throws Exception {
        // Arrange
        String newNickname = "availableUser";
        Message incomingMessage = new Message("USERJOINED", new Object[]{newNickname}, "COMMAND");

        // The clientsMap is empty, so the nickname is available

        // Act
        handler.handle(mockServer, incomingMessage, "availableUser");

        // Assert
        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }
}
