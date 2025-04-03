package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

public class ThinkOutsideTheRoom {


public static void main(String[] args) {
    if (args.length == 0) {
        System.out.println("Usage:");
        System.out.println("  server <port>");
        System.out.println("  client <host:port> [username]");
        System.out.println("  both <port> [username]");
        return;
    }

    String mode = args[0].toLowerCase();

    switch (mode) {
        case "server":
            if (args.length != 2) {
                System.err.println("Usage: server <port>");
                return;
            }
            Server.main(new String[]{args[1]});
            break;

        case "client":
            if (args.length < 2 || args.length > 3) {
                System.err.println("Usage: client <host:port> [username]");
                return;
            }
            Client.main(args);
            break;

        case "both":
            if (args.length < 2 || args.length > 3) {
                System.err.println("Usage: both <port> [username]");
                return;
            }

            String port = args[1];
            new Thread(() -> Server.main(new String[]{port})).start();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            String address = "localhost:" + port;

            if (args.length == 3) {
                Client.main(new String[]{"client", address, args[2]});
            } else {
                Client.main(new String[]{"client", address});
            }
            break;

        default:
            System.err.println("Unknown mode. Use: server, client or both");
        }
    }
}
