package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

public class RegisterCommandHandler implements CommandHandler {
    
@Override
    public void handle(Server server, Message msg, String senderUsername) {
        msg.setOption("Response");
        server.broadcastMessageToAll(msg);
    }
}
