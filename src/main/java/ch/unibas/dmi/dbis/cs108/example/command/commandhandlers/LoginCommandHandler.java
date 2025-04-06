package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "LOGIN" command, confirming the login by looking up a
 * game object's ID matching the provided name and returning it to the user.
 */
public class LoginCommandHandler implements CommandHandler {

    /**
     * Finds the {@link GameObject} with the specified name and responds with
     * the object's ID in a "LOGIN" response. If no object is found, the
     * parameters remain empty.
     *
     * @param server         the server holding the game instance and user mapping
     * @param msg            the "LOGIN" command
     * @param senderUsername the username of the client logging in
     */
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
