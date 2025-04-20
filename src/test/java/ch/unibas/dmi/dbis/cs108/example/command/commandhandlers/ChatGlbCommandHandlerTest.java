package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ChatGlbCommandHandlerTest {

    @Test
    public void testMessageIsBroadcasted() {
        Server mockServer = mock(Server.class);
        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();
        String sender = "Sena";
        String content = "Hello world!";
        Message incomingMessage = new Message("CHATGLB", new String[]{sender, content}, "REQUEST");
        handler.handle(mockServer, incomingMessage, sender);
        verify(mockServer, times(1)).broadcastMessageToOthers(any(Message.class), eq(sender));
    }

    @Test
    public void testInvalidMessageIsNotBroadcasted() {
        Server mockServer = mock(Server.class);
        ChatGlbCommandHandler handler = new ChatGlbCommandHandler();
        Message invalidMessage = new Message("CHATGLB", new String[]{}, "REQUEST");
        handler.handle(mockServer, invalidMessage, "Sena");
        verify(mockServer, never()).broadcastMessageToOthers(any(), anyString());
    }
}
