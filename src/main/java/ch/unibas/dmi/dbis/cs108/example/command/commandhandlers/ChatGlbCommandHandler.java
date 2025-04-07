package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "CHATGLB" command, which represents a global chat message.
 * This command is expected to carry the sender's username and the message content.
 *
 * <p>When received, the message is broadcast to all other connected clients,
 * excluding the sender.</p>
 *
 * <p>If the message parameters are invalid or insufficient,
 * the message is ignored and a warning is printed to the console.</p>
 *
 * <p>The expected parameters are:</p>
 * <ol>
 *   <li><code>params[0]</code>: sender's username (String)</li>
 *   <li><code>params[1]</code>: chat message content (String)</li>
 * </ol>
 *
 * <p>The broadcasted message uses the same "CHATGLB" type and includes
 * the sender and message content.</p>
 */
public class ChatGlbCommandHandler implements CommandHandler {

    /**
     * Handles an incoming "CHATGLB" message by validating its parameters and
     * broadcasting it to all other clients.
     *
     * @param server          the server instance managing connections and broadcasting
     * @param msg             the incoming message of type "CHATGLB"
     * @param senderUsername  the username of the sender client
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {

        Object[] params = msg.getParameters();
        if (params == null || params.length < 2) {
            System.out.println("Invalid CHATGLB message from " + senderUsername);
            return;
        }

        String fromUser = params[0].toString();
        String message = params[1].toString();

        Message broadcastMsg = new Message("CHATGLB", new String[]{fromUser, message}, "RESPONSE");
        //server.broadcastMessageToAll(broadcastMsg);
        server.broadcastMessageToOthers(broadcastMsg, senderUsername);
        System.out.println("Broadcasted CHATGLB message: " + fromUser + ": " + message);
    }
}
