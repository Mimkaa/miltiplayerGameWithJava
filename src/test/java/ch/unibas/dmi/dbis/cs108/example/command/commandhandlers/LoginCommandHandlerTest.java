package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

class LoginCommandHandlerTest {

    private Server mockServer;
    private GameSessionManager mockGameSessionManager;
    private ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game mockGame;
    private LoginCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGameSessionManager = mock(GameSessionManager.class);
        mockGame = mock(ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game.class);
        handler = new LoginCommandHandler();

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
    }

    @Test
    void testLoginFindsGameObjectAndResponds() {
        String senderUsername = "testUser";
        String objectName = "player1";
        String objectId = "object-123";

        GameObject mockGameObject = mock(GameObject.class);
        when(mockGameObject.getName()).thenReturn(objectName);
        when(mockGameObject.getId()).thenReturn(objectId);

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
        gameObjects.add(mockGameObject);
        when(mockGame.getGameObjects()).thenReturn(gameObjects);

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        Message loginMessage = new Message("LOGIN", new Object[]{objectName}, "REQUEST");

        handler.handle(mockServer, loginMessage, senderUsername);

        verify(mockServer, times(1)).enqueueMessage(any(Message.class), any(), anyInt());
    }

    @Test
    void testLoginNoMatchingGameObject() {
        String senderUsername = "testUser";
        String nonExistingName = "ghost";

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>();
        when(mockGame.getGameObjects()).thenReturn(gameObjects);

        ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
        clientsMap.put(senderUsername, new InetSocketAddress("127.0.0.1", 12345));
        when(mockServer.getClientsMap()).thenReturn(clientsMap);

        Message loginMessage = new Message("LOGIN", new Object[]{nonExistingName}, "REQUEST");

        handler.handle(mockServer, loginMessage, senderUsername);

        verify(mockServer, times(1)).enqueueMessage(any(Message.class), any(), anyInt());
    }

    @Test
    void testLoginNoGameInstance() {
        String senderUsername = "testUser";
        Message loginMessage = new Message("LOGIN", new Object[]{"any"}, "REQUEST");

        when(mockServer.getMyGameInstance()).thenReturn(null);

        handler.handle(mockServer, loginMessage, senderUsername);

        verify(mockServer, never()).enqueueMessage(any(), any(), anyInt());
    }
}
