package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.InetSocketAddress;

/**
 * Handles "CHAT" messages, optionally sending an ACK back to the client and then
 * broadcasting the chat text to all connected clients.
 */
public class ChatMessageHandler implements MessageHandler {

    /**
     * Required no-arg constructor for reflection-based discovery.
     */
    public ChatMessageHandler() {
        // ...
    }

    /**
     * Ensures the {@code ChatManager} is present, optionally sends an ACK if the
     * message has a UUID, and broadcasts the chat to all clients asynchronously.
     *
     * @param server       the server instance managing chat state
     * @param msg          the incoming "CHAT" message
     * @param senderSocket the network address of the message sender
     */
    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // 1) Initialize ChatManager if needed
        if (server.getServerChatManager() == null) {
            server.setServerChatManager(new ChatManager.ServerChatManager());
        }

        // 2) Send ACK if there's a UUID
        if (msg.getUUID() != null && !msg.getUUID().isEmpty()) {
            server.getAckProcessor().addAck(senderSocket, msg.getUUID());
        }

        // 3) Broadcast chat
        System.out.println("Processed CHAT message from " + senderSocket);
        AsyncManager.run(() -> server.broadcastMessageToAll(msg));
    }
}
