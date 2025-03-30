package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * LOGOUT: remove user, respond, etc.
 */
public class LogoutCommandHandler implements CommandHandler {

    public LogoutCommandHandler() {
        // Ein no-arg Konstruktor ist wichtig für Reflection.
    }

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        String commandType = "LOGOUT";  // Fest verdrahtet, da es sich um Logout handelt
        System.out.println("Client " + commandType + ": " + senderUsername);

        // Sende ein "LOGOUT" RESPONSE
        Message logoutMessage = new Message(commandType, msg.getParameters(), "RESPONSE");
        InetSocketAddress clientAddress = server.getClientsMap().get(senderUsername);
        if (clientAddress != null) {
            server.enqueueMessage(logoutMessage, clientAddress.getAddress(), clientAddress.getPort());
        }

        // Entferne den User aus dem Map
        server.getClientsMap().remove(senderUsername);
        System.out.println("Removed user: " + senderUsername);
    }

}