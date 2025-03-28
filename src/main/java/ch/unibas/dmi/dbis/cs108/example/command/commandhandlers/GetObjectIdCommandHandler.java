package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * GETOBJECTID: respond with the ID of a game object with the given name.
 */
public class GetObjectIdCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        if (server.getMyGameInstance() == null) {
            System.err.println("No game instance to search for objects.");
            return;
        }
        Object[] originalParams = msg.getParameters();
        String objectName = originalParams[0].toString();
        String objectID = "";

        for (GameObject gameObject : server.getMyGameInstance().getGameObjects()) {
            if (gameObject.getName().equals(objectName)) {
                objectID = gameObject.getId();
                break;
            }
        }

        Message responseMsg = new Message("GETOBJECTID", new Object[]{objectID}, "RESPONSE");
        server.broadcastMessageToAll(responseMsg);
    }
}