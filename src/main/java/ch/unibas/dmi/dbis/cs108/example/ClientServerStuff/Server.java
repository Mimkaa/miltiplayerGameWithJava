package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import chat.ChatManager;
import lombok.Getter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class Server {
    // Private constructor to prevent external instantiation.
    private Server() { }
    public static final int SERVER_PORT = 9876;

    ChatManager.ServerChatManager serverChatManager;

    // IMPORTANT FOR GETTING ONLY O N E SERVER INSTANCE!!!!!!!
    // Static inner helper class that holds the singleton instance.
    private static class SingletonHelper {
        private static final Server INSTANCE = new Server();
    }
    public static Server getInstance() {
        return SingletonHelper.INSTANCE;
    }
    
    // Concurrent map to track clients: key = username, value = InetSocketAddress.
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
    
    // Server socket.
    private DatagramSocket serverSocket;
    
    // Reliable sender and ACK processor.
    private ReliableUDPSender reliableSender;
    private AckProcessor ackProcessor;

    // The Game instance.
    private Game MY_GAME_INSTANCE;
    
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
            
            MY_GAME_INSTANCE = new Game("GameSession1");
            MY_GAME_INSTANCE.startPlayersCommandProcessingLoop();
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
                        
                        AsyncManager.run(() -> {
                            Message msg = MessageCodec.decode(messageString);
                            processMessage(msg, senderSocket);
                        });
                        
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
     * Processes a received message.
     * If the message is not an ACK, extracts the username from the last concealed parameter,
     * registers the client in clientsMap, and then:
     * - If the message option is "GAME", calls processMessageBestEffort(...) asynchronously,
     *   which sends the message via best-effort UDP to all clients except the sender.
     * - Otherwise, processes the message as before.
     *
     * @param msg          The decoded message.
     * @param senderSocket The sender's socket address.
     */
    private void processMessage(Message msg, InetSocketAddress senderSocket) {
        switch (msg.getMessageType().toUpperCase()) {

            case "ACK" -> {

                String ackUuid = msg.getParameters()[0].toString();
                reliableSender.acknowledge(ackUuid);
                System.out.println("Processed ACK message for UUID " + msg.getUUID());
            }

            case "CHAT" -> {
                System.out.println("Processed CHAT message 1 ");
                if (serverChatManager == null){
                    serverChatManager = new ChatManager.ServerChatManager();
                }
                serverChatManager.handleChatMessage(msg, senderSocket, ackProcessor);

            }

            default -> {
                System.out.println("Unknown message type: " + msg.getMessageType());
            }
        }



        // 2) Otherwise, handle REQUEST or broadcast.
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length >= 2) {
            // Username from the last element.
            String username = concealed[concealed.length - 1];
            clientsMap.put(username, senderSocket);
            System.out.println("Registered user: " + username + " at " + senderSocket
                    + ". Total clients: " + clientsMap.size());
    
            System.out.println("Current clientsMap after registering \"" + username + "\":");
            clientsMap.forEach((user, address) -> System.out.println("  " + user + " -> " + address));
    
            // If the message has a UUID, register it in the ACK processor.
            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }
    
            // 3) Distinguish based on message option.
            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                // For game messages, send best effort.
                MY_GAME_INSTANCE.addIncomingMessage(msg);
                AsyncManager.run(() -> processMessageBestEffort(msg, senderSocket));
            } else if ("REQUEST".equalsIgnoreCase(msg.getOption())) {
                AsyncManager.run(() -> handleRequest(msg, username));
            } else {
                AsyncManager.run(() -> broadcastMessageToOthers(msg, username));
            }
        } else {
            System.out.println("Concealed parameters missing or too short.");
        }
    }
    
    /**
     * Processes a GAME-type message using best-effort UDP send.
     * This method sends the message to all clients except the sender.
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
            Future<GameObject> futureObj = MY_GAME_INSTANCE.addGameObjectAsync(
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
            List<GameObject> gameObjectList = MY_GAME_INSTANCE.getGameObjects();
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
            List<GameObject> gameObjectList = MY_GAME_INSTANCE.getGameObjects();
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

        //handles Logout-request
        if ("LOGOUT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+",""))) {
            //prints in the server, that a client is logging out
            System.out.println("Client logging out:" + senderUsername);

            //Broadcast the new RESPONSE to all clients
            Message logoutmessage = new Message ("LOGOUT", msg.getParameters(), "RESPONSE");

            logoutmessage.setUUID("");
            broadcastMessageToAll(logoutmessage);

            //remove clients
            System.out.println("Removed user: " + senderUsername);
            clientsMap.remove(senderUsername);
        }
        if ("LOGIN".equalsIgnoreCase(msg.getMessageType())) {
            //print Client loggin in
            System.out.println("Client logging in:" + senderUsername);
            //takes the first parameter of the received message
            String firstParam = msg.getParameters()[0].toString();
            //create a new array of size one
            Object[] newParams = new Object[1];
            //gets all the characters
            GameObject[] gameObjects = MY_GAME_INSTANCE.getGameObjects().toArray(new GameObject[0]);
            //loops through all the characters and compares the first parameter of the received message with the character's name
            for (GameObject gameObject : gameObjects) {
                if (firstParam.equals(gameObject.getName())) {
                    newParams[0] = gameObject.getId();
                    break;//take the USERID and put it in the parameter of the new message
                }
            }
            //send message to the client
            Message Loginmessage = new Message("LOGIN", newParams, "RESPONSE");
            Loginmessage.setUUID("");

            InetSocketAddress clientAdress = clientsMap.get(senderUsername);
            enqueueMessage(Loginmessage, clientAdress.getAddress(), clientAdress.getPort());

        }

    }
    
    public static void main(String[] args) {
        new Server().start();
    }
}
