package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * SELECTGO: selects an existing game object by name within a specific game session,
 * returning the object's UUID if found.
 * Expected parameters:
 *   [0] - The game session ID
 *   [1] - The game object name
 */
public class SelectGoCommandHandler implements CommandHandler {

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 2) {
            System.err.println("SELECTGO request requires two parameters: game session ID and game object name.");
            return;
        }
        String targetGameId = params[0].toString();
        String targetObjectName = params[1].toString();

        // Retrieve the game session directly by its ID.
        Game targetGame = server.getGameSessionManager().getGameSession(targetGameId);
        if (targetGame == null) {
            System.out.println("No game session found with ID: " + targetGameId);
            return;
        }

        // Loop through the game objects in the target game.
        for (GameObject go : targetGame.getGameObjects()) {
            if (go.getName().equalsIgnoreCase(targetObjectName)) {
                Message response = new Message("SELECTGO", new Object[]{go.getId()}, "RESPONSE", msg.getConcealedParameters());
                InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
                if (senderAddress != null) {
                    server.enqueueMessage(response, senderAddress.getAddress(), senderAddress.getPort());
                    System.out.println("Sent SELECTGO response: game object UUID: " + go.getId());
                } else {
                    System.err.println("Sender address not found for user: " + senderUsername);
                }
                return;
            }
        }
        System.out.println("No game object with name \"" + targetObjectName + "\" found in game with ID \"" + targetGameId + "\".");
    }
}

