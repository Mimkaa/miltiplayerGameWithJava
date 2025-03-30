package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "PING" command, responding with "PONG" to confirm connectivity.
 */
public class PingCommandHandler implements CommandHandler {

    /**
     * Looks up the sender's address and responds with a "PONG" message.
     *
     * @param server         the server instance
     * @param msg            the "PING" command message
     * @param senderUsername the username of the client who sent the ping
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
        if (senderAddress != null) {
            Message responseMsg = new Message("PONG", new Object[]{}, "RESPONSE");
            server.enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }
}
