package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * JOINGAME: attempts to join an existing game session by its "friendly name".
 * If found, returns the matching game ID. Otherwise, logs an error or responds
 * with an error message.
 * Expected parameters:
 *   [0] - The name (string) of the game session to join.
 */
public class JoinGameCommandHandler implements CommandHandler {

    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 1) {
            System.err.println("JOINGAME request missing the game name to join.");
            return;
        }
        String requestedGameName = params[0].toString();
        String username =  params[1].toString();
        String prevGameId =  params[2].toString();

        // Search the gameSessionManager for a game with this name
        Game foundGame = null;
        for (Map.Entry<String, Game> entry : server.getGameSessionManager().getAllGameSessions().entrySet()) {

            Game candidate = entry.getValue();
            if (candidate.getGameName().equalsIgnoreCase(requestedGameName)) {
                foundGame = candidate;
                break;
            }
        }

        if (foundGame == null) {
            System.err.println("No game session found with name: " + requestedGameName);
            System.out.println("Serverref: " + server);
            System.out.println("gameSessionManager : " + server.getGameSessionManager());
            for (Map.Entry<String, Game> entry : server.getGameSessionManager().getAllGameSessions().entrySet()) {
                System.out.println("Games: " + entry.getKey());
            }
            // Option A: respond with an error
            Message errorResponse = new Message(
                    "JOINGAME_ERROR",
                    new Object[]{"No game found with name: " + requestedGameName},
                    "RESPONSE",
                    msg.getConcealedParameters()
            );
            InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
            if (senderAddress != null) {
                server.enqueueMessage(errorResponse, senderAddress.getAddress(), senderAddress.getPort());
            }
            return;
        }

        // Found the game
        String foundGameId = foundGame.getGameId();
        System.out.println("User " + senderUsername
                + " joined game session name: " + requestedGameName
                + " (ID: " + foundGameId + ")");

        // remove the user from the previous game
        if(!prevGameId.equals("default"))
        {
            server.getGameSessionManager().getGameSession(prevGameId).getUsers().remove(username);
        }
        // add the username in the game
        foundGame.getUsers().add(username);

        // Send back a success response with the found game’s ID
        Message response = new Message(
                "JOINGAME",
                new Object[]{foundGameId,username,prevGameId},
                "RESPONSE",
                msg.getConcealedParameters()
        );
        // Get the game ID.
        String gameId = foundGame.getGameId();

        // Convert the users set to a String array.
        String[] usersAsStrings = foundGame.getUsers().toArray(new String[0]);

        // Create a new Object array that will hold the game ID and all user names.
        Object[] paramss = new Object[1 + usersAsStrings.length];
        paramss[0] = gameId;
        System.arraycopy(usersAsStrings, 0, paramss, 1, usersAsStrings.length);

        // Create the message with a flattened parameters array.
        Message syncGamePlayers = new Message(
            "SYNCGP",
            paramss,
            "RESPONSE",
            msg.getConcealedParameters()
        );
        System.out.println(syncGamePlayers);
        InetSocketAddress address = server.getClientsMap().get(senderUsername);
        if (address != null) {
            server.broadcastMessageToAll(response);
            server.enqueueMessage(syncGamePlayers, address.getAddress(), address.getPort());
        } else {
            System.err.println("Sender address not found for user: " + senderUsername);
        }
    }
}
