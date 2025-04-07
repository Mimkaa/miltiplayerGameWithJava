package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

/**
 * Handles the "REGISTER" command sent by a client when connecting to the server.
 * <p>
 * This command is used to notify all other clients that a new user has joined.
 * The server simply forwards the message to all connected clients with the option set to "Response".
 * </p>
 */
public class RegisterCommandHandler implements CommandHandler {

    /**
     * Processes the REGISTER command by setting the message option to "Response"
     * and broadcasting the message to all connected clients.
     *
     * @param server the server instance handling the message
     * @param msg the REGISTER message sent by the client
     * @param senderUsername the username of the client sending the message
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        msg.setOption("Response");
        server.broadcastMessageToAll(msg);
    }
}
