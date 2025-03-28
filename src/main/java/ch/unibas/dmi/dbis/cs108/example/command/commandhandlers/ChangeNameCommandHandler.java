package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * CHANGENAME: rename a game object (and update clientsMap).
 */
public class ChangeNameCommandHandler implements CommandHandler {
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

        // Update the clientsMap key
        InetSocketAddress address = server.getClientsMap().remove(oldName);
        if (address != null) {
            server.getClientsMap().put(newName, address);
        }

        // Broadcast the change
        Message responseMsg = new Message(
                "CHANGENAME",
                new Object[]{objectID, newName},
                "RESPONSE"
        );
        server.broadcastMessageToAll(responseMsg);
    }
}
