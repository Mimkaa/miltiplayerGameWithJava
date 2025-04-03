package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

public class GetUsersCommandHandler implements CommandHandler
{
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
        if (senderAddress != null) {

            Object[] keyArray = server.getClientsMap().keySet().toArray(new Object[0]);


            Message responseMsg = new Message("GETUSERS", keyArray, "RESPONSE");
            server.enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            // System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }
}
