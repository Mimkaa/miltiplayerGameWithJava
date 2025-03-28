package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * LOGOUT or EXIT: remove user, respond, etc.
 */
public class LogoutOrExitCommandHandler implements CommandHandler {
    private final String commandType;
    public LogoutOrExitCommandHandler(String commandType) {
        this.commandType = commandType;
    }

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        System.out.println("Client " + commandType + ": " + senderUsername);

        Message logoutMessage = new Message(commandType, msg.getParameters(), "RESPONSE");
        InetSocketAddress clientAddress = server.getClientsMap().get(senderUsername);
        if (clientAddress != null) {
            server.enqueueMessage(logoutMessage, clientAddress.getAddress(), clientAddress.getPort());
        }

        // Remove the user from the map
        server.getClientsMap().remove(senderUsername);
        System.out.println("Removed user: " + senderUsername);
    }

}
