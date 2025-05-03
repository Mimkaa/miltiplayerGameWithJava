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
        if (p == null || p.length < 2) {
            System.err.println("REGISTER missing params");
            return;
        }
        String username = p[0].toString();
        String hostPort = p[1].toString();            // "127.0.0.1:54321"

        /* ----------------- add to clientsMap -------------- */
        try {
            String[] hp = hostPort.split(":");
            InetAddress ip = InetAddress.getByName(hp[0]);
            int         pt = Integer.parseInt(hp[1]);

            server.getClientsMap()
                  .putIfAbsent(username, new InetSocketAddress(ip, pt));

            System.out.println("REGISTER → added " + username + " @ " + hostPort);

        } catch (Exception ex) {
            System.err.println("Bad host:port in REGISTER: " + hostPort);
            ex.printStackTrace();
            return;
        }

        /* ----------------- optional broadcast ------------- */
        //msg.setOption("RESPONSE");
        //server.broadcastMessageToAll(msg);
        InetSocketAddress dest = server.getClientsMap().get(username);
        AsyncManager.run(() -> server.syncGames(dest));
    }
}
