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

public class Server {
    public static final int SERVER_PORT = 9876;

    public static Server getInstance() {
        return SingletonHelper.INSTANCE;
    }
    private static class SingletonHelper {
        private static final Server INSTANCE = new Server();
    }
    
    // Concurrent map to track clients: key = username, value = InetSocketAddress.
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
    
    // Server socket.
    private DatagramSocket serverSocket;
    
    // Reliable sender and ACK processor.
    private ReliableUDPSender reliableSender;
    private AckProcessor ackProcessor;

    // The Game instance.
    private Game myGameInstance;
    
    // Outgoing message queue to hold messages for reliable sending.
    private final ConcurrentLinkedQueue<OutgoingMessage> outgoingQueue = new ConcurrentLinkedQueue<>();
    
    // Helper class to bundle a Message with its destination.
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
    
    // Enqueue a message for sending.
    private void enqueueMessage(Message msg, InetAddress address, int port) {
        outgoingQueue.offer(new OutgoingMessage(msg, address, port));
    }
    
    public void start() {
        try {
            // Bind server socket to localhost:SERVER_PORT.
            InetAddress ipAddress = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);
            
            // Initialize ReliableUDPSender and ACK processor.
            reliableSender = new ReliableUDPSender(serverSocket, 50, 200);
            ackProcessor = new AckProcessor(serverSocket);
            ackProcessor.start();
            
            myGameInstance = new Game("GameSession1");
            myGameInstance.startPlayersCommandProcessingLoop();
            
            // Start a dedicated sender thread that continuously polls outgoingQueue.
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
            
            // Start a thread that continuously listens for UDP packets.
            new Thread(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);
                        
                        // Safely copy the data.
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                        
                        InetAddress clientAddress = packet.getAddress();
                        int clientPort = packet.getPort();
                        InetSocketAddress senderSocket = new InetSocketAddress(clientAddress, clientPort);
                        
                        String messageString = new String(data);
                        System.out.println("Received: " + messageString + " from " + senderSocket);
                        
                        // Decode the message.
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
     * Synchronously processes a received message.
     * If the message is not an ACK, it extracts the username from the last concealed parameter,
     * registers the client in clientsMap, and then:
     * - For "GAME" messages, it updates the game logic/UI and sends the message best effort.
     * - For "REQUEST", it handles the request.
     * - Otherwise, it broadcasts the message to other clients.
     *
     * @param msg          The decoded message.
     * @param senderSocket The sender's socket address.
     */
    private void processMessage(Message msg, InetSocketAddress senderSocket) {
        // 1) Check if it's an ACK.
        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
            String ackUuid = msg.getParameters()[0].toString();
            reliableSender.acknowledge(ackUuid);
            System.out.println("Processed ACK message for UUID " + msg.getUUID());
            return;
        }
    
        // 2) Otherwise, handle REQUEST or broadcast.
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length >= 2) {
            // Username from the last element.
            String username = concealed[concealed.length - 1];

            // If the username already exists in the clients map, send a collision response.
            if (clientsMap.containsKey(username)) {
                InetSocketAddress existingSocket = clientsMap.get(username);
                // Only treat it as a collision if the sender socket differs.
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
                    collisionResponse.setUUID(""); // so "null" doesn't get appended in the encoding
                    enqueueMessage(collisionResponse, senderSocket.getAddress(), senderSocket.getPort());
                    return;
                }
            }
            // Register the new client.
            clientsMap.put(username, senderSocket);
    
            System.out.println("Registered user: " + username + " at " + senderSocket
                    + ". Total clients: " + clientsMap.size());
    
            System.out.println("Current clientsMap after registering \"" + username + "\":");
            clientsMap.forEach((user, address) -> System.out.println("  " + user + " -> " + address));
    
            // If the message has a UUID and is not a "GAME" message, register it in the ACK processor.
            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }
    
            // 3) Process based on message option synchronously.
            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                // For GAME messages, update game logic/UI.
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
     * Processes a GAME-type message using best-effort UDP send.
     * Sends the message to all clients except the sender.
     *
     * @param msg          The message to send.
     * @param senderSocket The sender's socket address.
     */
    private void processMessageBestEffort(Message msg, InetSocketAddress senderSocket) {
        try {
            InetAddress dest;
            int port;
            // For each registered client except the sender, send the message best effort.
            for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
                if (!entry.getValue().equals(senderSocket)) {
                    dest = entry.getValue().getAddress();
                    port = entry.getValue().getPort();
                    // Send using a simple UDP send (best effort) directly on the server socket.
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
     * Broadcasts a message to all known clients in clientsMap.
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
     * Broadcasts a message to all clients except the one identified by excludedUsername.
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
     * Handles a CREATE request message.
     * Example incoming message: CREATE{REQUEST}[Player, Alice, 100, 150, 10, GameSession1]||
     */
    private void handleRequest(Message msg, String senderUsername) {
        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            // 1) Extract the original parameters from the request.
            Object[] originalParams = msg.getParameters(); 
            // e.g. ["Player", "Alice", "100", "150", "10", "GameSession1"]
            
            // 2) Generate a new UUID on the server side.
            String serverGeneratedUuid = UUID.randomUUID().toString();
            
            // 3) Build a new parameter array:
            //    [serverGeneratedUuid, objectType, objectName, posX, posY, size, gameSession]
            Object[] newParams = new Object[originalParams.length + 1];
            newParams[0] = serverGeneratedUuid;
            System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);
            
            // 4) Create a new response message.
            Message responseMsg = new Message("CREATE", newParams, "RESPONSE");
            
            // 5) Set the response message's UUID to an empty string so the encoder won't append "null".
            responseMsg.setUUID("");
            
            // 6) Update the game by adding the new game object asynchronously.
            Future<GameObject> futureObj = myGameInstance.addGameObjectAsync(
                originalParams[0].toString(),  // object type, e.g. "Player"
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
            
            // 7) Broadcast the new RESPONSE message to all clients.
            broadcastMessageToAll(responseMsg);
        }
    
        if ("PING".equalsIgnoreCase(msg.getMessageType())) {
            // Look up the sender's InetSocketAddress from clientsMap using the senderUsername.
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
    
        // Template for additional commands:
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
                // You can enqueue or directly send this response as needed.
                InetSocketAddress clientAddress = clientsMap.get(nickname);
                if (clientAddress != null) {
                    enqueueMessage(responseMsg, clientAddress.getAddress(), clientAddress.getPort());
                }
            }
        }
    
        // Handles Logout-request.
        if ("LOGOUT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+",""))) {
            System.out.println("Client logging out: " + senderUsername);
    
            // Broadcast the LOGOUT RESPONSE to all clients.
            Message logoutMessage = new Message("LOGOUT", msg.getParameters(), "RESPONSE");
            logoutMessage.setUUID("");
            InetSocketAddress clientAddress = clientsMap.get(senderUsername);
            if (clientAddress != null) {
                enqueueMessage(logoutMessage, clientAddress.getAddress(), clientAddress.getPort());
            }
    
            // Remove client.
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
    
        // Command for deleting the player.
        if ("DELETE".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Client deleting: " + senderUsername);
            Message deleteMessage = new Message("DELETE", msg.getParameters(), "RESPONSE");
            broadcastMessageToAll(deleteMessage);
            String targetPlayerName = msg.getParameters()[0].toString();
    
            // Remove the player from the game.
            for (GameObject go : myGameInstance.getGameObjects()) {
                if (go.getName().equals(targetPlayerName)) {
                    myGameInstance.getGameObjects().remove(go);
                    break;
                }
            }
            // For debugging.
            for (GameObject go : myGameInstance.getGameObjects()) {
                System.out.println(go.getName());
            }
        }
    
        // Handles Exit-request to close the client.
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
    
    public static void main(String[] args) {
        new Server().start();
    }
}
