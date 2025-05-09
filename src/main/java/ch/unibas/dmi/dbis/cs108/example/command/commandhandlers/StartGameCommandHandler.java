package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

public class StartGameCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) 
    {
        String gameId = msg.getParameters()[0].toString();
        server.getGameSessionManager().getGameSession(gameId).setStartedFlag(!server.getGameSessionManager().getGameSession(gameId).getStartedFlag());
        msg.setOption("RESPONSE");
        server.broadcastMessageToAll(msg);
    }

}
