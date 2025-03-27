package ch.unibas.dmi.dbis.cs108.example.command;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import java.util.UUID;
import java.util.concurrent.Future;

public class CreateCommandHandler implements CommandHandler {
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

        // 5) If you need to do extra logic (e.g., send existing objects to the new user),
        //    you can do that here. Possibly you move that from your old handleCreateRequest
        //    method.
    }


}
