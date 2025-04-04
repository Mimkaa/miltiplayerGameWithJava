package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import javafx.application.Platform;

/**
 * Entry point for the ThinkOutsideTheRoom application.
 * <p>
 * This class allows the user to start the application in one of three modes:
 * <ul>
 *     <li><b>server &lt;port&gt;</b>: Starts the server on the specified port.</li>
 *     <li><b>client &lt;host:port&gt; [username]</b>: Starts a client that connects to the given server address, optionally with a username.</li>
 *     <li><b>both &lt;port&gt; [username]</b>: Starts both server and client on the same machine for testing purposes.</li>
 * </ul>
 * Usage is printed if no arguments or invalid arguments are provided.
 */
public class ThinkOutsideTheRoom {

    /**
     * Main method for launching the application.
     *
     * @param args Command-line arguments to specify the mode and relevant parameters.
     *             <ul>
     *                 <li>server &lt;port&gt;</li>
     *                 <li>client &lt;host:port&gt; [username]</li>
     *                 <li>both &lt;port&gt; [username]</li>
     *             </ul>
     */
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

                // username
                String username = (args.length == 3) ? args[2] : null;

                // starting gamecontext
                GameContext context = new GameContext();

                new Thread(() -> {
                    context.start(); // baut UI, registriert sich
                    Platform.runLater(context::startGameLoop); // startet Game Loop
                }).start();

                // starting client
                Client.main(args);
                break;


            case "both":
                if (args.length < 2 || args.length > 3) {
                    System.err.println("Usage: both <port> [username]");
                    return;
                }

                String port = args[1];

                // Server starten
                new Thread(() -> Server.main(new String[]{port})).start();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                String address = "localhost:" + port;
                String usernameBoth = (args.length == 3) ? args[2] : null;

                // starting gamecontext
                GameContext contextBoth = new GameContext();
                new Thread(() -> {
                    contextBoth.start();
                    Platform.runLater(contextBoth::startGameLoop);
                }).start();

                // starting client
                if (usernameBoth != null) {
                    Client.main(new String[]{"client", address, usernameBoth});
                } else {
                    Client.main(new String[]{"client", address});
                }
                break;

            default:
                System.err.println("Unknown mode. Use: server, client or both");
        }
    }
}
