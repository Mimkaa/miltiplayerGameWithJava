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

    @Getter private InetSocketAddress lastSender;
    public void setLastSender(InetSocketAddress s) { this.lastSender = s; }

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

            reliableSender = new ReliableUDPSender(serverSocket, 100, 200);
            ackProcessor = new AckProcessor();
            ackProcessor.init(serverSocket);

            //ackProcessor.start();

            // Process outgoing messages.
            AsyncManager.runLoop(() -> {
                
                    try {
                        // This can throw InterruptedException if the thread is interrupted
                        OutgoingMessage om = outgoingQueue.take();
            
                        // No null-check needed: take() never returns null
                        reliableSender.sendMessage(om.msg, om.address, om.port);
                        System.out.println("Sent message to " + om.address + ":" + om.port);
            
                    } catch (InterruptedException ie) {
                        // Restore the interrupt flag and exit the loop
                        Thread.currentThread().interrupt();
                        return;
            
                    } catch (Exception e) {
                        // Catch any other failures from sendMessage()
                        System.err.println("Error sending message");
                        e.printStackTrace();
                    }
                
            });
            

            // Listen for incoming packets.
            new Thread(() -> {
                byte[] buf = new byte[1024];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            
                while (true) {
                    try {
                        serverSocket.receive(pkt);

                        InetSocketAddress sender =
                                new InetSocketAddress(pkt.getAddress(), pkt.getPort());
                        getInstance().setLastSender(sender);
            
                        String raw   = new String(pkt.getData(), pkt.getOffset(),
                                                   pkt.getLength(), StandardCharsets.UTF_8);
                        //InetSocketAddress sender =
                          //  new InetSocketAddress(pkt.getAddress(), pkt.getPort());
                        Message msg   = MessageCodec.decode(raw);
            
                        /* a) ACKs for *our* reliable sends --------------------- */
                        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
                            reliableSender.acknowledge(msg.getParameters()[0].toString());
                            continue;                       // done with this packet
                        }
            
                        /* b) queue best‑effort ACK back to the client ---------- */
                        if (msg.getUUID()!=null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                            AckProcessor.enqueue(sender, msg.getUUID());
                        }
            
                        /* c) application‑level handling ------------------------ */
                        //System.out.println(username);
                        //System.out.println(sender);
                        if ("GAME".equalsIgnoreCase(msg.getOption())) {
                            sendKeyEvent(msg);              // fire‑and‑forget
                        } else if ("REQUEST".equalsIgnoreCase(msg.getOption())) {
                            String username = findUserBySocket(sender);
                            System.out.println(username);
                            System.out.println(sender);
                            handleRequest(msg, username);
                        } else {
                            String username = findUserBySocket(sender);
                            broadcastMessageToOthers(msg, username);
                        }
            
                        /* d) notify higher‑level subsystems -------------------- */
                        messageHub.dispatch(msg);
            
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, "udp-receiver").start();


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
     * Reverse‑lookup: return the username that is mapped to the given
     * socket (IP + port) or {@code null} if we don’t know that socket yet.
     *
     * We simply scan the clientsMap; for the typical multiplayer‑game
     * server (tens, not thousands, of players) the O(n) cost is negligible.
     * If you expect hundreds‑plus clients, keep a second map
     *   socket → username
     * instead.
     */
    private String findUserBySocket(InetSocketAddress socket) {
        for (Map.Entry<String, InetSocketAddress> e : clientsMap.entrySet()) {
            if (e.getValue().equals(socket)) {
                return e.getKey();       // found it
            }
        }
        return null;                     // unknown connection
    }

    
    /**
     * Sends a key event message to all connected clients in a best-effort manner,
     * but only if the message type contains "KEY".
     */
    public void sendKeyEvent(Message msg) {
        // offload entire broadcast into the background
        AsyncManager.run(() -> {
            try {
                if (!msg.getMessageType().toUpperCase().contains("KEY")) {
                    return;
                }
                byte[] data = MessageCodec.encode(msg)
                                         .getBytes(StandardCharsets.UTF_8);
    
                // fire off sends in parallel
                clientsMap.values()
                          .parallelStream()
                          .forEach(destAddr -> {
                              try {
                                  DatagramPacket packet = new DatagramPacket(
                                      data, data.length,
                                      destAddr.getAddress(),
                                      destAddr.getPort()
                                  );
                                  serverSocket.send(packet);
                              } catch (IOException ioe) {
                                  ioe.printStackTrace();
                              }
                          });
    
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    

    /**
     * Renames a connected user in every place the server knows about:
     *   1) clientsMap    – socket lookup
     *   2) every Game    – Game#getUsers() set
     *
     * @return true if rename was performed, false if oldName not found
     *         or newName already taken.
     */
    public boolean renameUser(String oldName, String newName) {
        // debug: what was passed in, and what keys we actually have
        System.out.printf("renameUser called: oldName='%s', newName='%s'%n", oldName, newName);
        System.out.println("  clientsMap before → " + clientsMap.keySet());
    
        // 0) disallow trivial no-ops or collisions
        if (oldName.equals(newName)) {
            System.out.println("  abort: old==new");
            return false;
        }
        if (clientsMap.containsKey(newName)) {
            System.out.println("  abort: newName already in use");
            return false;
        }
    
        // 1) make sure the old name exists
        if (!clientsMap.containsKey(oldName)) {
            System.out.println("  abort: oldName not connected");
            return false;
        }
    
        // 2) perform the move
        InetSocketAddress socket = clientsMap.remove(oldName);
        clientsMap.put(newName, socket);
        System.out.println("  clientsMap after → " + clientsMap.keySet());
    
        // 3) update every game’s user lists
        for (Game g : gameSessionManager.getAllGameSessionsVals()) {
            if (g.getUsers().remove(oldName)) {
                g.getUsers().add(newName);
            }
        }
    
        System.out.printf("  rename succeeded: %s → %s%n", oldName, newName);
        return true;
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
     */
    public void sendMessageBestEffort(Message msg) {
        AsyncManager.run(() -> {
            try {
                byte[] data = MessageCodec.encode(msg)
                                          .getBytes(StandardCharsets.UTF_8);
    
                // send to all clients in parallel
                clientsMap.values()
                          .parallelStream()
                          .forEach(destAddr -> {
                              try {
                                  DatagramPacket packet = new DatagramPacket(
                                      data, data.length,
                                      destAddr.getAddress(),
                                      destAddr.getPort()
                                  );
                                  serverSocket.send(packet);
                              } catch (IOException ioe) {
                                  ioe.printStackTrace();
                              }
                          });
    
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    

    /**
     * Broadcasts a given message to <strong>all</strong> connected clients using the reliable queue.
     *
     * @param original the message to broadcast
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
            enqueueMessage(original, dest.getAddress(), dest.getPort());
            
          } catch (Exception e) {
            //System.err.println("Error enqueuing to " 
            //  + clientUsername 
            //  + " at " + dest + ": " + e.getMessage());
          }
        }
      }
      
      public void broadcastMessageToOthers(Message original, String excludedUsername) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
          String clientUsername  = entry.getKey();
          if (clientUsername.equals(excludedUsername)) continue;
          InetSocketAddress dest = entry.getValue();
      
          // again, new instance with its own UUID
          
      
          try {
            enqueueMessage(original, dest.getAddress(), dest.getPort());
            
          } catch (Exception e) {
            //System.err.println("Error enqueuing to " 
            //  + clientUsername 
            //  + " at " + dest + ": " + e.getMessage());
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
            System.out.println("RRRAAAARRRR");
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