package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

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
        if (originalParams == null || originalParams.length < 3) {
            System.err.println("CHANGENAME request missing parameters. Expected: [gameSessionId, objectUUID, newName]");
            return;
        }

        // Parameter 0: game session ID
        String gameSessionId = originalParams[0].toString();
        // Parameter 1: game object's UUID
        String objectId = originalParams[1].toString();
        // Parameter 2: requested new name
        String requestedName = originalParams[2].toString();

        // Retrieve the game session from the manager.
        Game game = server.getGameSessionManager().getGameSession(gameSessionId);
        if (game == null) {
            System.err.println("No game session found with id: " + gameSessionId);
            return;
        }

        // Determine a unique new name.
        String newName = server.findUniqueName(requestedName);

        // Find the game object by its UUID and update its name.
        String foundId = "";
        for (GameObject gameObject : game.getGameObjects()) {
            if (gameObject.getId().equals(objectId)) {
                foundId = gameObject.getId();
                gameObject.setName(newName);
                break;
            }
        }

        if (foundId.isEmpty()) {
            System.err.println("No game object found with UUID: " + objectId);
            return;
        }

        // Build a response message that includes all of the original parameters.
        // We update the third parameter (index 2) to the new name.
        Object[] responseParams = java.util.Arrays.copyOf(originalParams, originalParams.length);
        responseParams[2] = newName;

        Message responseMsg = new Message("CHANGENAME", responseParams, "RESPONSE");
        server.broadcastMessageToAll(responseMsg);

    }
}
