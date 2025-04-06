package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;

import javafx.application.Application;
import javafx.application.Platform;

/**
 * Entry point class for starting the application in different modes:
 * server, client, or both. It initializes networking components,
 * sets up the user interface, and handles command-line arguments.
 *
 * <p>This class supports:
 * <ul>
 *     <li>Launching only the server</li>
 *     <li>Launching only the client</li>
 *     <li>Launching both server and client together</li>
 * </ul>
 *
 * <p>The chosen mode is passed as a command-line argument.
 */
public class ThinkOutsideTheRoom {

    /** The singleton client instance accessible from other parts like the GUI. */
    public static Client client;

    /** The game context instance used for UI and session management. */
    public static GameContext gameContext;

    /**
     * Main method to parse arguments and start the application accordingly.
     *
     * @param args Command-line arguments (mode and optional settings)
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "server":
                if (args.length != 2) {
                    System.err.println("Usage: server <port>");
                    return;
                }
                startServer(args[1]);
                break;

            case "client":
                if (args.length < 2 || args.length > 3) {
                    System.err.println("Usage: client <host:port> [username]");
                    return;
                }
                String[] parts = args[1].split(":");
                if (parts.length != 2) {
                    System.err.println("Invalid host:port format.");
                    return;
                }
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                String username = args.length == 3 ? args[2] : null;

                prepareClientAndContext(host, port, username);
                Application.launch(ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI.class);
                break;

            case "both":
                if (args.length < 2 || args.length > 3) {
                    System.err.println("Usage: both <port> [username]");
                    return;
                }
                int bothPort = Integer.parseInt(args[1]);
                String bothUsername = args.length == 3 ? args[2] : null;

                startServer(String.valueOf(bothPort));
                try {
                    Thread.sleep(1000); // Wait until the server is ready
                } catch (InterruptedException ignored) {}

                System.out.println("Debug: Username from args = " + bothUsername + " to args = " + args[2]);
                prepareClientAndContext("localhost", bothPort, bothUsername);
                Application.launch(ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI.class);
                break;

            default:
                System.err.println("Unknown mode. Use: server, client or both");
        }
    }

    /**
     * Prints usage instructions to the console.
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  server <port>");
        System.out.println("  client <host:port> [username]");
        System.out.println("  both <port> [username]");
    }

    /**
     * Starts the server in a new thread using the given port.
     *
     * @param port The port number to bind the server to.
     */
    private static void startServer(String port) {
        new Thread(() -> Server.main(new String[]{port})).start();
    }

    /**
     * Prepares the client and game context.
     * Initializes the client with the given host, port, and username.
     * If no username is provided, one will be generated or requested via console.
     * Starts the networking, optional console reader, and initializes GameContext.
     *
     * @param host     The server host address.
     * @param port     The server port.
     * @param username The optional username to use (or null to prompt/generate).
     */
    private static void prepareClientAndContext(String host, int port, String username) {
        // Generate or request username
        if (username == null || username.trim().isEmpty()) {
            String suggested = Nickname_Generator.generateNickname();
            System.out.println("Suggested Nickname: " + suggested);

            if (System.console() != null) {
                System.out.println("Please press Enter to accept or type your own:");
                String input = System.console().readLine();
                username = input.isEmpty() ? suggested : input;
            } else {
                System.out.println("No console available. Using suggested nickname.");
                username = suggested;
            }
        }

        // Initialize and start the client
        client = new Client();
        client.setUsername(username);
        client.setServerAddress(host);
        client.setServerPort(port);
        new Thread(client::run).start();

        try {
            Thread.sleep(1000); // Wait for connection
        } catch (InterruptedException ignored) {}

        // Start console command input only if available
        if (System.console() != null) {
            client.startConsoleReaderLoop();
        } else {
            System.out.println("No console available. Skipping console input loop.");
        }

        // Send registration message
        Message register = new Message("REGISTER", new Object[]{}, "REQUEST");
        Client.sendMessageStatic(register);

        // Create GameContext (UI setup will happen in GUI class)
        gameContext = new GameContext();
    }
}
