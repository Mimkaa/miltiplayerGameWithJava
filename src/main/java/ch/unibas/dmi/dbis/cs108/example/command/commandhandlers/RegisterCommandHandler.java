package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * REGISTER  [ username , "ip:port" ]
 */
public class RegisterCommandHandler implements CommandHandler {

    @Override
    public void handle(Server server, Message msg, String ignoredUsername) {

        /* ----------------- extract params ----------------- */
        Object[] p = msg.getParameters();
        if (p == null || p.length < 1) {
            System.err.println("REGISTER missing username");
            return;
        }
        String requested = p[0].toString();
        InetSocketAddress sender = server.getLastSender();
        String assigned = server.findUniqueName(requested);
        System.out.printf("Register requested %s @ %s%n", requested, sender);

        server.getClientsMap().putIfAbsent(assigned, sender);
        System.out.printf("Register added %s @ %s%n", assigned, sender);


        /* ----------------- optional broadcast ------------- */
        //msg.setOption("RESPONSE");
        //server.broadcastMessageToAll(msg);
        //InetSocketAddress dest = server.getClientsMap().get(username);
        Message responseMsg = Server.makeResponse(msg, new Object[]{assigned});
        responseMsg.setOption("RESPONSE");
        String one = responseMsg.getConcealedParameters()[0].toString();
        String two = responseMsg.getConcealedParameters()[1].toString();
        String three = responseMsg.getConcealedParameters()[2].toString();
          System.out.println("one: " + one + " two: " + two + " three: " + three);
        responseMsg.setConcealedParameters(new String[]{assigned});
        AsyncManager.run(() -> server.syncGames(sender));
    }
}
