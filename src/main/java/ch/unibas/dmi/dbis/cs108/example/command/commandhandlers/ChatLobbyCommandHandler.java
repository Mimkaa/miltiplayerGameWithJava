package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

public class ChatLobbyCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 3) {
            System.err.println("CHATLOBBY message missing required parameters.");
            return;
        }
        // Expected parameters:
        //   [0] - sender's username
        //   [1] - game session ID
        //   [2] - message text
        String sender = params[0].toString();
        String gameId = params[1].toString();
        String messageText = params[2].toString();

        // Retrieve the game session using the provided game ID
        Game game = server.getGameSessionManager().getGameSession(gameId);
        if (game == null) {
            System.err.println("No game session found with id: " + gameId);
            return;
        }

        // Create a new message to broadcast to all users in the game session.
        // We use "RESPONSE" as the option to indicate this is a forwarded/broadcast message.
        Message broadcastMsg = new Message(
                "CHATLOBBY",
                new Object[]{sender, gameId, messageText},
                "RESPONSE",
                msg.getConcealedParameters()
        );

        // Iterate over all users in the game session and send the message, except the sender.
        for (String user : game.getUsers()) {
            if (user.equals(sender)) {
                continue;
            }
            InetSocketAddress recipientAddress = server.getClientsMap().get(user);
            if (recipientAddress != null) {
                server.enqueueMessage(broadcastMsg, recipientAddress.getAddress(), recipientAddress.getPort());
            } else {
                System.err.println("No client address found for user: " + user);
            }
        }
    }
}
