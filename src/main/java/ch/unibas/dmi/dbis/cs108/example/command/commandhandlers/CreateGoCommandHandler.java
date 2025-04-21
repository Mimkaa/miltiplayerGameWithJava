package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Future;

import static ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server.makeResponse;

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
            System.err.println("CREATEGO request requires at least two parameters: game session ID and object type.");
            return;
        }
        System.out.println("RARRRRR:");
        System.out.println(msg);
        
        // originalParams[0]: game session ID, originalParams[1]: object type, remaining are constructor parameters

        // Generate a new UUID for the game object.
        String serverGeneratedUuid = UUID.randomUUID().toString();

        // Build a new parameter array with one extra element.
        Object[] newParams = new Object[originalParams.length + 1];
        // Insert the generated UUID as the first parameter.
        newParams[0] = serverGeneratedUuid;
        // Copy all original parameters into newParams starting at index 1.
        System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);

        // Build the CREATEGO response message using the new parameter array.
        Message responseMsg = new Message("CREATEGO", newParams, "RESPONSE", msg.getConcealedParameters());

        // The newParams array is now structured as:
        // [0] generated UUID, [1] game session ID, [2] object type, [3...] additional constructor parameters.
        // Extract constructor parameters starting from index 3.
        Object[] constructorParams = new Object[0];
        if (newParams.length > 3) {
            constructorParams = java.util.Arrays.copyOfRange(newParams, 3, newParams.length);
        }

        // Retrieve the target game session from the manager using the game session ID (newParams[1]).
        String gameSessionId = newParams[1].toString();
        Game targetGame = server.getGameSessionManager().getGameSession(gameSessionId);
        if (targetGame == null) {
            System.err.println("No game session found with ID: " + gameSessionId);
            return;
        }

        // === Check for duplicate object name ===
        String objectName = constructorParams.length > 0 ? constructorParams[0].toString() : null;
        if (objectName != null && targetGame.containsObjectByName(objectName)) {
            System.out.println("Duplicate detected: Object with name " + objectName + " already exists in game " + gameSessionId);
            return;
        }

        // Asynchronously create the game object using the factory.
        Future<GameObject> futureObj = targetGame.addGameObjectAsync(newParams[2].toString(), serverGeneratedUuid, constructorParams);
        try {
            GameObject newObj = futureObj.get();
            System.out.println("Created new game object with UUID: " + serverGeneratedUuid
                    + " and name: " + newObj.getName() + " in game session: " + gameSessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
     

        // Broadcast the CREATEGO response message to all clients.
        System.out.println("Broadcasting CREATEGO to all: " + responseMsg);
        server.broadcastMessageToAll(responseMsg);

        InetSocketAddress newUserAddress = server.getClientsMap().get(senderUsername);
        if (newUserAddress != null) {
        String sessionId = targetGame.getGameId();  // or newParams[1].toString()
        for (GameObject gameObject : targetGame.getGameObjects()) {
            String objId   = gameObject.getId();
            if (objId.equals(serverGeneratedUuid)) continue;

            Object[] ctorArgs = gameObject.getConstructorParamValues();
            // build [ objId, sessionId, type, ...ctorArgs ]
            Object[] finalParameters = new Object[ctorArgs.length + 3];
            finalParameters[0] = objId;
            finalParameters[1] = sessionId;
            finalParameters[2] = gameObject.getClass().getSimpleName();
            System.arraycopy(ctorArgs, 0, finalParameters, 3, ctorArgs.length);

            Message replayMsg = makeResponse(msg, finalParameters);
            //server.enqueueMessage(replayMsg,newUserAddress.getAddress(),newUserAddress.getPort());
        }
        } else {
        System.err.println("No known address for user: " + senderUsername);
        }


        // After handling CREATEGO, print all game sessions (IDs and names) for debugging.
        System.out.println("Current game sessions:");
        server.getGameSessionManager().getAllGameSessions().forEach((id, gameSession) -> {
            System.out.println("  Game Session ID: " + id + " | Name: " + gameSession.getGameName());
        });

        

    }
}
