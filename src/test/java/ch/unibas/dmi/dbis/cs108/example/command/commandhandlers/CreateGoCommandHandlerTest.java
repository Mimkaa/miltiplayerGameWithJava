package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.BaseTest;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CreateGoCommandHandler}.
 * <p>
 * Verifies that CREATEGO commands are correctly handled by:
 * <ul>
 *   <li>Ignoring requests with too few parameters</li>
 *   <li>Creating game objects via the GameSessionManager</li>
 *   <li>Broadcasting a RESPONSE message with the new object's details</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateGoCommandHandlerTest extends BaseTest {

    @Mock Server                 server;
    @Mock GameSessionManager     gsm;
    @Mock Game                   fakeGame;
    @Mock GameObject             fakeObj;
    @Captor ArgumentCaptor<Message> msgCap;

    private CreateGoCommandHandler handler;

    /**
     * Sets up the handler and mocks before each test.
     * <p>
     * Stubs GameSessionManager, Game.getGameObjects(), getAllGameSessions(),
     * Server.getClientsMap(), Game.containsObjectByName(), and async creation.
     * </p>
     */
    @BeforeEach
    void setUp() {
        handler = new CreateGoCommandHandler();

        when(server.getGameSessionManager()).thenReturn(gsm);
        when(gsm.getGameSession("sess1")).thenReturn(fakeGame);
        when(fakeGame.getGameObjects()).thenReturn(new CopyOnWriteArrayList<>());
        when(gsm.getAllGameSessions()).thenReturn(new ConcurrentHashMap<>());

        ConcurrentHashMap<String, InetSocketAddress> clients = new ConcurrentHashMap<>();
        clients.put("alice", new InetSocketAddress("127.0.0.1", 12345));
        when(server.getClientsMap()).thenReturn(clients);

        when(fakeGame.containsObjectByName("Alice")).thenReturn(false);
        when(fakeGame.addGameObjectAsync(
                eq("Player"), anyString(), any(Object[].class)
        )).thenReturn(CompletableFuture.completedFuture(fakeObj));
        when(fakeObj.getName()).thenReturn("Alice");
    }

    /**
     * Tests that a CREATEGO message with too few parameters is ignored.
     */
    @Test
    void testHandle_withTooFewParameters_doesNothing() {
        Message bad = new Message("CREATEGO", new Object[]{"sess1"}, "REQUEST");
        handler.handle(server, bad, "alice");
        verify(server, never()).broadcastMessageToAll(any());
    }

    /**
     * Tests that a valid CREATEGO request triggers async creation and broadcasts the RESPONSE.
     */
    @Test
    void testHandle_validRequest_createsAndBroadcasts() {
        Message req = new Message(
                "CREATEGO",
                new Object[]{"sess1", "Player", "Alice"},
                "REQUEST",
                new String[]{"alice"}
        );

        handler.handle(server, req, "alice");

        // verify asynchronous creation
        verify(fakeGame).addGameObjectAsync(
                eq("Player"), anyString(), any(Object[].class)
        );

        // capture and assert broadcast message
        verify(server).broadcastMessageToAll(msgCap.capture());
        Message bc = msgCap.getValue();

        assertEquals("CREATEGO", bc.getMessageType());
        assertEquals("RESPONSE", bc.getOption());

        Object[] params = bc.getParameters();
        assertEquals("sess1", params[1],  "second param should be sessionId");
        assertEquals("Player", params[2], "third param should be type");
        assertEquals("Alice", params[3],  "fourth param should be ctor-arg name");
    }
}
