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
public class ChangeUsernameCommandHandler implements CommandHandler {

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] p = msg.getParameters();
        if (p == null || p.length < 2) {
            System.out.println("CHANGENAME request missing params");
            return;
        }
        String oldName = p[0].toString();
        String newName = p[1].toString().trim();
        if (newName.isEmpty()) {
            System.out.println("CHANGENAME: new name empty");
            return;
        }
        System.out.println(oldName);
        System.out.println(newName);

        boolean ok = server.renameUser(oldName, newName);

        /* build response */
        Message resp = new Message(
                "CHANGEUSERNAME",
                new Object[]{ ok ? "OK" : "FAIL", oldName, newName },
                "RESPONSE"
        );

        /* send only to the requester (could also broadcast if you wish) */
        InetSocketAddress dest = server.getClientsMap().get(
                ok ? newName : oldName      // lookup by whichever key exists
        );
        if (dest != null) {
            server.enqueueMessage(resp, dest.getAddress(), dest.getPort());
        }
        System.out.println("Connected users → " + server.getClientsMap().keySet());
        
    }
}
