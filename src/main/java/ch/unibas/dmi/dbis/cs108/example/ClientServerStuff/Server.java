package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHub;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import lombok.Getter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import ch.unibas.dmi.dbis.cs108.example.command.CommandRegistry;

import lombok.Setter;


@Getter
/**
 * The {@code Server} class implements a simple UDP-based server that
 * can handle both reliable and best-effort messages. It manages a set of
 * registered clients, game sessions via a GameSessionManager, and the logic for
 * handling various message types and requests (including creation of new game objects,
 * game session creation/joining, and selection of game objects).
 */
public class Server {

    // ================================
    // Singleton Implementation
    // ================================
    private Server() { }
    private static class SingletonHelper {
        private static final Server INSTANCE = new Server();
    }

    /**
     * Returns the singleton instance of the {@code Server}.
     *
     * @return the singleton {@code Server} instance
     */
    public static Server getInstance() {
        return SingletonHelper.INSTANCE;
    }

    // ================================
    // Server Properties
    // ================================

    /** The default UDP port on which this server listens for incoming messages. */
    public static final int SERVER_PORT = 9876;

    /** Provides the server-side chat manager used for handling chat messages. */
    @Setter
    private ChatManager.ServerChatManager serverChatManager;

    /**
     * Maps each connected user's username to the corresponding remote socket address
     * (IP + port).
     */
    @Getter
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();

    /** The underlying DatagramSocket used to receive and send UDP packets. */
    private DatagramSocket serverSocket;

    /** Responsible for sending messages over a "reliable" UDP mechanism. */
    private ReliableUDPSender reliableSender;

    /** Processes incoming ACK messages and notifies the ReliableUDPSender. */
    private AckProcessor ackProcessor;
    // The game session(s) are managed via GameSessionManager.
    private final GameSessionManager gameSessionManager = new GameSessionManager();
    // Install the MessageHub as a singleton.
    private final MessageHub messageHub = MessageHub.getInstance();
    // Outgoing messages queue.

    /** The primary/default game instance for this server. */
    private Game myGameInstance;

    /**
     * A queue for outgoing messages that should be sent asynchronously to clients.
     * Processed by a background loop in {@link #start()}.
     */
    private final ConcurrentLinkedQueue<OutgoingMessage> outgoingQueue = new ConcurrentLinkedQueue<>();

    /** Holds multiple active games by ID (UUID, etc.). */
    private final ConcurrentHashMap<String, Game> gameSessions = new ConcurrentHashMap<>();


    /** Handles command-based messages (e.g., "CREATE", "PING") for "REQUEST" operations. */
    private final CommandRegistry commandRegistry = new CommandRegistry();





    // ================================
    // Outgoing Message Inner Class
    // ================================

    /**
     * Represents a message to be sent to a particular network address.
     * Used internally by the {@code outgoingQueue}.
     */
    private static class OutgoingMessage {
        Message msg;
        InetAddress address;
        int port;

        /**
         * Constructs a new {@code OutgoingMessage}.
         *
         * @param msg     the message object to be sent
         * @param address the destination IP address
         * @param port    the destination UDP port
         */
        public OutgoingMessage(Message msg, InetAddress address, int port) {
            this.msg = msg;
            this.address = address;
            this.port = port;
        }
    }

    /**
     * Creates a new {@link Message} based on an original message, converting it
     * to a {@code RESPONSE} and replacing its parameters with {@code newParams}.
     *
     * @param original  the original request/message
     * @param newParams the new parameters for the response
     * @return a new {@code Message} marked as a {@code RESPONSE}
     */
    public static Message makeResponse(Message original, Object[] newParams) {
        String type = original.getMessageType();
        String[] concealed = original.getConcealedParameters();
        return new Message(type, newParams, "RESPONSE", concealed);
    }

    public void enqueueMessage(Message msg, InetAddress address, int port) {
        outgoingQueue.offer(new OutgoingMessage(msg, address, port));
    }


        /**
     * Returns a modified version of the requested name if that name is already taken.
     * It checks both the clientsMap (connected users) and the existing GameObjects.
     * If the requestedName is taken, it appends "_1", "_2", etc., until a free one is found.
     *
     * @param requestedName The new name the user is requesting
     * @return A guaranteed-unique name
     */
        public String findUniqueName(String requestedName) {
        String baseName = requestedName;
        int counter = 1;
        while (isNameTaken(requestedName)) {
            requestedName = baseName + "_" + counter++;
        }
        return requestedName;
    }

    /**
     * Helper function to see if a name is already taken by either:
     *  - Another user in the 'clientsMap', or
     *  - A game object in 'myGameInstance'.
     */
    private boolean isNameTaken(String name) {
        if (clientsMap.containsKey(name)) {
            return true;
        }
        // If you have a default game instance, check its objects as well.
        return false;
    }

    // ================================
    // Server Start Method
    // ================================

    /**
     * Initializes and starts this UDP server. Binds to {@link #SERVER_PORT},
     * launches background threads for sending and receiving packets, and registers
     * command and message handlers via reflection.
     */
    public void start() {
        try {
            InetAddress ipAddress = InetAddress.getByName("25.12.99.19");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);

            commandRegistry.initCommandHandlers();

            // Initialize the game instance so commands can work properly.
            myGameInstance = new Game("DefaultSessionID", "DefaultGameName");
            myGameInstance.startPlayersCommandProcessingLoop();

            reliableSender = new ReliableUDPSender(serverSocket, 50, 200);
            ackProcessor = new AckProcessor(serverSocket);
            ackProcessor.start();

            // Optionally initialize a default game session:
            // Game defaultGame = new Game("GameSession1", "Default Game");
            // defaultGame.startPlayersCommandProcessingLoop();
            // gameSessionManager.addGameSession("GameSession1", defaultGame);

            // Process outgoing messages.
            AsyncManager.runLoop(() -> {
                OutgoingMessage om = outgoingQueue.poll();
                if (om != null) {
                    try {
                        reliableSender.sendMessage(om.msg, om.address, om.port);
                        System.out.println("Sent message to " + om.address + ":" + om.port);
                    } catch (Exception e) {
                        System.err.println("Error sending message to " + om.address + ":" + om.port + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Listen for incoming packets.
            new Thread(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);

                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                        InetAddress clientAddress = packet.getAddress();
                        int clientPort = packet.getPort();
                        InetSocketAddress senderSocket = new InetSocketAddress(clientAddress, clientPort);
                        String messageString = new String(data);
                        System.out.println("Received: " + messageString + " from " + senderSocket);

                        Message msg = MessageCodec.decode(messageString);
                        AsyncManager.run(() -> processMessage(msg, senderSocket));
                        // Also dispatch the message to the MessageHub.
                        messageHub.dispatch(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================================
    // Message Processing
    // ================================

    /**
     * Processes an incoming {@link Message} from a specified sender address.
     * <p>
     * The method first normalizes the message type (e.g., "ACK", "CHAT", "REQUEST", etc.)
     * and optionally registers or verifies the user if the message includes concealed
     * parameters (e.g., username). It then attempts to find an appropriate
     * <ul>
     *   <li>If a matching handler exists, the server delegates processing by invoking
     *       {@code handler.handle(this, msg, senderSocket)}.</li>
     *   <li>If no matching handler is found, the method logs a message indicating that
     *       no handler is available for the given message type.</li>
     * </ul>
     *
     * @param msg          the {@link Message} object from the client
     * @param senderSocket the network socket (IP + port) of the message sender
     */
    private void processMessage(Message msg, InetSocketAddress senderSocket) {
        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
            String ackUuid = msg.getParameters()[0].toString();
            reliableSender.acknowledge(ackUuid);
            System.out.println("Processed ACK message for UUID " + msg.getUUID());
            return;
        }
        if ("CHAT".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Processed CHAT message 1 ");
            if (serverChatManager == null) {
                serverChatManager = new ChatManager.ServerChatManager();
            }
            if (msg.getUUID() != null && !msg.getUUID().isEmpty()) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
            }
            AsyncManager.run(() -> broadcastMessageToAll(msg));
            System.out.println("Processed CHAT message 2");
            return;
        }

        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length >= 2) {
            String username = concealed[concealed.length - 1];
            if (clientsMap.containsKey(username)) {
                InetSocketAddress existingSocket = clientsMap.get(username);
                if (!existingSocket.equals(senderSocket)) {
                    if (msg.getUUID() != null && !msg.getUUID().isEmpty()) {
                        ackProcessor.addAck(senderSocket, msg.getUUID());
                    }
                    String suggestedNickname = Nickname_Generator.generateNickname();
                    Message collisionResponse = new Message("NAME_TAKEN", new Object[]{suggestedNickname}, "RESPONSE");
                    collisionResponse.setUUID("");
                    enqueueMessage(collisionResponse, senderSocket.getAddress(), senderSocket.getPort());
                    return;
                }
            } else {
                clientsMap.put(username, senderSocket);
                synchronizeNewClient(username, senderSocket);
            }
            System.out.println("Registered user: " + username + " at " + senderSocket + ". Total clients: " + clientsMap.size());
            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }
            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                processMessageBestEffort(msg, senderSocket);
            } else if ("REQUEST".equalsIgnoreCase(msg.getOption())) {
                AsyncManager.run(() -> handleRequest(msg, username));
            } else {
                broadcastMessageToOthers(msg, username);
            }
        } else {
            System.out.println("Concealed parameters missing or too short.");
        }
    }
   

    /**
     * Sends the given {@link Message} to all connected clients except the sender.
     * This is done in a "best effort" manner, i.e., without the reliable UDP layer.
     *
     * @param msg          the message to broadcast
     * @param senderSocket the socket of the original sender (excluded from broadcast)
     */
    private void processMessageBestEffort(Message msg, InetSocketAddress senderSocket) {
        try {
            for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
                if (!entry.getValue().equals(senderSocket)) {
                    InetAddress dest = entry.getValue().getAddress();
                    int port = entry.getValue().getPort();
                    String encoded = MessageCodec.encode(msg);
                    byte[] data = encoded.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, dest, port);
                    serverSocket.send(packet);
                    System.out.println("Best effort sent message to " + entry.getKey() + " at " + entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a given message to <strong>all</strong> connected clients using the reliable queue.
     *
     * @param msg the message to broadcast
     */
    public void broadcastMessageToAll(Message msg) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            try {
                enqueueMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                System.out.println("Enqueued broadcast message to " + clientUsername + " at " + clientAddress);
            } catch (Exception e) {
                System.err.println("Error enqueuing message to " + clientUsername + " at " + clientAddress + ": " + e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients <em>except</em> the one
     * specified by {@code excludedUsername}. Useful for scenarios where
     * the original sender does not need to receive its own message.
     *
     * @param msg             the message to broadcast
     * @param excludedUsername the username to exclude from receiving the message
     */
    public void broadcastMessageToOthers(Message msg, String excludedUsername) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            if (!clientUsername.equals(excludedUsername)) {
                try {
                    enqueueMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                    System.out.println("Enqueued message to " + clientUsername + " at " + clientAddress);
                } catch (Exception e) {
                    System.err.println("Error enqueuing message to " + clientUsername + " at " + clientAddress + ": " + e.getMessage());
                }
            }
        }
    }

    // ================================
    // Request Handling
    // ================================

    /**
     * Handles a request-type message by looking up the appropriate {@link CommandHandler}
     * from {@link #commandRegistry} and delegating the logic.
     * <p>
     * If no matching handler is found, sends a default response message to the sender.
     *
     * @param msg           the request message
     * @param senderUsername the username of the client who sent this request
     */
    private void handleRequest(Message msg, String senderUsername) {
        // Get the raw message type and normalize it.
        String rawType = msg.getMessageType();
        String commandType = rawType.replaceAll("\\s+", "").toUpperCase();
        System.out.println("Raw type: '" + rawType + "' normalized: '" + commandType + "'");

        // Lookup the handler in the registry.
        CommandHandler handler = commandRegistry.getHandler(commandType);
        if (handler != null) {
            // Delegate handling to the appropriate CommandHandler.
            handler.handle(this, msg, senderUsername);
        } else {
            // Handler not found; print debug message and send default response.
            System.out.println("Unknown request type: " + msg.getMessageType());
            Message defaultResponse = makeResponse(msg, msg.getParameters());
            InetSocketAddress senderAddress = clientsMap.get(senderUsername);
            if (senderAddress != null) {
                enqueueMessage(defaultResponse, senderAddress.getAddress(), senderAddress.getPort());
                System.out.println("Sent default response to " + senderUsername);
            } else {
                System.err.println("Sender address not found for user: " + senderUsername);
            }
        }
    }

    private void synchronizeNewClient(String username, InetSocketAddress clientSocket) {
        // 1) For each existing game in the GameSessionManager:
        for (Map.Entry<String, Game> entry : gameSessionManager.getAllGameSessions().entrySet()) {
            String gameId = entry.getKey();
            Game game = entry.getValue();

            // a) Send a CREATEGAME message, so the client knows this game ID + name.
            //    The client can treat this exactly like if the server had just created the game.
            Message createGameMsg = new Message(
                    "CREATEGAME",
                    new Object[]{ gameId, game.getGameName() },
                    "RESPONSE"
            );
            enqueueMessage(createGameMsg, clientSocket.getAddress(), clientSocket.getPort());

            // b) For each GameObject in that game, send a CREATEGO message with all constructor params.
            for (GameObject go : game.getGameObjects()) {
                // The CREATEGO parameters:
                //    0 -> gameObject UUID
                //    1 -> game session ID
                //    2 -> object type (from the class or however you identify it)
                //    3... -> constructor parameters
                String objectUuid = go.getId();
                String objectType = go.getClass().getSimpleName();

                // The constructor parameters are exactly what that object's getConstructorParamValues() returns.
                Object[] constructorParams = go.getConstructorParamValues();

                // Build a combined param array:
                //    [ 0: objectUuid, 1: gameId, 2: objectType, 3..: constructorParams ]
                Object[] createGoParams = new Object[3 + constructorParams.length];
                createGoParams[0] = objectUuid;
                createGoParams[1] = gameId;
                createGoParams[2] = objectType;
                System.arraycopy(constructorParams, 0, createGoParams, 3, constructorParams.length);

                Message createGoMsg = new Message("CREATEGO", createGoParams, "RESPONSE");
                enqueueMessage(createGoMsg, clientSocket.getAddress(), clientSocket.getPort());
            }
        }
    }





    // ================================
    // Main Method
    // ================================
    public static void main(String[] args) {
        Server.getInstance().start();
    }
}
