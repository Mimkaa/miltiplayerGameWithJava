package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import chat.ChatManager;
import lombok.Getter;

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

@Getter
/**
 * The {@code Server} class implements a simple UDP-based server that
 * can handle both reliable and best-effort messages. It manages a
 * set of registered clients, a game instance, and the logic for
 * handling various message types and requests (including creation
 * of new game objects and user login/logout).
 */
public class Server {

    // ================================
    // Singleton Implementation
    // ================================

    /**
     * Private constructor to prevent external instantiation.
     */
    private Server() {
    }

    /**
     * Static inner helper class that holds the singleton instance.
     */
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

    public static final int SERVER_PORT = 9876;

    private ChatManager.ServerChatManager serverChatManager;

    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();

    private DatagramSocket serverSocket;

    private ReliableUDPSender reliableSender;

    private AckProcessor ackProcessor;

    private Game myGameInstance;

    private final ConcurrentLinkedQueue<OutgoingMessage> outgoingQueue = new ConcurrentLinkedQueue<>();

    // ================================
    // Outgoing Message Inner Class
    // ================================

    private static class OutgoingMessage {
        Message msg;
        InetAddress address;
        int port;

        public OutgoingMessage(Message msg, InetAddress address, int port) {
            this.msg = msg;
            this.address = address;
            this.port = port;
        }
    }

    public static Message makeResponse(Message original, Object[] newParams) {
        String type = original.getMessageType();
        String[] concealed = original.getConcealedParameters();
        Message response = new Message(type, newParams, "RESPONSE", concealed);
        return response;
    }

    private void enqueueMessage(Message msg, InetAddress address, int port) {
        outgoingQueue.offer(new OutgoingMessage(msg, address, port));
    }

    // ================================
    // Server Start Method
    // ================================

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

            // Send messages from outgoing queue
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

            // Listen for incoming packets
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
            }

            System.out.println("Registered user: " + username + " at " + senderSocket
                    + ". Total clients: " + clientsMap.size());

            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }

            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                myGameInstance.addIncomingMessage(msg);
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

    // ================================
    // Request Handling
    // ================================

    private void handleRequest(Message msg, String senderUsername) {
        switch (msg.getMessageType().replaceAll("\\s+", "").toUpperCase()) {
            case "CREATE":
                handleCreateRequest(msg, senderUsername);
                break;
            case "PING":
                handlePingRequest(senderUsername);
                break;
            case "GETOBJECTID":
                handleGetObjectIdRequest(msg);
                break;
            case "CHANGENAME":
                handleChangeNameRequest(msg);
                break;
            case "USERJOINED":
                handleUserJoinedRequest(msg);
                break;
            case "LOGOUT":
            case "EXIT":
                handleLogoutOrExitRequest(msg, senderUsername);
                break;
            case "LOGIN":
                handleLoginRequest(msg, senderUsername);
                break;
            case "DELETE":
                handleDeleteRequest(msg, senderUsername);
                break;
           
            default:
                System.out.println("Unknown request type: " + msg.getMessageType());
        }
    }

    private void handleCreateRequest(Message msg, String senderUsername) {
        Object[] originalParams = msg.getParameters();
        String serverGeneratedUuid = UUID.randomUUID().toString();
    
        // 1) Prepend server-generated UUID to the original parameters
        Object[] newParams = new Object[originalParams.length + 1];
        newParams[0] = serverGeneratedUuid;
        System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);
    
        // 2) Build the CREATE response message
        Message responseMsg = new Message("CREATE", newParams, "RESPONSE");
    
        // 3) Optionally add a debug printout of the entire message
        //    If your Message class has a good toString(), you can do:
        // System.out.println("Constructed CREATE message: " + responseMsg);
        //
        // Or if you want to see the exact encoded form, do something like:
        // System.out.println("Constructed CREATE message: " + MessageCodec.encode(responseMsg));
    
        // 4) Actually create the GameObject asynchronously
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
    
        // 5) Broadcast this CREATE message to all known clients
        //    This ensures everyone sees the newly created object.
        System.out.println("Broadcasting CREATE to all: " + responseMsg);
        broadcastMessageToAll(responseMsg);
    
        // 6) Now, for the new user, also send them CREATE messages for each existing object
        //    so they can synchronize state. If you only want the new user to receive these,
        //    you need to look up the *new user’s* InetSocketAddress and send them there.
        for (String username : clientsMap.keySet()) {
            if (!username.equals(senderUsername)) {
                GameObject gameObject = myGameInstance.getGameObjects()
                .stream()
                .filter(obj -> obj.getName().equals(username))
                .findFirst()
                .orElse(null);

                Object[] constructorParameters = gameObject.getConstructorParamValues();
                Object[] finalParameters = new Object[constructorParameters.length + 2];

                // 1) Insert the UUID
                finalParameters[0] = gameObject.getId();

                // 2) Insert the type
                String objectType = gameObject.getClass().getSimpleName(); 
                // Or store the type somewhere in the object if your classes vary
                // e.g., if (gameObject instanceof Player) type="Player"; or a getType() method

                finalParameters[1] = objectType;

                // 3) Copy the old constructor params after the first two slots
                System.arraycopy(constructorParameters, 0, 
                                finalParameters, 2, 
                                constructorParameters.length);

                // Now finalParameters looks like:
                // [UUID, objectType, param0, param1, param2, ...]
                Message createResponseMessage = makeResponse(msg, finalParameters);
    
                // If you want to send these "existing object" CREATEs
                // to the *new user* only, do something like:
                InetSocketAddress newUserAddress = clientsMap.get(senderUsername);
                if (newUserAddress == null) {
                    System.err.println("No known address for user: " + senderUsername);
                    return; 
                }
                enqueueMessage(createResponseMessage,
                               newUserAddress.getAddress(),
                               newUserAddress.getPort());
    
               
            }
        }
    }

    private void handlePingRequest(String senderUsername) {
        InetSocketAddress senderAddress = clientsMap.get(senderUsername);
        if (senderAddress != null) {
            Message responseMsg = new Message("PONG", new Object[]{}, "RESPONSE");
            enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }

    private void handleGetObjectIdRequest(Message msg) {
        Object[] originalParams = msg.getParameters();
        String objectName = originalParams[0].toString();
        String objectID = "";

        for (GameObject gameObject : myGameInstance.getGameObjects()) {
            if (gameObject.getName().equals(objectName)) {
                objectID = gameObject.getId();
            }
        }

        Message responseMsg = new Message("GETOBJECTID", new Object[]{objectID}, "RESPONSE");
        broadcastMessageToAll(responseMsg);
    }

    private void handleChangeNameRequest(Message msg) {
        Object[] originalParams = msg.getParameters();
        String oldName = originalParams[0].toString();
        String newName = originalParams[1].toString();
    
        // 1) Update the game object in 'myGameInstance'
        String objectID = "";
        for (GameObject gameObject : myGameInstance.getGameObjects()) {
            if (gameObject.getName().equals(oldName)) {
                objectID = gameObject.getId();
                gameObject.setName(newName);
                break; // Assuming only one object matches
            }
        }
    
        // 2) Update the 'clientsMap' key from oldName -> newName
        //    For example, if you have: private final ConcurrentHashMap<String, InetSocketAddress> clientsMap;
        InetSocketAddress address = clientsMap.remove(oldName);
        if (address != null) {
            // Insert the new key
            clientsMap.put(newName, address);
        }
    
        // 3) Broadcast the change to everyone
        Message responseMsg = new Message("CHANGENAME", new Object[]{objectID, newName}, "RESPONSE");
        broadcastMessageToAll(responseMsg);
    }
    

    private void handleUserJoinedRequest(Message msg) {
        String nickname = msg.getParameters()[0].toString();
        boolean hasRepetition = clientsMap.containsKey(nickname);

        if (hasRepetition) {
            String suggestedNickname = Nickname_Generator.generateNickname();
            Message responseMsg = new Message("USERJOINED", new Object[]{suggestedNickname}, "RESPONSE");

            InetSocketAddress clientAddress = clientsMap.get(nickname);
            if (clientAddress != null) {
                enqueueMessage(responseMsg, clientAddress.getAddress(), clientAddress.getPort());
            }
        }
    }

    private void handleLogoutOrExitRequest(Message msg, String senderUsername) {
        String type = msg.getMessageType().replaceAll("\\s+", "").toUpperCase();
        System.out.println("Client " + type + ": " + senderUsername);

        Message logoutMessage = new Message(type, msg.getParameters(), "RESPONSE");
        InetSocketAddress clientAddress = clientsMap.get(senderUsername);
        if (clientAddress != null) {
            enqueueMessage(logoutMessage, clientAddress.getAddress(), clientAddress.getPort());
        }

        clientsMap.remove(senderUsername);
        System.out.println("Removed user: " + senderUsername);
    }

    private void handleLoginRequest(Message msg, String senderUsername) {
        String firstParam = msg.getParameters()[0].toString();
        Object[] newParams = new Object[1];

        for (GameObject gameObject : myGameInstance.getGameObjects()) {
            if (firstParam.equals(gameObject.getName())) {
                newParams[0] = gameObject.getId();
                break;
            }
        }

        Message loginMessage = new Message("LOGIN", newParams, "RESPONSE");

        InetSocketAddress clientAddr = clientsMap.get(senderUsername);
        if (clientAddr != null) {
            enqueueMessage(loginMessage, clientAddr.getAddress(), clientAddr.getPort());
        }
    }

    private void handleDeleteRequest(Message msg, String senderUsername) {
        System.out.println("Client deleting: " + senderUsername);
        Message deleteMessage = makeResponse(msg, msg.getParameters());
        broadcastMessageToAll(deleteMessage);

        String targetPlayerName = msg.getParameters()[0].toString();
        myGameInstance.getGameObjects().removeIf(go -> go.getName().equals(targetPlayerName));
    }

    

    // ================================
    // Main Method
    // ================================

    public static void main(String[] args) {
        Server.getInstance().start();
    }
}
