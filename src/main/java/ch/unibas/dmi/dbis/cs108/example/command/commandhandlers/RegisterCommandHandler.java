package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.BestEffortBroadcastManager;
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
        if (p == null || p.length < 3) {
            System.err.println("REGISTER missing parameters (username, reliablePort, bestEffortPort)");
            return;
        }
        String requested      = p[0].toString();
        int    reliablePort   = Integer.parseInt(p[1].toString());
        int    bestEffortPort = Integer.parseInt(p[2].toString());

        InetSocketAddress sender = server.getLastSender();        // has the IP
        String assigned = server.findUniqueName(requested);

        System.out.printf("Register requested %s @ %s%n", requested, sender);

        // 1) store in the reliable‐map (uses sender.getPort() or reliablePort?)
        //    If you want to use the port they told you, do:
        InetSocketAddress reliableAddr =
            new InetSocketAddress(sender.getAddress(), reliablePort);
        server.getClientsMap()
            .putIfAbsent(assigned, reliableAddr);
        System.out.printf("Reliable client map added %s @ %s%n",
                        assigned, reliableAddr);

        // 2) store in the best‐effort map
        InetSocketAddress beAddr =
            new InetSocketAddress(sender.getAddress(), bestEffortPort);
        server.getClientsMapBestEffort()
            .putIfAbsent(assigned, beAddr);
        System.out.printf("Best-effort client map added %s @ %s%n",
                        assigned, beAddr);

        /* ------------- register with BestEffortBroadcastManager ------------- */
        BestEffortBroadcastManager bem = server.getBestEffortBroadcastManager();
        // for broadcast you’ll want to use the BE port:
        bem.registerClient(assigned, beAddr);

        /* ----------------- send back a RESPONSE ----------------- */
        Message responseMsg = Server.makeResponse(msg, new Object[]{assigned});
        responseMsg.setOption("RESPONSE");
        // only conceal the assigned username in the response
        responseMsg.setConcealedParameters(new String[]{ assigned });

        server.enqueueMessage(
            responseMsg,
            reliableAddr.getAddress(),
            reliableAddr.getPort()
        );

        // sync any game‐state over on the BE channel if needed
        AsyncManager.run(() -> server.syncGames(beAddr));
    }

}
