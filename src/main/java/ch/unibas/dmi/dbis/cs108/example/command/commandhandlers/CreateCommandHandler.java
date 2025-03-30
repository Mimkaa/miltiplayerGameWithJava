package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Handles the "CREATE" command, which spawns a new {@link GameObject} in the
 * active {@code myGameInstance}, assigns it a server-generated UUID, and
 * broadcasts the new object to all connected clients.
 */
public class CreateCommandHandler implements CommandHandler {

    /**
     * Generates a unique object UUID, creates the corresponding {@link GameObject}
     * asynchronously, and broadcasts a "CREATE" response to all clients.
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
