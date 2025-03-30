package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * DELETEGO: removes a game object from the specified session
 * and broadcasts that deletion to all clients.
 * Expected parameters:
 *   [0] - The game session ID
 *   [1] - The object ID (UUID) to remove
 */
public class DeleteGoCommandHandler implements CommandHandler {

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] originalParams = msg.getParameters();
        if (originalParams == null || originalParams.length < 2) {
            System.err.println("DELETEGO request missing required parameters. Expected: [gameSessionId, objectId]");
            return;
        }

        String sessionId = originalParams[0].toString();
        String objectId = originalParams[1].toString();

        // Find the game session
        Game game = server.getGameSessionManager().getGameSession(sessionId);
        if (game == null) {
            System.out.println("No game session found with id: " + sessionId);
            return;
        }

        // Remove the object
        boolean removed = game.getGameObjects().removeIf(go -> go.getId().equals(objectId));
        if (removed) {
            System.out.println("Deleted game object with id: " + objectId + " from session " + sessionId);
        } else {
            System.out.println("No game object with id " + objectId + " found in session " + sessionId);
        }

        // Broadcast a DELETEGO response with the original parameters
        Message responseMsg = new Message("DELETEGO", originalParams, "RESPONSE", msg.getConcealedParameters());
        server.broadcastMessageToAll(responseMsg);
    }
}
