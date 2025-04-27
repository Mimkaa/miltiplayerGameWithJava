package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

class GetObjectIdCommandHandlerTest {

    private Server mockServer;
    private Game mockGame;
    private GetObjectIdCommandHandler handler;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        mockGame = mock(Game.class);
        handler = new GetObjectIdCommandHandler();
    }

    @Test
    void testHandle_NoGameInstance_ShouldNotBroadcast() {
        // Arrange
        Message incomingMessage = new Message("GETOBJECTID", new Object[]{"TestObject"}, "COMMAND");

        when(mockServer.getMyGameInstance()).thenReturn(null);

        // Act
        handler.handle(mockServer, incomingMessage, "testUser");

        // Assert
        verify(mockServer, never()).broadcastMessageToAll(any(Message.class));
    }

    @Test
    void testHandle_GameInstanceWithMatchingObject_ShouldBroadcastCorrectId() {
        // Arrange
        GameObject mockGameObject = mock(GameObject.class);
        when(mockGameObject.getName()).thenReturn("TestObject");
        when(mockGameObject.getId()).thenReturn("ObjectId123");

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject));

        Message incomingMessage = new Message("GETOBJECTID", new Object[]{"TestObject"}, "COMMAND");

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(gameObjects);

        // Act
        handler.handle(mockServer, incomingMessage, "testUser");

        // Assert
        verify(mockServer).broadcastMessageToAll(argThat(msg ->
                msg.getMessageType().equals("GETOBJECTID")
                        && msg.getParameters()[0].equals("ObjectId123")
                        && msg.getOption().equals("RESPONSE")
        ));
    }

    @Test
    void testHandle_GameInstanceWithoutMatchingObject_ShouldBroadcastEmptyId() {
        // Arrange
        GameObject mockGameObject = mock(GameObject.class);
        when(mockGameObject.getName()).thenReturn("AnotherObject");

        CopyOnWriteArrayList<GameObject> gameObjects = new CopyOnWriteArrayList<>(Collections.singletonList(mockGameObject));

        Message incomingMessage = new Message("GETOBJECTID", new Object[]{"TestObject"}, "COMMAND");

        when(mockServer.getMyGameInstance()).thenReturn(mockGame);
        when(mockGame.getGameObjects()).thenReturn(gameObjects);

        // Act
        handler.handle(mockServer, incomingMessage, "testUser");

        // Assert
        verify(mockServer).broadcastMessageToAll(argThat(msg ->
                msg.getMessageType().equals("GETOBJECTID")
                        && msg.getParameters()[0].equals("")
                        && msg.getOption().equals("RESPONSE")
        ));
    }
}
