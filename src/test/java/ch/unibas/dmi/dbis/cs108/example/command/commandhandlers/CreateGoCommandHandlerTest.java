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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateGoCommandHandlerTest extends BaseTest {

    @Mock Server                 server;
    @Mock GameSessionManager     gsm;
    @Mock Game                   fakeGame;
    @Mock GameObject             fakeObj;
    @Captor ArgumentCaptor<Message> msgCap;

    private CreateGoCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateGoCommandHandler();

        // 1) Server → GameSessionManager → fakeGame
        when(server.getGameSessionManager()).thenReturn(gsm);
        when(gsm.getGameSession("sess1")).thenReturn(fakeGame);

        // 1a) game.getGameObjects() darf nicht null sein
        when(fakeGame.getGameObjects())
                .thenReturn(new CopyOnWriteArrayList<>());

        // 1b) getAllGameSessions() darf nicht null sein
        when(gsm.getAllGameSessions())
                .thenReturn(new ConcurrentHashMap<>());  // leere Map

        // 2) Sorge dafür, dass getClientsMap() nie null ist.
        ConcurrentHashMap<String, InetSocketAddress> clients = new ConcurrentHashMap<>();
        clients.put("alice", new InetSocketAddress("127.0.0.1", 12345));
        when(server.getClientsMap()).thenReturn(clients);

        // 3) simuliere, dass noch kein Objekt mit Namen „Alice“ existiert
        when(fakeGame.containsObjectByName("Alice")).thenReturn(false);

        // 4) stubpe das async-Add, damit es sofort unser fakeObj liefert
        when(fakeGame.addGameObjectAsync(
                eq("Player"),
                anyString(),
                any(Object[].class)
        )).thenReturn(CompletableFuture.completedFuture(fakeObj));

        // 5) stub für fakeObj.getName()
        when(fakeObj.getName()).thenReturn("Alice");
    }

    @Test
    void testHandle_withTooFewParameters_doesNothing() {
        Message bad = new Message("CREATEGO", new Object[]{"sess1"}, "REQUEST");
        handler.handle(server, bad, "alice");
        verify(server, never()).broadcastMessageToAll(any());
    }

    @Test
    void testHandle_validRequest_createsAndBroadcasts() {
        Message req = new Message(
                "CREATEGO",
                new Object[]{"sess1", "Player", "Alice"},
                "REQUEST",
                new String[]{"alice"}
        );

        handler.handle(server, req, "alice");

        // verify we asked the fakeGame to create a new obj
        verify(fakeGame).addGameObjectAsync(
                eq("Player"),
                anyString(),
                any(Object[].class)
        );

        // capture the broadcasted response
        verify(server).broadcastMessageToAll(msgCap.capture());
        Message bc = msgCap.getValue();

        assertEquals("CREATEGO", bc.getMessageType());
        assertEquals("RESPONSE", bc.getOption());

        Object[] params = bc.getParameters();
        // params[0] = generated UUID
        assertEquals("sess1", params[1],  "second param should be sessionId");
        assertEquals("Player", params[2], "third param should be type");
        assertEquals("Alice", params[3],  "fourth param should be ctor-arg name");
    }
}
