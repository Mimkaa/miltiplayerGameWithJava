package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "USERJOINED" command, which checks for a nickname conflict
 * and, if necessary, suggests an alternative nickname.
 */
public class UserJoinedCommandHandler implements CommandHandler {

    /**
     * Checks if the requested nickname is already in use. If so, it sends
     * a "USERJOINED" response with a newly generated nickname suggestion.
     *
     * @param server         the server instance
     * @param msg            the "USERJOINED" command containing the desired nickname
     * @param senderUsername the username of the client
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        String nickname = msg.getParameters()[0].toString();
        boolean hasRepetition = server.getClientsMap().containsKey(nickname);

        if (hasRepetition) {
            String suggestedNickname = Nickname_Generator.generateNickname();
            Message responseMsg = new Message("USERJOINED", new Object[]{suggestedNickname}, "RESPONSE");
            InetSocketAddress clientAddress = server.getClientsMap().get(nickname);
            if (clientAddress != null) {
                server.enqueueMessage(responseMsg, clientAddress.getAddress(), clientAddress.getPort());
            }
        }
    }
}
