package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Handles the "CREATEGAME" command, allowing a user to create a new
 * {@link Game} session, store it in {@code gameSessions}, and respond
 * with the new session's UUID and friendly name.
 */
public class CreateGameCommandHandler implements CommandHandler {

    /**
     * Creates a new {@link Game} instance (if a name parameter is present),
     * starts the session loop, and sends a response containing the game UUID
     * and requested name back to the initiating client.
     *
     * @param server         the server instance to access and store game sessions
     * @param msg            the "CREATEGAME" command
     * @param senderUsername the username of the client who requested the new game
     */
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
