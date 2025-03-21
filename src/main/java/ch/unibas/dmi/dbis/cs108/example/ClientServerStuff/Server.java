package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * The {@code Server} class implements a simple UDP-based server that
 * can handle both reliable and best-effort messages. It manages a
 * set of registered clients, a game instance, and the logic for
 * handling various message types and requests (including creation
 * of new game objects and user login/logout).
 * <p>
 * This server binds to a fixed port (default {@link #SERVER_PORT})
 * and continuously listens for incoming UDP packets. Incoming
 * messages are decoded and dispatched to {@link #processMessage(Message, InetSocketAddress)}
 * asynchronously.
 */
public class Server {

    /**
     * The default server port on which this server listens for incoming UDP traffic.
     */
    public static final int SERVER_PORT = 9876;

    /**
     * Returns the singleton instance of the {@code Server}.
     *
     * @return the singleton {@code Server} instance
     */
    public static Server getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * A helper class to hold the singleton instance of {@link Server}.
     */
    private static class SingletonHelper {
        private static final Server INSTANCE = new Server();
    }

    /**
     * A thread-safe map associating usernames with their respective socket addresses.
     * Key: {@code String} username, Value: {@link InetSocketAddress}.
     */
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();

    /**
     * The {@link DatagramSocket} used by this server to receive and send UDP packets.
     */
    private DatagramSocket serverSocket;

    /**
     * A {@link ReliableUDPSender} instance handling reliable message sending logic for this server.
     */
    private ReliableUDPSender reliableSender;

    /**
     * An {@link AckProcessor} that processes and sends ACK messages for reliably sent messages.
     */
    private AckProcessor ackProcessor;

    /**
     * The active {@link Game} instance to which game-related messages are dispatched.
     */
    private Game myGameInstance;

    /**
     * A thread-safe queue of {@link OutgoingMessage} objects awaiting transmission.
     */
    private final ConcurrentLinkedQueue<OutgoingMessage> outgoingQueue = new ConcurrentLinkedQueue<>();

    /**
     * A helper class associating a {@link Message} with its network destination (address and port).
     */
    private static class OutgoingMessage {
        Message msg;
        InetAddress address;
        int port;

        /**
         * Constructs an {@code OutgoingMessage} with the specified message and destination.
         *
         * @param msg     the message to send
         * @param address the destination IP address
         * @param port    the destination port
         */
        public OutgoingMessage(Message msg, InetAddress address, int port) {
            this.msg = msg;
            this.address = address;
            this.port = port;
        }
    }

    /**
     * Enqueues a message for sending, storing it in the {@link #outgoingQueue}.
     *
     * @param msg     the {@link Message} to send
     * @param address the destination IP address
     * @param port    the destination port
     */
    private void enqueueMessage(Message msg, InetAddress address, int port) {
        outgoingQueue.offer(new OutgoingMessage(msg, address, port));
    }

    /**
     * Initializes and starts the server, binding to a fixed port and continuously listening
     * for incoming UDP traffic. The server also initializes the {@link ReliableUDPSender},
     * the {@link AckProcessor}, and the main {@link Game} instance.
     * <p>
     * This method spawns a separate thread for listening and processing incoming packets,
     * as well as a loop for sending messages enqueued in {@link #outgoingQueue}.
     */
    public void start() {
        try {
            InetAddress ipAddress = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);

            reliableSender = new ReliableUDPSender(serverSocket, 50, 200);
            ackProcessor = new AckProcessor(serverSocket);
            ackProcessor.start();

            myGameInstance = new Game("GameSession1");
            myGameInstance.startPlayersCommandProcessingLoop();

            // Continuously poll and send messages from the outgoing queue.
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

            // A separate thread listens for incoming packets indefinitely.
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

                        // Dispatch message processing asynchronously.
                        AsyncManager.run(() -> processMessage(msg, senderSocket));

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
     * Processes an incoming {@link Message} from a specified sender. ACK messages
     * are handled immediately (removing the message from the reliable sender's
     * pending messages). Other messages are processed based on their type or option:
     * <ul>
     *   <li>Requests: might create new objects or handle login/logout logic</li>
     *   <li>GAME messages: forwarded to the {@link Game} instance and possibly broadcast</li>
     *   <li>Other messages: typically broadcast to other connected clients</li>
     * </ul>
     *
     * @param msg          the decoded {@link Message} received
     * @param senderSocket the source address and port of the message
     */
    private void processMessage(Message msg, InetSocketAddress senderSocket) {
        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
            String ackUuid = msg.getParameters()[0].toString();
            reliableSender.acknowledge(ackUuid);
            System.out.println("Processed ACK message for UUID " + msg.getUUID());
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
                    Message collisionResponse = new Message(
                            "NAME_TAKEN",
                            new Object[]{ suggestedNickname },
                            "RESPONSE"
                    );
                    collisionResponse.setUUID("");
                    enqueueMessage(collisionResponse, senderSocket.getAddress(), senderSocket.getPort());
                    return;
                }
            }
            else {
                clientsMap.put(username, senderSocket);
            }

            System.out.println("Registered user: " + username + " at " + senderSocket
                    + ". Total clients: " + clientsMap.size());

            System.out.println("Current clientsMap after registering \"" + username + "\":");
            clientsMap.forEach((user, address) -> System.out.println("  " + user + " -> " + address));

            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }

            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                myGameInstance.addIncomingMessage(msg);
                processMessageBestEffort(msg, senderSocket);
            } else if ("REQUEST".equalsIgnoreCase(msg.getOption())) {
                handleRequest(msg, username);
            } else {
                broadcastMessageToOthers(msg, username);
            }
        } else {
            System.out.println("Concealed parameters missing or too short.");
        }
    }

    /**
     * Sends a GAME-type message to all registered clients except the sender using
     * best-effort (unreliable) UDP. This approach bypasses the reliability mechanisms
     * of {@link ReliableUDPSender}.
     *
     * @param msg          the {@link Message} to send
     * @param senderSocket the origin of the message (excluded from distribution)
     */
    private void processMessageBestEffort(Message msg, InetSocketAddress senderSocket) {
        try {
            for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
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
     * Broadcasts a message to all clients currently tracked in {@link #clientsMap}.
     *
     * @param msg the {@link Message} to broadcast
     */
    public void broadcastMessageToAll(Message msg) {
        for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            try {
                enqueueMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                System.out.println("Enqueued broadcast message to " + clientUsername + " at " + clientAddress);
            } catch (Exception e) {
                System.err.println("Error enqueuing message to " + clientUsername + " at "
                        + clientAddress + ": " + e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a message to all clients except the one identified by the specified username.
     *
     * @param msg             the {@link Message} to broadcast
     * @param excludedUsername the name of the client to exclude
     */
    public void broadcastMessageToOthers(Message msg, String excludedUsername) {
        for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            if (!clientUsername.equals(excludedUsername)) {
                try {
                    enqueueMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                    System.out.println("Enqueued message to " + clientUsername + " at " + clientAddress);
                } catch (Exception e) {
                    System.err.println("Error enqueuing message to " + clientUsername + " at "
                            + clientAddress + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles various types of request messages, such as {@code CREATE}, {@code PING},
     * {@code GETOBJECTID}, and others. Depending on the request type, this method may
     * create new game objects in {@link #myGameInstance}, respond to ping messages,
     * or update/broadcast changes to all connected clients.
     *
     * @param msg            the incoming request {@link Message}
     * @param senderUsername the username of the sender
     */
    private void handleRequest(Message msg, String senderUsername) {
        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            Object[] originalParams = msg.getParameters();
            String serverGeneratedUuid = UUID.randomUUID().toString();

            Object[] newParams = new Object[originalParams.length + 1];
            newParams[0] = serverGeneratedUuid;
            System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);

            Message responseMsg = new Message("CREATE", newParams, "RESPONSE");
            responseMsg.setUUID("");

            Future<GameObject> futureObj = myGameInstance.addGameObjectAsync(
                    originalParams[0].toString(),
                    serverGeneratedUuid,
                    (Object[]) java.util.Arrays.copyOfRange(originalParams, 1, originalParams.length)
            );

            try {
                GameObject newObj = futureObj.get();
                System.out.println("Created new game object with UUID: " + serverGeneratedUuid
                        + " and name: " + newObj.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            broadcastMessageToAll(responseMsg);
        }

        if ("PING".equalsIgnoreCase(msg.getMessageType())) {
            InetSocketAddress senderAddress = clientsMap.get(senderUsername);
            if (senderAddress != null) {
                Message responseMsg = new Message("PONG", new Object[] {}, "RESPONSE");
                enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
                System.out.println("Enqueued PONG response to " + senderUsername + " at " + senderAddress);
            } else {
                System.out.println("Sender " + senderUsername + " not found in clientsMap!");
            }
            return;
        }

        if ("GETOBJECTID".equalsIgnoreCase(msg.getMessageType())) {
            Object[] originalParams = msg.getParameters();
            List<GameObject> gameObjectList = myGameInstance.getGameObjects();
            String objectName = originalParams[0].toString();
            String objectID = "";
            for (GameObject gameObject : gameObjectList) {
                if (gameObject.getName().equals(objectName)) {
                    objectID = gameObject.getId();
                }
            }
            Object[] newParams = new Object[1];
            newParams[0] = objectID;
            Message responseMsg = new Message("GETOBJECTID", newParams, "RESPONSE");
            responseMsg.setUUID("");
            broadcastMessageToAll(responseMsg);
        }

        if ("CHANGENAME".equalsIgnoreCase(msg.getMessageType())) {
            Object[] originalParams = msg.getParameters();
            List<GameObject> gameObjectList = myGameInstance.getGameObjects();
            String objectName = originalParams[0].toString();
            String newObjectName = originalParams[1].toString();
            String objectID = "";
            for (GameObject gameObject : gameObjectList) {
                if (gameObject.getName().equals(objectName)) {
                    objectID = gameObject.getId();
                    gameObject.setName(newObjectName);
                }
            }
            Object[] newParams = new Object[2];
            newParams[0] = objectID;
            newParams[1] = newObjectName;
            Message responseMsg = new Message("CHANGENAME", newParams, "RESPONSE");
            responseMsg.setUUID("");
            broadcastMessageToAll(responseMsg);
        }

        if ("USERJOINED".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+",""))) {
            Object[] originalParams = msg.getParameters();
            String nickname = originalParams[0].toString();
            boolean hasRepetition = false;
            for (String key : clientsMap.keySet()) {
                if (key.equals(nickname)) {
                    hasRepetition = true;
                }
            }
            if (hasRepetition) {
                String suggestedNickname = Nickname_Generator.generateNickname();
                Object[] newParams = new Object[1];
                newParams[0] = suggestedNickname;
                Message responseMsg = new Message("USERJOINED", newParams, "RESPONSE");
                InetSocketAddress clientAddress = clientsMap.get(nickname);
                if (clientAddress != null) {
                    enqueueMessage(responseMsg, clientAddress.getAddress(), clientAddress.getPort());
                }
            }
        }

        if ("LOGOUT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+",""))) {
            System.out.println("Client logging out: " + senderUsername);
            Message logoutMessage = new Message("LOGOUT", msg.getParameters(), "RESPONSE");
            logoutMessage.setUUID("");
            InetSocketAddress clientAddress = clientsMap.get(senderUsername);
            if (clientAddress != null) {
                enqueueMessage(logoutMessage, clientAddress.getAddress(), clientAddress.getPort());
            }
            System.out.println("Removed user: " + senderUsername);
            clientsMap.remove(senderUsername);
        }

        if ("LOGIN".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Client logging in: " + senderUsername);
            String firstParam = msg.getParameters()[0].toString();
            Object[] newParams = new Object[1];
            GameObject[] gameObjects = myGameInstance.getGameObjects().toArray(new GameObject[0]);
            for (GameObject gameObject : gameObjects) {
                if (firstParam.equals(gameObject.getName())) {
                    newParams[0] = gameObject.getId();
                    break;
                }
            }
            Message loginMessage = new Message("LOGIN", newParams, "RESPONSE");
            loginMessage.setUUID("");
            InetSocketAddress clientAddr = clientsMap.get(senderUsername);
            if (clientAddr != null) {
                enqueueMessage(loginMessage, clientAddr.getAddress(), clientAddr.getPort());
            }
        }

        if ("DELETE".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Client deleting: " + senderUsername);
            Message deleteMessage = new Message("DELETE", msg.getParameters(), "RESPONSE");
            broadcastMessageToAll(deleteMessage);
            String targetPlayerName = msg.getParameters()[0].toString();
            for (GameObject go : myGameInstance.getGameObjects()) {
                if (go.getName().equals(targetPlayerName)) {
                    myGameInstance.getGameObjects().remove(go);
                    break;
                }
            }
            for (GameObject go : myGameInstance.getGameObjects()) {
                System.out.println(go.getName());
            }
        }

        if ("EXIT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+",""))) {
            System.out.println("Client logging out: " + senderUsername);
            Message exitMessage = new Message("EXIT", msg.getParameters(), "RESPONSE");
            exitMessage.setUUID("");
            InetSocketAddress clientAddr = clientsMap.get(senderUsername);
            if (clientAddr != null) {
                enqueueMessage(exitMessage, clientAddr.getAddress(), clientAddr.getPort());
            }
            System.out.println("Removed user: " + senderUsername);
            clientsMap.remove(senderUsername);
        }
    }

    /**
     * The main entry point for running this server as a standalone application.
     *
     * @param args command-line arguments (none expected)
     */
    public static void main(String[] args) {
        new Server().start();
    }
}
