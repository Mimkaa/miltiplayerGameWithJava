package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * Handles the "GETOBJECTID" command, which returns the unique ID of a
 * specific game object (by name) in the current {@code myGameInstance}.
 */
public class GetObjectIdCommandHandler implements CommandHandler {

    /**
     * Searches the game instance for an object matching the requested name,
     * then broadcasts its ID as a "GETOBJECTID" response. If no instance exists,
     * logs an error.
     *
     * @param server         the server that holds the game instance
     * @param msg            the incoming "GETOBJECTID" command
     * @param senderUsername the username of the client requesting the ID
     */
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
