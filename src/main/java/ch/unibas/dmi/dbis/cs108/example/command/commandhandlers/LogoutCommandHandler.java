package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "LOGOUT" command, removing the user from the {@code clientsMap}
 * and sending a response message confirming the logout.
 */
public class LogoutCommandHandler implements CommandHandler {

    /**
     * Required no-arg constructor for reflection-based discovery.
     */
    public LogoutCommandHandler() {
        // ...
    }

    /**
     * Sends a "LOGOUT" response to the user and removes them from the server's
     * {@code clientsMap}, effectively terminating their session.
     *
     * @param server         the server handling the logout
     * @param msg            the "LOGOUT" command
     * @param senderUsername the username of the client who is logging out
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        String commandType = "LOGOUT";
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
