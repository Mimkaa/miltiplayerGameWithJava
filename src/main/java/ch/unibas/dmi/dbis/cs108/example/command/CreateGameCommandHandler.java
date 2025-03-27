package ch.unibas.dmi.dbis.cs108.example.command;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * CREATEGAME: create a new Game session, store it in gameSessions, and respond.
 */
public class CreateGameCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 1) {
            System.err.println("CREATEGAME request missing game name parameter.");
            return;
        }
        String requestedGameName = params[0].toString();
        String gameUuid = UUID.randomUUID().toString();

        // Create and start the new game session
        Game newGame = new Game(gameUuid, requestedGameName);
        newGame.startPlayersCommandProcessingLoop();

        // Store in server’s gameSessions map
        server.getGameSessions().put(gameUuid, newGame);

        // Build a response message
        Message response = new Message(
                "CREATEGAME",
                new Object[]{gameUuid, requestedGameName},
                "RESPONSE",
                msg.getConcealedParameters()
        );

        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
        if (senderAddress != null) {
            server.enqueueMessage(response, senderAddress.getAddress(), senderAddress.getPort());
            System.out.println("Created new game session '" + requestedGameName
                    + "' with UUID: " + gameUuid);
        } else {
            System.err.println("Sender address not found for user: " + senderUsername);
        }
    }
}

