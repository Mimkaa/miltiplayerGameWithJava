package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import java.net.InetSocketAddress;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

public class CollectGSCommandHandler implements CommandHandler 
{
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        InetSocketAddress senderAddress = server.getClientsMap().get(senderUsername);
        if (senderAddress != null) {

            Object[] keyArray = server.getClientsMap().keySet().toArray(new Object[0]);

            Message responseMsg = msg.clone();
            responseMsg.setOption("RESPONSE");
            
            server.enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            // System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }
}
