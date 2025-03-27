package ch.unibas.dmi.dbis.cs108.example.command;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import java.net.InetSocketAddress;

public class PingCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
        if (senderAddress != null) {
            Message responseMsg = new Message("PONG", new Object[]{}, "RESPONSE");
            server.enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }


}
