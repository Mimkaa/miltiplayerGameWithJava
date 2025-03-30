package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "CHANGENAME" command, which renames a game object and updates
 * the internal {@code clientsMap} if necessary. Afterwards, it broadcasts
 * the change to all connected clients.
 */
public class ChangeNameCommandHandler implements CommandHandler {

    /**
     * Renames an existing game object (if found) to a unique name,
     * updates the {@code clientsMap}, and broadcasts the updated
     * name information to all users.
     *
     * @param server         the server instance providing access to game state, etc.
     * @param msg            the incoming "CHANGENAME" request
     * @param senderUsername the username of the client sending this command
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] originalParams = msg.getParameters();
        String oldName = originalParams[0].toString();
        String requestedName = originalParams[1].toString();

        // Ensure the new name is unique
        String newName = server.findUniqueName(requestedName);

        // Update the game object
        String objectID = "";
        if (server.getMyGameInstance() != null) {
            for (GameObject gameObject : server.getMyGameInstance().getGameObjects()) {
                if (gameObject.getName().equals(oldName)) {
                    objectID = gameObject.getId();
                    gameObject.setName(newName);
                    break;
                }
            }
        }

        // Update the clientsMap key (oldName -> newName)
        InetSocketAddress address = server.getClientsMap().remove(oldName);
        if (address != null) {
            server.getClientsMap().put(newName, address);
        }

        // Broadcast the name change
        Message responseMsg = new Message(
                "CHANGENAME",
                new Object[]{objectID, newName},
                "RESPONSE"
        );
        server.broadcastMessageToAll(responseMsg);
    }
}
