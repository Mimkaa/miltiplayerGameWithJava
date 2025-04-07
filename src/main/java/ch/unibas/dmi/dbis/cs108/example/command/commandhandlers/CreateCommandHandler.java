package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Handles the "CREATE" command, which spawns a new {@link GameObject}
 * in the current game instance ({@code myGameInstance}).
 * <p>
 * This handler performs the following:
 * <ul>
 *   <li>Generates a new UUID for the object on the server side</li>
 *   <li>Creates the game object asynchronously</li>
 *   <li>Constructs a "CREATE" response message with the new UUID and original parameters</li>
 *   <li>Broadcasts the message to all connected clients</li>
 * </ul>
 * <p>
 * The original parameters are expected to include at least:
 * <ul>
 *   <li><code>params[0]</code>: name or type of the object</li>
 *   <li><code>params[1..n]</code>: any additional object-specific data</li>
 * </ul>
 */
public class CreateCommandHandler implements CommandHandler {

    /**
     * Handles an incoming "CREATE" command by generating a UUID, creating the
     * {@link GameObject} asynchronously, and broadcasting the result to all clients.
     *
     * @param server         the server instance for accessing game state and broadcasting
     * @param msg            the incoming "CREATE" command message
     * @param senderUsername the username of the client sending this command
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] originalParams = msg.getParameters();
        String serverGeneratedUuid = UUID.randomUUID().toString();

        // 1) Prepend server-generated UUID
        Object[] newParams = new Object[originalParams.length + 1];
        newParams[0] = serverGeneratedUuid;
        System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);

        // 2) Build the CREATE response
        Message responseMsg = new Message("CREATE", newParams, "RESPONSE");

        // 3) Actually create the GameObject asynchronously
        Future<GameObject> futureObj = server.getMyGameInstance().addGameObjectAsync(
                originalParams[0].toString(),
                serverGeneratedUuid,
                (Object[]) java.util.Arrays.copyOfRange(originalParams, 1, originalParams.length)
        );
        try {
            GameObject newObj = futureObj.get();
            System.out.println("Created new game object with UUID: " + serverGeneratedUuid
                    + " and name: " + newObj.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4) Broadcast to all
        server.broadcastMessageToAll(responseMsg);

        // 5) Optionally: additional logic, e.g., sending existing objects to the new user
    }
}
