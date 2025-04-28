// src/test/java/ch/unibas/dmi/dbis/cs108/example/command/commandhandlers/CreateGoCommandHandlerTest.java
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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateGoCommandHandlerTest extends BaseTest {

    @Mock Server               server;
    @Mock GameSessionManager   gsm;
    @Mock Game                 fakeGame;
    @Mock GameObject           fakeObj;
    @Captor ArgumentCaptor<Message> msgCap;

    private CreateGoCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateGoCommandHandler();

        // wire up server → gsm → fakeGame
        when(server.getGameSessionManager()).thenReturn(gsm);
        when(gsm.getGameSession("sess1")).thenReturn(fakeGame);

        // simulate no duplicate name
        when(fakeGame.containsObjectByName("Alice")).thenReturn(false);

        // stub out async creation to immediately return our fakeObj
        when(fakeGame.addGameObjectAsync(
                eq("Player"),
                anyString(),
                any(Object[].class)        // match any var-args array
        )).thenReturn(CompletableFuture.completedFuture(fakeObj));
    }

    @Test
    void testHandle_withTooFewParameters_doesNothing() {
        // only one param → should bail out before broadcast
        Message bad = new Message("CREATEGO", new Object[]{ "sess1" }, "REQUEST");
        handler.handle(server, bad, "alice");
        verify(server, never()).broadcastMessageToAll(any());
    }

    @Test
    void testHandle_validRequest_createsAndBroadcasts() {
        // prepare a valid CREATEGO request
        Message req = new Message(
                "CREATEGO",
                new Object[]{ "sess1", "Player", "Alice" },
                "REQUEST",
                new String[]{ "alice" }
        );

        // fakeObj.getName() must be stubbed so handler can log it
        when(fakeObj.getName()).thenReturn("Alice");

        // invoke
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
