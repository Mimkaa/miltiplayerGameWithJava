package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.InetSocketAddress;

/**
 * For "REQUEST" messages, e.g. CREATE, PING, etc.
 * This is where you delegate to your existing CommandHandler system.
 */
public class RequestMessageHandler implements MessageHandler {

    public RequestMessageHandler() {
    }

    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // 1) User extrahieren
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null || concealed.length < 2) {
            System.out.println("Concealed parameters missing for REQUEST");
            return;
        }
        String username = concealed[concealed.length - 1];

        // 2) Hier kannst du eine eventuelle Nicknamen-Kollision abfangen etc.
        server.getClientsMap().putIfAbsent(username, senderSocket);

        // 3) Command-Name ermitteln (z.B. "PING", "CREATE", "LOGOUT", ...)
        String rawType = msg.getMessageType();
        String commandType = rawType.replaceAll("\\s+", "").toUpperCase();

        // 4) Pass an dein CommandRegistry
        CommandHandler handler = server.getCommandRegistry().getHandler(commandType);
        if (handler != null) {
            handler.handle(server, msg, username);
        } else {
            System.out.println("Unknown request type: " + commandType);
            // Evtl. default response senden
        }
    }
}