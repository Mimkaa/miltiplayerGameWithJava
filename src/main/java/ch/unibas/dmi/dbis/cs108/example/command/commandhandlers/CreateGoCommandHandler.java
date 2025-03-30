package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * CREATEGO: Creates a new game object within a specified game session, assigning
 * a server-generated UUID and broadcasting the creation to all clients.
 * Expected parameters:
 *   [0] - The game session ID
 *   [1] - The object type (e.g. "Player")
 *   [2+] - Additional constructor parameters for the object
 */
public class CreateGoCommandHandler implements CommandHandler {

    /**
     * Creates a new {@link GameObject} in the specified game session, identified by
     * the first parameter in {@code msg.getParameters()}. The new object is assigned
     * a server-generated UUID and then broadcast to all connected clients.
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] originalParams = msg.getParameters();
        if (originalParams == null || originalParams.length < 2) {
            System.err.println("CREATEGO request requires at least two parameters: gameSessionId and objectType.");
            return;
        }

        // Generate a new UUID for the game object
        String serverGeneratedUuid = UUID.randomUUID().toString();

        // Build a new parameter array with an extra slot for the UUID
        Object[] newParams = new Object[originalParams.length + 1];
        newParams[0] = serverGeneratedUuid;
        System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);

        // Response message
        Message responseMsg = new Message("CREATEGO", newParams, "RESPONSE", msg.getConcealedParameters());

        // E.g., newParams: [uuid, gameSessionId, objectType, ...constructorParams...]
        String gameSessionId = newParams[1].toString();
        String objectType = newParams[2].toString();

        // Extract the object’s constructor parameters, if any
        Object[] constructorParams = new Object[0];
        if (newParams.length > 3) {
            constructorParams = java.util.Arrays.copyOfRange(newParams, 3, newParams.length);
        }

        // Find the target game session
        // (Assuming your server has a gameSessionManager or something similar)
        Game targetGame = server.getGameSessionManager().getGameSession(gameSessionId);
        if (targetGame == null) {
            System.err.println("No game session found with ID: " + gameSessionId);
            return;
        }

        // Asynchronously create the game object in that session
        Future<GameObject> futureObj = targetGame.addGameObjectAsync(objectType, serverGeneratedUuid, constructorParams);
        try {
            GameObject newObj = futureObj.get();
            System.out.println("Created new game object with UUID: " + serverGeneratedUuid
                    + " and name: " + newObj.getName()
                    + " in game session: " + gameSessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Broadcast the CREATEGO to everyone
        server.broadcastMessageToAll(responseMsg);

        // Optionally, send existing objects in this session to the new user
        InetSocketAddress newUserAddress = server.getClientsMap().get(senderUsername);
        if (newUserAddress != null) {
            for (GameObject gameObject : targetGame.getGameObjects()) {
                if (gameObject.getId().equals(serverGeneratedUuid)) {
                    // skip the newly created one, if desired
                    continue;
                }
                Object[] constructorParameters = gameObject.getConstructorParamValues();
                Object[] finalParameters = new Object[constructorParameters.length + 2];
                finalParameters[0] = gameObject.getId();
                finalParameters[1] = gameObject.getClass().getSimpleName();
                System.arraycopy(constructorParameters, 0, finalParameters, 2, constructorParameters.length);

                Message createResponseMessage = Server.makeResponse(msg, finalParameters);
                server.enqueueMessage(createResponseMessage,
                        newUserAddress.getAddress(), newUserAddress.getPort());
            }
        } else {
            System.err.println("No known address for user: " + senderUsername);
        }

        // Debug: print all game sessions
        System.out.println("Current game sessions:");
        server.getGameSessionManager().getAllGameSessions().forEach((id, g) -> {
            System.out.println("  ID: " + id + " | Name: " + g.getGameName());
        });
    }
}
