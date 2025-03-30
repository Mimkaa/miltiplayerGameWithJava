package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.InetSocketAddress;

/**
 * Handles "REQUEST" messages (for commands such as CREATE, PING, DELETE, etc.)
 * by delegating to the appropriate {@link CommandHandler} in the
 * server's {@code CommandRegistry}.
 */
public class RequestMessageHandler implements MessageHandler {

    /**
     * Required no-arg constructor for reflection-based discovery.
     */
    public RequestMessageHandler() {
        // ...
    }

    /**
     * Extracts the username from concealed parameters, ensures it is in the
     * {@code clientsMap}, determines the command name (e.g. "PING", "CREATE"),
     * and invokes the relevant {@link CommandHandler}.
     *
     * @param server       the server instance with access to {@code CommandRegistry}
     * @param msg          the "REQUEST" message containing command info
     * @param senderSocket the socket address of the client
     */
    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // 1) Extract user from concealed parameters
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null || concealed.length < 2) {
            System.out.println("Concealed parameters missing for REQUEST");
            return;
        }
        String username = concealed[concealed.length - 1];

        // 2) Add user if not already present
        server.getClientsMap().putIfAbsent(username, senderSocket);

        // 3) Determine the actual command name
        String rawType = msg.getMessageType();
        String commandType = rawType.replaceAll("\\s+", "").toUpperCase();

        // 4) Lookup a matching CommandHandler
        CommandHandler handler = server.getCommandRegistry().getHandler(commandType);
        if (handler != null) {
            handler.handle(server, msg, username);
        } else {
            System.out.println("Unknown request type: " + commandType);
            // Possibly send a default response
        }
    }
}
