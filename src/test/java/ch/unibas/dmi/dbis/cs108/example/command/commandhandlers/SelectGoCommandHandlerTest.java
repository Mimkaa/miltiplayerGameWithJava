package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SelectGoCommandHandler}.
 * <p>
 * Verifies that SELECTGO commands correctly identify the target game object
 * and enqueue a RESPONSE message with the object's UUID, or ignore invalid requests.
 * </p>
 */
class SelectGoCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private Game mockGame;
    private GameObject mockGameObject;
    private SelectGoCommandHandler handler;
    private ConcurrentHashMap<String, InetSocketAddress> clientsMap;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(Game.class);
        mockGameObject = mock(GameObject.class);
        handler = new SelectGoCommandHandler();
        clientsMap = new ConcurrentHashMap<>();

        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager);
    }

    /**
     * Tests that when the specified game object exists, a RESPONSE message
     * containing its UUID is enqueued to the correct client.
     */
    @Test
    void testHandle_ObjectFound_ShouldSendResponse() throws Exception {
        String username = "testUser";
        String gameId = "game123";
        String objectName = "player1";
        String objectId = "object-uuid-123";

        Message incomingMessage = new Message("SELECTGO", new Object[]{gameId, objectName}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        clientsMap.put(username, socketAddress);

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(
                new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject)));
        when(mockGameObject.getName()).thenReturn(objectName);
        when(mockGameObject.getId()).thenReturn(objectId);

        handler.handle(mockServer, incomingMessage, username);

        verify(mockServer).enqueueMessage(
                argThat(msg ->
                        msg.getMessageType().equals("SELECTGO")
                                && msg.getOption().equals("RESPONSE")
                                && msg.getParameters()[0].equals(objectId)
                ),
                eq(address),
                eq(port)
        );
    }

    /**
     * Tests that when the target object does not exist, no message is enqueued.
     */
    @Test
    void testHandle_ObjectNotFound_ShouldDoNothing() throws Exception {
        String username = "testUser";
        String gameId = "game123";
        String objectName = "nonexistentObject";

        Message incomingMessage = new Message("SELECTGO", new Object[]{gameId, objectName}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        clientsMap.put(username, new InetSocketAddress(address, port));

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(new CopyOnWriteArrayList<>());

        handler.handle(mockServer, incomingMessage, username);

        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }

    /**
     * Tests that when the game session is not found, the command is ignored.
     */
    @Test
    void testHandle_GameSessionNotFound_ShouldDoNothing() throws Exception {
        String username = "testUser";
        String gameId = "invalidGame";
        String objectName = "anyObject";

        Message incomingMessage = new Message("SELECTGO", new Object[]{gameId, objectName}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        clientsMap.put(username, new InetSocketAddress(address, port));

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(null);

        handler.handle(mockServer, incomingMessage, username);

        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }
}