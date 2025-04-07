package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "CHATLOBBY" command, which is used to send chat messages
 * within a specific game lobby (game session).
 *
 * <p>Expected parameters:</p>
 * <ol>
 *   <li><code>params[0]</code>: sender's username (String)</li>
 *   <li><code>params[1]</code>: game session ID (String)</li>
 *   <li><code>params[2]</code>: chat message content (String)</li>
 * </ol>
 *
 * <p>The message will be broadcast to all users in the game lobby,
 * except the sender. If the game session does not exist or a user's
 * address cannot be found, appropriate error messages are logged.</p>
 */
public class ChatLobbyCommandHandler implements CommandHandler {

    /**
     * Processes the incoming "CHATLOBBY" message and broadcasts it to
     * all users in the corresponding game session, excluding the sender.
     *
     * @param server          the server instance handling communication and session management
     * @param msg             the received message containing the chat content
     * @param senderUsername  the username of the client who sent the message
     */
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
