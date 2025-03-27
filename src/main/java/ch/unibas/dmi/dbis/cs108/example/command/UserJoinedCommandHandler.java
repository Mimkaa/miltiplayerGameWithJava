package ch.unibas.dmi.dbis.cs108.example.command;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import java.net.InetSocketAddress;

/**
 * USERJOINED: checks if there's a name conflict, if so, suggests a new name.
 */
public class UserJoinedCommandHandler implements CommandHandler {
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