package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * LOGIN: confirm the login and respond with the object's ID.
 */
public class LoginCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        if (server.getMyGameInstance() == null) {
            System.err.println("No game instance to handle LOGIN.");
            return;
        }
        String firstParam = msg.getParameters()[0].toString();
        Object[] newParams = new Object[1];

        for (GameObject gameObject : server.getMyGameInstance().getGameObjects()) {
            if (firstParam.equals(gameObject.getName())) {
                newParams[0] = gameObject.getId();
                break;
            }
        }

        Message loginMessage = new Message("LOGIN", newParams, "RESPONSE");
        InetSocketAddress clientAddr = server.getClientsMap().get(senderUsername);
        if (clientAddr != null) {
            server.enqueueMessage(loginMessage, clientAddr.getAddress(), clientAddr.getPort());
        }
    }
}