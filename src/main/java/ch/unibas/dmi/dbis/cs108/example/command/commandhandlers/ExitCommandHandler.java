package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * EXIT: fully terminate client session, respond, etc.
 */
public class ExitCommandHandler implements CommandHandler {

    public ExitCommandHandler() {
        // Wichtig: parameterloser Konstruktor.
    }

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        String commandType = "EXIT";  // Fest verdrahtet auf Exit
        System.out.println("Client " + commandType + ": " + senderUsername);

        Message exitMessage = new Message(commandType, msg.getParameters(), "RESPONSE");
        InetSocketAddress clientAddress = server.getClientsMap().get(senderUsername);
        if (clientAddress != null) {
            server.enqueueMessage(exitMessage, clientAddress.getAddress(), clientAddress.getPort());
        }

        // Entferne den User aus dem Map
        server.getClientsMap().remove(senderUsername);
        System.out.println("Removed user: " + senderUsername);
    }

}
