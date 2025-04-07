package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Handles the "CREATEGAME" command, allowing a user to create a new
 * {@link Game} session. The handler generates a unique UUID for the game,
 * stores it in the server's game session manager, and sends a response
 * to the initiating client with the session's UUID and display name.
 *
 * <p>This is typically triggered when a client wants to create a private
 * or public game lobby before other players join.</p>
 *
 * <p>Expected parameters:</p>
 * <ul>
 *   <li><code>params[0]</code>: the requested name of the game session (String)</li>
 * </ul>
 */
public class CreateGameCommandHandler implements CommandHandler {

    /**
     * Handles the "CREATEGAME" command by creating a new {@link Game} instance
     * with a generated UUID and the provided session name. If successful,
     * the session is stored and a response is broadcast to all connected clients.
     *
     * @param server         the server instance to access and store game sessions
     * @param msg            the "CREATEGAME" command containing the session name
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

        Game newGame = new Game(gameUuid, requestedGameName);
        server.getGameSessionManager().addGameSession(gameUuid, newGame);

        Message response = new Message("CREATEGAME", new Object[]{gameUuid, requestedGameName}, "RESPONSE", msg.getConcealedParameters());
        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);

        if (senderAddress != null) {
            server.broadcastMessageToAll(response);
            System.out.println("Created new game session '" + requestedGameName + "' with UUID: " + gameUuid);
        } else {
            System.err.println("Sender address not found for user: " + senderUsername);
        }
    }
}
