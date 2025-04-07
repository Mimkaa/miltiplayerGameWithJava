package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "GETUSERS" command from a client.
 * <p>
 * This command allows a client to request a list of all currently connected usernames.
 * The server responds with a message containing all keys from its client map.
 * </p>
 */
public class GetUsersCommandHandler implements CommandHandler {

    /**
     * Processes the GETUSERS request by looking up the sender's address,
     * retrieving all usernames (as keys) from the server's client map,
     * and sending them back in a "GETUSERS" response message.
     *
     * @param server the server handling the request
     * @param msg the incoming message object (expected type: GETUSERS)
     * @param senderUsername the username of the client who sent the request
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
        if (senderAddress != null) {

            Object[] keyArray = server.getClientsMap().keySet().toArray(new Object[0]);

            Message responseMsg = new Message("GETUSERS", keyArray, "RESPONSE");
            server.enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            // System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }
}
