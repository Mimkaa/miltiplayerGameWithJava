package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * DELETE: remove a player from the game and broadcast.
 */
public class DeleteCommandHandler implements CommandHandler {
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