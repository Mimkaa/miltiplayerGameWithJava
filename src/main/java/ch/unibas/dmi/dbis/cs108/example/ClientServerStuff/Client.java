package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHub;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatPanel;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import lombok.Getter;

import javax.swing.SwingUtilities;

import static ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server.SERVER_BE_PORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingDeque;

/**
 * The {@code Client} class represents a networked game client that communicates
 * with a server via UDP, sending and receiving messages related to gameplay and chat.
 *
 * <p><strong>Main responsibilities include:</strong></p>
 * <ul>
 *   <li>Managing a {@link Game} instance responsible for local game logic.</li>
 *   <li>Handling chat functionality through a {@link ChatManager.ClientChatManager}.</li>
 *   <li>Queuing and sending outgoing {@link Message} objects via {@link #outgoingQueue}.</li>
 *   <li>Receiving incoming {@link Message} objects via {@link #incomingQueue} and dispatching them to handlers.</li>
 *   <li>Providing reliable and best-effort UDP message sending through {@link ReliableUDPSender}.</li>
 *   <li>Handling acknowledgments (ACKs) for messages using an {@link AckProcessor}.</li>
 *   <li>Optionally tracking round-trip times (ping) via a {@link PingManager} (disabled by default in this code).</li>
 * </ul>
 *
 * <p>Usage example for running the client might include:</p>
 * <ol>
 *   <li>Create an instance of {@code Client} with a desired game session name.</li>
 *   <li>Set the client's username (e.g., {@link #setUsername(String)}).</li>
 *   <li>Configure the game UI (e.g., 2D or 3D viewport).</li>
 *   <li>Start console reading via {@link #startConsoleReaderLoop()} if needed.</li>
 *   <li>Run the main loop in a separate thread using {@link #run()}.</li>
 *   <li>Use {@link #login()} to send a login request to the server.</li>
 * </ol>
 *
 * <p>
 * This class maintains a singleton instance accessible via {@link #getInstance()}.
 * </p>
 */
@Getter
public class Client {

    /**
     * The default server address, set to "localhost" by default.
     */
    public static String SERVER_ADDRESS = "localhost";

    /**
     * The default server port, set to 9876 by default.
     */
    public static int SERVER_PORT = 9876;
    public int CLIENT_PORT = 9876;
    public int CLIENT_BESTEFFORT_PORT = 2222;

    /**
     * Manages all chat functionality on the client side.
     */
    public ChatManager.ClientChatManager clientChatManager;

    /**
     * A thread-safe queue for outgoing messages to be sent to the server or other clients.
     * This queue is processed in a background loop.
     */
    // before:
    // private static final LinkedBlockingQueue<Message> outgoingQueue = new LinkedBlockingQueue<>();
    private static final BlockingDeque<OutgoingMessage> outgoingQueue = new LinkedBlockingDeque<>();


    /**
     * A thread-safe queue for incoming messages received from the server or other clients.
     * This queue is processed in a background loop.
     */
    private static final LinkedBlockingQueue<Message> incomingQueue = new LinkedBlockingQueue<>();

    /**
     * The {@link Game} instance handling local game logic, such as objects and event responses.
     */
    private Game game;

    /**
     * A {@link Scanner} for reading commands from the console (if available).
     */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * A unique string identifier for this client, randomly generated.
     */
    private final String clientId = UUID.randomUUID().toString();

    /**
     * The client's username, stored in an {@link AtomicReference} for thread-safe updates.
     */
    private final AtomicReference<String> username = new AtomicReference<>(clientId);

    /**
     * A reliable UDP sender used for messages requiring reliable delivery.
     */
    private ReliableUDPSender myReliableUDPSender;

    /**
     * The acknowledgment processor responsible for receiving and sending ACK messages.
     */
    private AckProcessor ackProcessor;

    /**
     * The underlying UDP socket used by this client for sending and receiving data.
     */
    private DatagramSocket clientSocket;

    private DatagramSocket clientSocketBestEffort;

    /**
     * Tracks ping (round-trip time) data if started. Inactive by default.
     */
    private PingManager pingManager;

    /**
     * The singleton instance of this client.
     */
    private static Client instance;

    /**
     * A {@link MessageHub} for dispatching incoming messages to appropriate handlers.
     */
    private final MessageHub messageHub = MessageHub.getInstance();

    /**
     * Constructs a new {@code Client} and initializes the singleton instance.
     */
    public Client() {
        instance = this;  // Set the singleton instance
    }

    /**
     * Returns the singleton {@code Client} instance.
     *
     * @return The singleton {@code Client}.
     */
    public static Client getInstance() {
        return instance;
    }

    /**
     * Retrieves the client's unique identifier.
     *
     * @return The client UUID as a {@link String}.
     */
    public static String getMyClientId() {
        return ThinkOutsideTheRoom.client.getClientId();
    }

    

    /**
     * Retrieves the chat panel UI component from the client chat manager.
     *
     * @return The {@link ChatPanel} used for displaying and sending chat messages.
     */
    public ChatPanel getChatPanel() {
        return this.clientChatManager.getChatPanel();
    }

    /**
     * The core networking loop for the client. This method:
     * <ul>
     *   <li>Opens a {@link DatagramSocket}.</li>
     *   <li>Starts the {@link ReliableUDPSender} and {@link AckProcessor}.</li>
     *   <li>Begins background loops for receiving and sending messages.</li>
     *   <li>Blocks the main thread indefinitely (via {@link Thread#join()}).</li>
     * </ul>
     *
     * <p>Call this in its own thread to avoid blocking the UI or other processes.</p>
     */
    public void run() {
        try {
            // Initialize the client socket once.
            clientSocket = new DatagramSocket();
            int chosenPort = clientSocket.getLocalPort();
            CLIENT_PORT = chosenPort;

            clientSocketBestEffort = new DatagramSocket();
            int chosenPortBestEffort = clientSocketBestEffort.getLocalPort();
            CLIENT_PORT = chosenPortBestEffort;

            // Initialize the reliable sender without a fixed destination.
            myReliableUDPSender = new ReliableUDPSender(clientSocket, 50, 1000, outgoingQueue);

            ackProcessor = new AckProcessor();
            ackProcessor.init(outgoingQueue);
         

            // Receiver Task: Continuously listen for UDP packets and enqueue decoded messages.
        
            // 1) Reliable-channel receiver thread
            new Thread(() -> {
                byte[] buf = new byte[1024];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);

                while (true) {
                    try {
                        clientSocket.receive(pkt);
                        String raw = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(),
                                                StandardCharsets.UTF_8);
                        Message msg = MessageCodec.decode(raw);
                        InetSocketAddress sender =
                            new InetSocketAddress(pkt.getAddress(), pkt.getPort());

                        // ACKs for reliable path
                        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
                            Object[] params = msg.getParameters();
                            if (params != null && params.length > 0) {
                                myReliableUDPSender.acknowledge(params[0].toString());
                                System.out.println("Client: ACKed UUID " + params[0]);
                            }
                            continue;
                        }

                        // Enqueue ACKs back for any non-GAME messages
                        if (msg.getUUID() != null
                            && !msg.getUUID().isEmpty()
                            && !"GAME".equalsIgnoreCase(msg.getOption())) {
                            AckProcessor.enqueue(sender, msg.getUUID());
                        }

                        // All other reliable messages
                        messageHub.dispatch(msg);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "reliable-receiver").start();


            // 2) Best-effort CHANNEL receiver thread
            new Thread(() -> {
                byte[] buf = new byte[1024];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);

                while (true) {
                    try {
                        clientSocketBestEffort.receive(pkt);
                        String raw = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(),
                                                StandardCharsets.UTF_8);
                        Message msg = MessageCodec.decode(raw);

                        // Only GAME messages belong here
                        if ("GAME".equalsIgnoreCase(msg.getOption())) {
                            messageHub.dispatch(msg);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "besteffort-receiver").start();



            

            AsyncManager.runLoop(() -> {
            try {
                OutgoingMessage om = outgoingQueue.take();  // blocks until there’s one

                if ("GAME".equals(om.msg.getOption())) {
                    // Best-effort: fire-and-forget via raw UDP
                    byte[] raw = MessageCodec.encode(om.msg)
                                            .getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(
                        raw,
                        raw.length,
                        om.address,
                        om.port
                    );
                    // use whatever DatagramSocket you share for best-effort
                    instance.clientSocketBestEffort.send(packet);

                } else {
                    // Reliable path
                    myReliableUDPSender.send(om);
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;  // stop the loop if interrupted
            } catch (IOException ioe) {
                // best-effort: drop on I/O error
                ioe.printStackTrace();
            }
        });

            


            // Block the main thread indefinitely.
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }




    /**
     * Sends a {@link Message} in a static context by updating its concealed parameters
     * with the current username, then enqueuing it for sending.
     *
     * @param msg The {@link Message} to be sent.
     */
    public static void sendMessageStatic(Message msg) 
    {
        // Access the singleton instance's username.
        String currentUsername = instance.username.get();
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null) {
            concealed = new String[]{ currentUsername };
        } else {
            String[] newConcealed = new String[concealed.length + 1];
            System.arraycopy(concealed, 0, newConcealed, 0, concealed.length);
            newConcealed[newConcealed.length - 1] = currentUsername;
            concealed = newConcealed;
        }
        msg.setConcealedParameters(concealed);

        // Enqueue the message for sending.
        AsyncManager.run(() -> {
            try {
                InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);
                // this can throw InterruptedException:
                outgoingQueue.putFirst(new OutgoingMessage(msg, dest, SERVER_PORT));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // restore interrupted flag so higher-level code knows:
                Thread.currentThread().interrupt();
                // optionally log or silently drop
            }
        });
    }

    /**
     * Sends a message using a best-effort approach.
     * This method bypasses the outgoing queue and sends the message immediately.
     *
     * @param msg The {@link Message} to send.
     */
    public static void sendMessageBestEffort(Message msg) {
        try {
            if (Client.getInstance().myReliableUDPSender.hasBacklog() > 0) {
                return;  // postpone this update
            }

            // Update the concealed parameters with the current username
            String currentUsername = instance.username.get();
            String[] concealed = msg.getConcealedParameters();
            if (concealed == null) {
                concealed = new String[]{ currentUsername };
            } else {
                String[] newConcealed = new String[concealed.length + 1];
                System.arraycopy(concealed, 0, newConcealed, 0, concealed.length);
                newConcealed[newConcealed.length - 1] = currentUsername;
                concealed = newConcealed;
            }
            msg.setConcealedParameters(concealed);

            // Prepare destination
            InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);

            // Enqueue for best-effort sending: push to the tail of the deque.
            // This will block only if the deque is full; you can switch to offerLast(…)
            // if you prefer to handle a full-queue case yourself.
            outgoingQueue.putLast(
                new OutgoingMessage(msg.clone(), dest, SERVER_BE_PORT)
            );

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // restore interrupted status and drop the message
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // other unexpected exceptions
            e.printStackTrace();
        }
    }


    /**
     * Sends an ACK back to the server for the given received ACK message.
     *
     * @param receivedMessage the ACK Message we just got from the server
     */
    /**
     * A static helper that sends an ACK for the given message
     * by delegating to the singleton’s AckProcessor.
     */
    public static void acknowledge(Message receivedMessage) {
        // make sure the client has been initialized
        Client self = Client.getInstance();
        if (self == null || self.ackProcessor == null) {
            System.err.println("Cannot ACK before client is running");
            return;
        }

        // extract the UUID
        Object[] params = receivedMessage.getParameters();
        if (params == null || params.length == 0) {
            System.err.println("ACK received with no UUID parameter");
            return;
        }
        String uuid = params[0].toString();

        try {
            InetAddress addr = InetAddress.getByName(SERVER_ADDRESS);
            InetSocketAddress dest = new InetSocketAddress(addr, SERVER_PORT);
            //self.ackProcessor.addAck(dest, uuid);
            System.out.println("Sent ACK for UUID " + uuid + " to " + dest);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a one‑shot best‑effort ACK back to the server for the packet we
     * just received.  Runs asynchronously so the network thread never blocks.
     *
     * @param original the reliable message we need to acknowledge
     */
    private void sendPlainAckToServer(Message original) {

        // 1) sanity checks --------------------------------------------------
        String uuid = original.getUUID();
        if (uuid == null || uuid.isEmpty()) {
            System.err.println("Client‑ACK: message has no UUID → skip");
            return;
        }

        // 2) fire‑and‑forget -------------------------------------------------
        AsyncManager.run(() -> {
            try {
                /* Build the bare‑minimum ACK packet */
                Message ack = new Message("ACK", new Object[]{ uuid }, "GAME");
                String encoded = MessageCodec.encode(ack);
                byte[] data    = encoded.getBytes(StandardCharsets.UTF_8);

                /* Destination = the server we are already talking to */
                InetAddress addr = InetAddress.getByName(SERVER_ADDRESS);
                DatagramPacket pkt =
                    new DatagramPacket(data, data.length, addr, SERVER_PORT);

                clientSocket.send(pkt);

                System.out.println("→ Sent best‑effort ACK to server for UUID "
                                + uuid);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }




    /**
     * Handles certain local client state updates based on message content.
     * For example, updating the client's username or triggering a quick login.
     *
     * @param msg The {@link Message} instructing state updates.
     */
    private void updateLocalClientState(Message msg) {
        if ("CHANGE_USERNAME".equalsIgnoreCase(msg.getMessageType())) {
            this.username.set(msg.getParameters()[0].toString());
            System.out.println("Username Changed to: " + msg.getParameters()[0].toString());
        }
        if ("FAST_LOGIN".equalsIgnoreCase(msg.getMessageType())) {
            login();
        } else {
            System.out.println("Unhandled response type: " + msg.getMessageType());
        }
    }

    public InetAddress getClientIp() {
        return (clientSocket == null) ? null : clientSocket.getLocalAddress();
    }

    /**
     * Processes server responses not related to ACK.
     * Common types include:
     * <ul>
     *   <li>PONG: For ping results.</li>
     *   <li>CREATE: Add a new game object to the client's {@link Game}.</li>
     *   <li>CHANGENAME: Rename an existing game object.</li>
     *   <li>LOGIN: Associate the client with a game object on the server.</li>
     *   <li>LOGOUT: Request to remove a given player from the game.</li>
     *   <li>DELETE: Remove a player from the client's {@link Game} object list.</li>
     *   <li>EXIT: Command the client to shut down.</li>
     * </ul>
     *
     * @param msg The response {@link Message} from the server.
     */
    private void processServerResponse(Message msg) {
        if ("PONG".equalsIgnoreCase(msg.getMessageType())) {
            if (pingManager != null) {
                // game.updatePingIndicator(pingManager.getTimeDifferenceMillis());
            }
            return;
        }

        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params != null) {
                String serverUuid = params[0].toString();
                String objectType = params[1].toString();
                Object[] remainingParams = java.util.Arrays.copyOfRange(params, 2, params.length);
                Future<GameObject> futureObj = game.addGameObjectAsync(objectType, serverUuid, remainingParams);
                try {
                    GameObject newObj = futureObj.get();
                    System.out.println("Created new game object with UUID: " + serverUuid
                            + " and name: " + newObj.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("CREATE RESPONSE message does not contain enough parameters.");
            }
        } else if ("CHANGENAME".equalsIgnoreCase(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            String objectID = params[0].toString();
            String newObjectName = params[1].toString();
            List<GameObject> gameObjectList = game.getGameObjects();
            for (GameObject gameObject : gameObjectList) {
                if (gameObject.getId().equals(objectID)) {
                    // If it is the object we are controlling, also change our local username
                    if (gameObject.getName().equals(username.get())) {
                        username.set(newObjectName);
                    }
                    gameObject.setName(newObjectName);
                }
            }
        } else if ("LOGIN".equalsIgnoreCase(msg.getMessageType().trim())) {
            System.out.println("Logging in...");
            System.out.println(msg);
            if (msg.getParameters() == null || msg.getParameters().length < 1) {
                System.err.println("Error: LOGIN response missing parameters");
                return;
            }
            String assignedUUID = msg.getParameters()[0].toString();
            System.out.println("LOGIN confirmed for UUID: " + assignedUUID);
            for (GameObject gameObject : game.getGameObjects()) {
                if (gameObject.getId().equals(assignedUUID)) {
                    // Rebind local input handling to the newly confirmed object
                    SwingUtilities.invokeLater(() -> {
                        // game.rebindKeyListeners(gameObject.getName());
                        instance.username.set(gameObject.getName());
                        // game.updateGamePanel();
                    });
                }
            }
        } else if ("EXIT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+", ""))) {
            System.out.println("Exiting game...");
            System.exit(0);
        } else if ("LOGOUT".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Logging out " + msg.getParameters()[0].toString());
            try {
                String messagelogString = "DELETE{REQUEST}[" + msg.getParameters()[0].toString() + "]||";
                Message logoutMessage = MessageCodec.decode(messagelogString);
                sendMessageStatic(logoutMessage);
                System.out.println("Reliable sent: " + MessageCodec.encode(logoutMessage));
            } catch (Exception e) {
                System.err.println("Failed to send DELETE message reliably: " + e.getMessage());
                e.printStackTrace();
            }
        } else if ("DELETE".equalsIgnoreCase(msg.getMessageType())) {
            String targetPlayerId = msg.getParameters()[0].toString();
            for (GameObject go : game.getGameObjects()) {
                if (go.getName().equals(targetPlayerId)) {
                    game.getGameObjects().remove(go);
                    break;
                }
            }
        }

        if ("CREATEGAME".equalsIgnoreCase(msg.getMessageType())) {
            // Expecting the response parameters to contain the game UUID and name.
            if (msg.getParameters() != null && msg.getParameters().length >= 2) {
                String newGameUuid = msg.getParameters()[0].toString();
                String newGameName = msg.getParameters()[1].toString();
                // Create a new Game instance with the received UUID and friendly name.
                Game newGame = new Game(newGameUuid, newGameName);
                // Optionally start the game loop or reinitialize the UI.
                newGame.startPlayersCommandProcessingLoop();
                // Update the client's game reference.
                this.game = newGame;
                System.out.println("CREATEGAME response received. New game created: "
                        + newGameName + " with UUID: " + newGameUuid);
            } else {
                System.err.println("CREATEGAME response missing required parameters!");
            }
            return;
        }
    }

    /**
     * Continuously reads commands from the system console (if available) and enqueues
     * them as messages. Type "exit" (without quotes) to stop reading from the console.
     * <p>
     * Each command line is expected to be a parsable string containing message data,
     * which will be converted into a {@link Message} via {@link MessageCodec#decode(String)}.
     * If a command lacks the '|' separator, a default "||" is appended.
     * </p>
     */
    public void startConsoleReaderLoop() {
        // Check if a console is available
        if (System.console() == null) {
            System.out.println("No console available. Skipping console input loop.");
            return;
        }

        AsyncManager.runLoop(() -> {
            try {
                System.out.print("Command> ");
                if (!scanner.hasNextLine()) {
                    System.out.println("No input. Exiting input loop.");
                    return;
                }

                String command = scanner.nextLine();
                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Exiting console reader...");
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!command.contains("|")) {
                    command = command + "||";
                }
                try {
                    Message msg = MessageCodec.decode(command);
                    String[] concealedParams = { "something1", "something2" };
                    msg.setConcealedParameters(concealedParams);
                    sendMessageStatic(msg);
                } catch (Exception e) {
                    System.out.println("Invalid message format: " + command);
                }
            } catch (NoSuchElementException e) {
                System.out.println("Scanner closed or no input available.");
            }
        });
    }

    /**
     * Changes the username for this client instance.
     *
     * @param newUsername The desired new username.
     */
    public void setUsername(String newUsername) {
        username.set(newUsername);
    }

   

    /**
     * Sends multiple CREATE messages in quick succession, each creating a "Player"
     * object at incrementally spaced coordinates. Useful for testing or debugging.
     * This operation does not interrupt the main client loop.
     */
    public void sendBulkCreateMessages() {
        AsyncManager.run(() -> {
            for (int i = 0; i < 50; i++) {
                float x = 300.0f + i * 50;
                float y = 200.0f + i * 50;
                Object[] params = new Object[] {"Player", "Mike", x, y, 25.0f, "GameSession1"};
                Message createMsg = new Message("CREATE", params, "REQUEST");
                createMsg.setConcealedParameters(new String[] {"Mike", "GameSession1"});
                sendMessageStatic(createMsg);
                try {
                    Thread.sleep(50); // 50 ms delay to avoid flooding.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

        /**
     * Returns the port bound to the reliable‐UDP socket.
     */
    public int getClientPort() {
        // If the socket isn’t initialized yet, fall back to the field
        return (clientSocket != null)
            ? clientSocket.getLocalPort()
            : CLIENT_PORT;
    }

    /**
     * Returns the port bound to the best‐effort UDP socket.
     */
    public int getClientBestEffortPort() {
        return (clientSocketBestEffort != null)
            ? clientSocketBestEffort.getLocalPort()
            : CLIENT_BESTEFFORT_PORT;
    }


    /**
     * Performs a minimal "login" sequence by sending a CREATE message to spawn a Player,
     * followed by a LOGIN request. This demonstrates a basic handshake to join a game session.
     */
    public void login() {
        try {
            String createStr = "CREATE{REQUEST}[S:Player, S:" + username.get()
                    + ", F:100.0, F:100.0, F:25.0, S:GameSession1]||";
            Message createMsg = MessageCodec.decode(createStr);
            String[] concealedParams = { "something1", "something2" };
            createMsg.setConcealedParameters(concealedParams);
            sendMessageStatic(createMsg);
            System.out.println("Sent CREATE message: " + createStr);

            // Small delay before sending LOGIN to ensure CREATE is processed.
            Thread.sleep(1000);

            String loginStr = "LOGIN{REQUEST}[" + username.get() + "]||";
            Message loginMsg = MessageCodec.decode(loginStr);
            loginMsg.setConcealedParameters(concealedParams);
            sendMessageStatic(loginMsg);
            System.out.println("Sent LOGIN message: " + loginStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the server address to which this client will connect.
     *
     * @param host The new server host name or IP address.
     */
    public void setServerAddress(String host) {
        this.SERVER_ADDRESS = host;
    }

    /**
     * Sets the server port to which this client will connect.
     *
     * @param port The new server port number.
     */
    public void setServerPort(int port) {
        this.SERVER_PORT = port;
    }

    /**
     * The main method for launching this client application in a standalone context.
     * <p>
     * It prompts for a username (with a suggested nickname), sets the client's username,
     * optionally starts console reading, and begins the client's main loop in a new thread.
     * After a short delay, it can also initiate the login sequence if desired.
     * </p>
     *
     * @param args Command-line arguments (ignored).
     */
    public static void main(String[] args) {
        try {
            Scanner inputScanner = new Scanner(System.in);
            String suggestedNickname = Nickname_Generator.generateNickname();
            System.out.println("Suggested Nickname: " + suggestedNickname);
            System.out.println("Please enter to use suggested name, or type your own: ");
            String userName = inputScanner.nextLine();
            System.out.println("Entered nick: " + userName);
            if (userName.isEmpty()) {
                userName = suggestedNickname;
            }

            Client client = new Client();
            client.setUsername(userName);
            System.out.println("Set client username: " + userName);

            // client.initChatManager();
            // client.startGraphicsStuff();  // Hypothetical UI method
            client.startConsoleReaderLoop();

            new Thread(client::run).start();
            Thread.sleep(1000);

            // client.login();  // Uncomment if you want to login automatically
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
