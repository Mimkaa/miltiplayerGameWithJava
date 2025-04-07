package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * Handles the "CHANGENAME" command which allows renaming of game objects.
 *
 * <p>This handler performs the following operations:
 * <ol>
 *   <li>Validates the incoming request parameters</li>
 *   <li>Locates the specified game session and object</li>
 *   <li>Generates a unique name if the requested name is taken</li>
 *   <li>Updates the object's name and broadcasts the change</li>
 * </ol>
 *
 * <p>The expected message format is:
 * <pre>
 * {
 *   "messageType": "CHANGENAME",
 *   "parameters": ["gameSessionId", "objectUUID", "requestedName"],
 *   "option": "REQUEST"
 * }
 * </pre>
 */
public class ChangeNameCommandHandler implements CommandHandler {

    /**
     * Processes a CHANGENAME request to rename a game object.
     *
     * <p>The method:
     * <ul>
     *   <li>Verifies parameter count and validity</li>
     *   <li>Finds the target game session and object</li>
     *   <li>Generates a unique variant if name is taken</li>
     *   <li>Updates the object and notifies all clients</li>
     * </ul>
     *
     * <p>Error cases handled:
     * <ul>
     *   <li>Missing/invalid parameters</li>
     *   <li>Nonexistent game session</li>
     *   <li>Nonexistent game object</li>
     * </ul>
     *
     * @param server         the server instance providing access to game state and utilities
     * @param msg            the incoming CHANGENAME message containing rename parameters
     * @param senderUsername the username of the client requesting the change
     *
     * @see Server#findUniqueName(String)
     * @see Server#broadcastMessageToAll(Message)
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