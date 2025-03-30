package ch.unibas.dmi.dbis.cs108.example.message;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import java.net.InetSocketAddress;

/**
 * A common interface for all distinct message-type handlers:
 * ACK, CHAT, REQUEST, GAME, etc.
 */
public interface MessageHandler {
    /**
     * Handle the given message from the specified sender.
     */
    void handle(Server server, Message msg, InetSocketAddress senderSocket);
}