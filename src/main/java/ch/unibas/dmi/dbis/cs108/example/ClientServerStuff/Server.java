package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHub;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import lombok.Getter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import ch.unibas.dmi.dbis.cs108.example.command.CommandRegistry;
import java.util.concurrent.LinkedBlockingQueue;


import lombok.Setter;

/**
 * The {@code Server} class implements a UDP-based game server that handles both reliable and
 * best-effort messaging for multiplayer game sessions. It manages client connections, game
 * state synchronization, and message routing between clients.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Singleton pattern implementation for global server access</li>
 *   <li>Reliable UDP messaging with acknowledgment handling</li>
 *   <li>Client registration and unique name management</li>
 *   <li>Game session management through {@link GameSessionManager}</li>
 *   <li>Command-based request handling via {@link CommandRegistry}</li>
 *   <li>Asynchronous message processing with outgoing queue</li>
 *   <li>Broadcast capabilities to all or specific clients</li>
 * </ul>
 *
 * <p>The server handles several message types:
 * <ul>
 *   <li><strong>ACK</strong>: Message acknowledgment handling</li>
 *   <li><strong>CHAT</strong>: Chat message broadcasting</li>
 *   <li><strong>REQUEST</strong>: Command-based operations</li>
 *   <li><strong>GAME</strong>: Best-effort game state updates</li>
 * </ul>
 */
@Getter
public class Server {

    // ================================
    // Singleton Implementation
    // ================================

    /**
     * Private constructor to prevent instantiation.
     */
    private Server() { }

    /**
     * Helper class for lazy initialization of the singleton instance.
     */
    private static class SingletonHelper {
        /** The singleton instance of the Server. */
        private static final Server INSTANCE = new Server();
    }

    /**
     * Returns the singleton instance of the Server.
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
    public static int SERVER_PORT = 9876;

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

    /** The game session(s) are managed via GameSessionManager. */
    private final GameSessionManager gameSessionManager = new GameSessionManager();

    /** Install the MessageHub as a singleton. */
    private final MessageHub messageHub = MessageHub.getInstance();

    /** The primary/default game instance for this server. */
    private Game myGameInstance;

    /**
     * A queue for outgoing messages that should be sent asynchronously to clients.
     * Processed by a background loop in {@link #start()}.
     */
    private final LinkedBlockingQueue<OutgoingMessage> outgoingQueue = new LinkedBlockingQueue<>();

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
        /** The message to be sent */
        Message msg;
        /** Destination IP address */
        InetAddress address;
        /** Destination UDP port */
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

    /**
     * Enqueues a message for asynchronous sending to the specified address.
     *
     * @param msg     the message to send
     * @param address the destination IP address
     * @param port    the destination UDP port
     */
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
     *
     * @param name The name to check
     * @return true if the name is already taken, false otherwise
     */
    private boolean isNameTaken(String name) {
        if (clientsMap.containsKey(name)) {
            return true;
        }
        return false;
    }

    // ================================
    // Server Start Method
    // ================================

    /**
     * Initializes and starts this UDP server. Binds to {@link #SERVER_PORT},
     * launches background threads for sending and receiving packets, and registers
     * command and message handlers via reflection.
     *
     * @param port The port number to listen on
     */
    public void start(String address, int port) {
        SERVER_PORT = port;
        try {
            InetAddress ipAddress = InetAddress.getByName(address);
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);

            commandRegistry.initCommandHandlers();

            // Initialize the game instance so commands can work properly.
            myGameInstance = new Game("DefaultSessionID", "DefaultGameName");
            myGameInstance.startPlayersCommandProcessingLoop();

            reliableSender = new ReliableUDPSender(serverSocket, 100, 200);
            ackProcessor = new AckProcessor(serverSocket);
            //ackProcessor.start();

            // Process outgoing messages.
            AsyncManager.runLoop(() -> {
                while (true) {
                    try {
                        // This can throw InterruptedException if the thread is interrupted
                        OutgoingMessage om = outgoingQueue.take();
            
                        // No null-check needed: take() never returns null
                        reliableSender.sendMessage(om.msg, om.address, om.port);
                        System.out.println("Sent message to " + om.address + ":" + om.port);
            
                    } catch (InterruptedException ie) {
                        // Restore the interrupt flag and exit the loop
                        Thread.currentThread().interrupt();
                        break;
            
                    } catch (Exception e) {
                        // Catch any other failures from sendMessage()
                        System.err.println("Error sending message");
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

                    // 1) Decode
                    String raw = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                    InetSocketAddress sender = new InetSocketAddress(packet.getAddress(), packet.getPort());
                    Message msg = MessageCodec.decode(raw);
                    System.out.println("Received: " + raw + " from " + sender);

                    // 2) If it’s an ACK *for* one of our reliable sends, clear it immediately
                    if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
                        String ackUuid = msg.getParameters()[0].toString();
                        reliableSender.acknowledge(ackUuid);
                        // don’t process it any further
                        continue;
                    }
                    else
                    {
                        // 3) Best‑effort ACK back to the user who sent it:
                        String[] concealed = msg.getConcealedParameters();
                        if (msg.getUUID() != null
                            && !msg.getUUID().isEmpty()
                            && concealed != null
                            && concealed.length > 0
                            && !"GAME".equalsIgnoreCase(msg.getOption())
                        ) {
                        // last concealed parameter is the username
                        String username = concealed[concealed.length - 1];
                        sendPlainAckAsync(username, msg);
                        }

                        // 4) Finally hand it off to the rest of your server logic
                        AsyncManager.run(() -> processMessage(msg, sender));
                        messageHub.dispatch(msg);
                    }

                    } catch (IOException e) {
                    e.printStackTrace();
                    }
                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the server on the default port (9876).
     */
    public void start() {
        start("localhost", SERVER_PORT);
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
                syncGames(senderSocket);
            }
            System.out.println("Registered user: " + username + " at " + senderSocket + ". Total clients: " + clientsMap.size());
            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }
            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                //processMessageBestEffort(msg, senderSocket);
                AsyncManager.run(() -> sendKeyEvent(msg));
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
     * Sends a key event message to all connected clients in a best-effort manner,
     * but only if the message type contains "KEY".
     */
    public void sendKeyEvent(Message msg) {
        try {
            // Check if the message type contains "KEY" (case-insensitive).
            if (!msg.getMessageType().toUpperCase().contains("KEY")) {
                //System.out.println("sendKeyEvent: Message type does not contain 'KEY'; skipping key event send.");
                return;
            }
            
            // Iterate over all connected clients.
            for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
                InetAddress dest = entry.getValue().getAddress();
                int port = entry.getValue().getPort();
                String encoded = MessageCodec.encode(msg);
                byte[] data = encoded.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, dest, port);
                serverSocket.send(packet);
                System.out.println("Key event sent to " + entry.getKey() + " at " + entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Asynchronously sends a best‑effort plain‑UDP ACK for the given message
     * back to the client identified by username.
     *
     * @param username the client’s username (must have been registered in clientsMap)
     * @param original the message we’re ACKing; its UUID will be echoed back
     */
    public void sendPlainAckAsync(String username, Message original) {
        InetSocketAddress dest = clientsMap.get(username);
        if (dest == null) {
            System.err.println("Cannot ACK: no address for user " + username);
            return;
        }
        String uuid = original.getUUID();
        if (uuid == null || uuid.isEmpty()) {
            System.err.println("Cannot ACK: message has no UUID");
            return;
        }

        AsyncManager.run(() -> {
            try {
                // build a minimal ACK message
                Message ack = new Message("ACK", new Object[]{ uuid }, "GAME");
                String payload = MessageCodec.encode(ack);
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);

                DatagramPacket packet = new DatagramPacket(
                    data, data.length,
                    dest.getAddress(), dest.getPort()
                );
                serverSocket.send(packet);

                System.out.println("→ Sent best‑effort ACK to "
                    + username + "@" + dest + " for UUID=" + uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }



    /**
     * Sends the given {@link Message} to all connected clients except the sender.
     * This is done in a "best effort" manner, i.e., without the reliable UDP layer.
     *
     * @param msg          the message to broadcast
     * @param senderSocket the socket of the original sender (excluded from broadcast)
     */
    public void sendMessageBestEffort(Message msg) {
        try {
            // Check if the message type contains "KEY"
            //boolean isKeyMessage = msg.getMessageType().contains("KEY");
            for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
                // If it's not a key message, then skip sending to the sender
                //if (!isKeyMessage && entry.getValue().equals(senderSocket)) {
                //    continue;
                //}
                InetAddress dest = entry.getValue().getAddress();
                int port = entry.getValue().getPort();
                String encoded = MessageCodec.encode(msg);
                byte[] data = encoded.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, dest, port);
                serverSocket.send(packet);
                System.out.println("Best effort sent message to " + entry.getKey() + " at " + entry.getValue());
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
    public void broadcastMessageToAll(Message original) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
          String clientUsername  = entry.getKey();
          InetSocketAddress dest = entry.getValue();
      
          // clone the original so constructor gives us a new UUID
          Message perClient = new Message(
            original.getMessageType(),
            original.getParameters(),
            original.getOption(),
            original.getConcealedParameters()
          );
      
          try {
            enqueueMessage(perClient, dest.getAddress(), dest.getPort());
            
          } catch (Exception e) {
            System.err.println("Error enqueuing to " 
              + clientUsername 
              + " at " + dest + ": " + e.getMessage());
          }
        }
      }
      
      public void broadcastMessageToOthers(Message original, String excludedUsername) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
          String clientUsername  = entry.getKey();
          if (clientUsername.equals(excludedUsername)) continue;
          InetSocketAddress dest = entry.getValue();
      
          // again, new instance with its own UUID
          Message perClient = new Message(
            original.getMessageType(),
            original.getParameters(),
            original.getOption(),
            original.getConcealedParameters()
          );
      
          try {
            enqueueMessage(perClient, dest.getAddress(), dest.getPort());
            
          } catch (Exception e) {
            System.err.println("Error enqueuing to " 
              + clientUsername 
              + " at " + dest + ": " + e.getMessage());
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

    /**
     * 1) Send CREATEGAME for every existing game.
     */
    public void syncGames(InetSocketAddress clientSocket) {
        for (Game game : gameSessionManager.getAllGameSessionsVals()) {
            String gameId   = game.getGameId();
            String gameName = game.getGameName();
            Message createGameMsg = new Message(
                "CREATEGAME",
                new Object[]{ gameId, gameName },
                "RESPONSE"
            );
            enqueueMessage(createGameMsg, clientSocket.getAddress(), clientSocket.getPort());
        }
    }

    /**
     * 2) For a given gameId, send CREATEGO for all of its objects.
     */
    public void syncGameObjects(InetSocketAddress clientSocket, String gameId) {
        Game game = gameSessionManager.getGameSession(gameId);
        if (game == null) return;

        for (GameObject go : game.getGameObjects()) {
            String objectUuid    = go.getId();
            String objectType    = go.getClass().getSimpleName();
            Object[] params      = go.getConstructorParamValues();

            // Build [uuid, gameId, type, ...ctorParams]
            Object[] createGoParams = new Object[3 + params.length];
            createGoParams[0] = objectUuid;
            createGoParams[1] = gameId;
            createGoParams[2] = objectType;
            System.arraycopy(params, 0, createGoParams, 3, params.length);

            Message createGoMsg = new Message("CREATEGO", createGoParams, "RESPONSE");
            enqueueMessage(createGoMsg, clientSocket.getAddress(), clientSocket.getPort());
        }
    }

    // ================================
    // Main Method
    // ================================

    /**
     * Main entry point for the server application.
     *
     * @param args Command line arguments (optional port number)
     */
    public static void main(String[] args) {
        if (args.length == 2) {
            try {
                String address = args[0];
                int port = Integer.parseInt(args[1]);
                Server.getInstance().start(address, port);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
            }
        } else {
            Server.getInstance().start("0.0.0.0", 9876);
            System.out.println("Server started at localhost:9876 (default)");
        }
    }
}