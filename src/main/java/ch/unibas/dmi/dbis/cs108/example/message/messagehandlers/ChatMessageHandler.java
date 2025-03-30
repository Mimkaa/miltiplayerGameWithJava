package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;


import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.InetSocketAddress;

public class ChatMessageHandler implements MessageHandler {

    public ChatMessageHandler() {
    }

    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // 1) ChatManager initialisieren (wie in processMessage)
        if (server.getServerChatManager() == null) {
            server.setServerChatManager(new ChatManager.ServerChatManager());
        }

        // 2) ACK verschicken?
        if (msg.getUUID() != null && !msg.getUUID().isEmpty()) {
            server.getAckProcessor().addAck(senderSocket, msg.getUUID());
        }

        // 3) Nachricht an alle broadcasten
        System.out.println("Processed CHAT message from " + senderSocket);
        AsyncManager.run(() -> server.broadcastMessageToAll(msg));
    }
}