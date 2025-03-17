package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.UUID;

public class Server {
    public static final int SERVER_PORT = 9876;
    
    // Concurrent map to track clients: key = username, value = InetSocketAddress.
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
    
    // Server socket.
    private DatagramSocket serverSocket;
    
    // Reliable sender and ACK processor.
    private ReliableUDPSender reliableSender;
    private AckProcessor ackProcessor;

    // The Game instance.
    private  Game MyGameInstance;
    
    /**
     * Starts the server by creating a thread that continuously listens for incoming messages.
     */
    public void start() {
        try {
            // Bind server socket to localhost:SERVER_PORT.
            InetAddress ipAddress = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);
            
            // Initialize ReliableUDPSender and ACK processor.
            reliableSender = new ReliableUDPSender(serverSocket, 10, 200);
            ackProcessor = new AckProcessor(serverSocket);
            ackProcessor.start();
            
            MyGameInstance = new Game("GameSession1");
            // Start a thread that continuously listens for UDP messages.
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
     * If the message is not an ACK, extracts the username from the second-to-last concealed parameter
     * and registers the client in clientsMap.
     *
     * @param msg           The decoded message.
     * @param senderSocket  The sender's socket address.
     */
    private void processMessage(Message msg, InetSocketAddress senderSocket) {
        // 1) Check if it's an ACK
        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
            // Process ACK
            String ackUuid = msg.getParameters()[0].toString();
            reliableSender.acknowledge(ackUuid);
            System.out.println("Processed ACK message for UUID " + msg.getUUID());
            return;
        }
    
        // 2) Otherwise, handle REQUEST or broadcast
        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length >= 2) {
            // Username from the last element
            String username = concealed[concealed.length - 1];
            clientsMap.put(username, senderSocket);
            System.out.println("Registered user: " + username + " at " + senderSocket
                    + ". Total clients: " + clientsMap.size());
    
            // Print the entire map
            System.out.println("Current clientsMap after registering \"" + username + "\":");
            clientsMap.forEach((user, address) -> {
                System.out.println("  " + user + " -> " + address);
            });
    
            // If the message has a UUID, register it in the ACK processor
            if (msg.getUUID() != null) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }
    
            // 3) Distinguish between REQUEST and all other types
            if ("REQUEST".equals(msg.getOption())) {
                // If it's a REQUEST, run a separate handler for that
                AsyncManager.run(() -> handleRequest(msg, username));
            } else {
                // Otherwise, broadcast the message asynchronously
                AsyncManager.run(() -> broadcastMessageToOthers(msg, username));
            }
        } else {
            System.out.println("Concealed parameters missing or too short.");
        }
    }
    
    
    /**
 * Broadcasts a message to all known clients in clientsMap.
    */
    private void broadcastMessageToAll(Message msg) {
        for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            
            try {
                reliableSender.sendMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                System.out.println("Broadcast to " + clientUsername + " at " + clientAddress);
            } catch (Exception e) {
                System.err.println("Error sending message to " + clientUsername + " at " 
                                + clientAddress + ": " + e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a message to all clients except the one identified by `excludedUsername`.
     */
    private void broadcastMessageToOthers(Message msg, String excludedUsername) {
        for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            
            // Send to every client whose username is not `excludedUsername`.
            if (!clientUsername.equals(excludedUsername)) {
                try {
                    reliableSender.sendMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                    System.out.println("Forwarded message to " + clientUsername + " at " + clientAddress);
                } catch (Exception e) {
                    System.err.println("Error sending message to " + clientUsername + " at " 
                                    + clientAddress + ": " + e.getMessage());
                }
            }
        }
    }


    

    private void handleRequest(Message msg, String senderUsername) {
        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            // Example incoming message: CREATE{REQUEST}[Player, Alice, 100, 150, 10, GameSession1]||
            // where "Player" is the type and "Alice" is the desired object name.
            
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
            
            // 4) Create a new message with:
            //    - messageType = "CREATE"
            //    - option      = "RESPONSE"
            //    - parameters  = newParams
            Message responseMsg = new Message("CREATE", newParams, "RESPONSE");
            
            // 5) Set responseMsg UUID to an empty string so the encoder won't append "null".
            responseMsg.setUUID("");
            
            // 6) Update the game on the server by adding the new game object.
            // The factory expects the following signature:
            //   addGameObjectAsync(String type, String uuid, Object... params)
            // where:
            //   type is originalParams[0] (e.g. "Player"),
            //   uuid is the serverGeneratedUuid,
            //   and the remaining parameters (starting from index 1) are passed as varargs.
            Future<GameObject> futureObj = MyGameInstance.addGameObjectAsync(
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
            
            // 7) Broadcast the new RESPONSE message to all clients.
            broadcastMessageToAll(responseMsg);
        }
    }

    

    
    
    
    public static void main(String[] args) {
        new Server().start();
    }
}
