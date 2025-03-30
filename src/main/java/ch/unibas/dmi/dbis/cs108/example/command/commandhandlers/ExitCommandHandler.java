package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "EXIT" command, which fully terminates a client's session
 * and removes the user from the server's {@code clientsMap}.
 */
public class ExitCommandHandler implements CommandHandler {

    /**
     * Required no-argument constructor for reflection-based discovery.
     */
    public ExitCommandHandler() {
        // ...
    }

    /**
     * Sends an "EXIT" response to the user, removes them from the {@code clientsMap},
     * and logs the event.
     *
     * @param server         the server instance handling the user session
     * @param msg            the incoming "EXIT" command
     * @param senderUsername the username of the client who wants to exit
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        String commandType = "EXIT";  // Hard-coded because this class is specifically for EXIT
        System.out.println("Client " + commandType + ": " + senderUsername);

        Message exitMessage = new Message(commandType, msg.getParameters(), "RESPONSE");
        InetSocketAddress clientAddress = server.getClientsMap().get(senderUsername);
        if (clientAddress != null) {
            server.enqueueMessage(exitMessage, clientAddress.getAddress(), clientAddress.getPort());
        }

        // Remove user
        server.getClientsMap().remove(senderUsername);
        System.out.println("Removed user: " + senderUsername);
    }
}
