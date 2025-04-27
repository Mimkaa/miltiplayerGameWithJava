package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

class SelectGoCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager; // ✅ NEW
    private Game mockGame;
    private GameObject mockGameObject;
    private SelectGoCommandHandler handler;
    private ConcurrentHashMap<String, InetSocketAddress> clientsMap;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class); // ✅ NEW
        mockGame = mock(Game.class);
        mockGameObject = mock(GameObject.class);
        handler = new SelectGoCommandHandler();
        clientsMap = new ConcurrentHashMap<>();

        when(mockServer.getClientsMap()).thenAnswer(invocation -> clientsMap);
        when(mockServer.getGameSessionManager()).thenReturn(mockGameSessionManager); // ✅ NEW
    }

    @Test
    void testHandle_ObjectFound_ShouldSendResponse() throws Exception {
        // Arrange
        String username = "testUser";
        String gameId = "game123";
        String objectName = "player1";
        String objectId = "object-uuid-123";

        Message incomingMessage = new Message("SELECTGO", new Object[]{gameId, objectName}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        clientsMap.put(username, socketAddress);

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame); // ✅ USE mockGameSessionManager
        when(mockGame.getGameObjects()).thenReturn(new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject)));
        when(mockGameObject.getName()).thenReturn(objectName);
        when(mockGameObject.getId()).thenReturn(objectId);

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert
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

    @Test
    void testHandle_ObjectNotFound_ShouldDoNothing() throws Exception {
        // Arrange
        String username = "testUser";
        String gameId = "game123";
        String objectName = "nonexistentObject";

        Message incomingMessage = new Message("SELECTGO", new Object[]{gameId, objectName}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        clientsMap.put(username, socketAddress);

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(new CopyOnWriteArrayList<>());

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert
        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }

    @Test
    void testHandle_GameSessionNotFound_ShouldDoNothing() throws Exception {
        // Arrange
        String username = "testUser";
        String gameId = "invalidGame";
        String objectName = "anyObject";

        Message incomingMessage = new Message("SELECTGO", new Object[]{gameId, objectName}, "COMMAND");

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 12345;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

        clientsMap.put(username, socketAddress);

        when(mockGameSessionManager.getGameSession(gameId)).thenReturn(null);

        // Act
        handler.handle(mockServer, incomingMessage, username);

        // Assert
        verify(mockServer, never()).enqueueMessage(any(Message.class), any(), anyInt());
    }
}
