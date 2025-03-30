package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * Handles the "DELETE" command to remove a player or object from
 * the current game and broadcast this deletion event to all clients.
 */
public class DeleteCommandHandler implements CommandHandler {

    /**
     * Removes the specified player/object from {@code myGameInstance}
     * and broadcasts a {@code DELETE} message to every client.
     *
     * @param server         the server instance, giving access to the active game
     * @param msg            the incoming "DELETE" command
     * @param senderUsername the username of the client requesting the deletion
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        System.out.println("Client deleting: " + senderUsername);
        Message deleteMessage = Server.makeResponse(msg, msg.getParameters());
        server.broadcastMessageToAll(deleteMessage);

        String targetPlayerName = msg.getParameters()[0].toString();
        if (server.getMyGameInstance() != null) {
            server.getMyGameInstance().getGameObjects()
                    .removeIf(go -> go.getName().equals(targetPlayerName));
        }
    }
}
