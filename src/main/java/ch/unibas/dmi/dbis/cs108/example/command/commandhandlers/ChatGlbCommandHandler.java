package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

public class ChatGlbCommandHandler implements CommandHandler {

    @Override
    public void handle(Server server, Message msg, String senderUsername) {

        Object[] params = msg.getParameters();
        if (params == null || params.length < 2) {
            System.out.println("Invalid CHATGLB message from " + senderUsername);
            return;
        }

        String fromUser = params[0].toString();
        String message = params[1].toString();

        Message broadcastMsg = new Message("CHATGLB", new String[]{fromUser, message}, "RESPONSE");
        //server.broadcastMessageToAll(broadcastMsg);
        server.broadcastMessageToOthers(broadcastMsg, senderUsername);
        System.out.println("Broadcasted CHATGLB message: " + fromUser + ": " + message);

    }
}

