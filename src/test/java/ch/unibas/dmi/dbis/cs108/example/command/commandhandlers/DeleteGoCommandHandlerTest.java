package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Unit tests for {@link DeleteGoCommandHandler}.
 */
class DeleteGoCommandHandlerTest {

    @Mock Server server;
    @Mock GameSessionManager gsm;

    DeleteGoCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new DeleteGoCommandHandler();
        when(server.getGameSessionManager()).thenReturn(gsm);
    }

    @Test
    void testHandle_withTooFewParameters_doesNothing() {
        Message msg = new Message(
                "DELETEGO",
                new Object[]{ "onlySession" },
                "REQUEST",
                new String[]{ "bob" }
        );

        handler.handle(server, msg, "bob");

        verify(server, never()).broadcastMessageToAll(any());
    }

    @Test
    void testHandle_withUnknownSession_doesNothing() {
        String sess = "sessX", obj = "obj1";
        Message msg = new Message(
                "DELETEGO",
                new Object[]{ sess, obj },
                "REQUEST",
                new String[]{ "alice" }
        );

        when(gsm.getGameSession(sess)).thenReturn(null);

        handler.handle(server, msg, "alice");

        verify(server, never()).broadcastMessageToAll(any());
    }

    @Test
    void testHandle_validRequest_removesAndBroadcasts() {
        String sess = "sess1", obj = "obj1";

        // 1) prepare the incoming DELETEGO request
        Message msg = new Message(
                "DELETEGO",
                new Object[]{ sess, obj },
                "REQUEST",
                new String[]{ "charlie" }
        );

        // 2) stub out session lookup
        Game mockGame = mock(Game.class);
        when(gsm.getGameSession(sess)).thenReturn(mockGame);

        // 3) use CopyOnWriteArrayList for stubbing getGameObjects()
        CopyOnWriteArrayList<GameObject> objects = new CopyOnWriteArrayList<>();
        GameObject go1 = mock(GameObject.class);
        when(go1.getId()).thenReturn("obj1");
        GameObject go2 = mock(GameObject.class);
        when(go2.getId()).thenReturn("obj2");
        objects.add(go1);
        objects.add(go2);

        when(mockGame.getGameObjects()).thenReturn(objects);

        // ACT
        handler.handle(server, msg, "charlie");

        // ASSERT removal
        assertFalse(objects.stream().anyMatch(o -> "obj1".equals(o.getId())),
                "object obj1 should have been removed");

        // ASSERT broadcast
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message> cap = ArgumentCaptor.forClass(Message.class);
        verify(server).broadcastMessageToAll(cap.capture());
        Message bc = cap.getValue();
        assertEquals("DELETEGO", bc.getMessageType());
        assertArrayEquals(new Object[]{ sess, obj }, bc.getParameters(),
                "Broadcast should carry the original parameters");
    }
}
