package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final int SERVER_PORT = 9876;
    
    // Concurrent map to track clients: key = username, value = InetSocketAddress.
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
    
    // Server socket.
    private DatagramSocket serverSocket;
    
    // Reliable sender and ACK processor.
    private ReliableUDPSender reliableSender;
    private AckProcessor ackProcessor;
    
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
        if (!"ACK".equalsIgnoreCase(msg.getMessageType())) {
            // Extract the concealed parameters.
            String[] concealed = msg.getConcealedParameters();
            if (concealed != null && concealed.length >= 2) {
                // Username is assumed to be the last item (post-decode),
                // so we retrieve it from concealed[concealed.length - 1].
                String username = concealed[concealed.length - 1];
                clientsMap.put(username, senderSocket);
                System.out.println("Registered user: " + username + " at " + senderSocket + 
                                   ". Total clients: " + clientsMap.size());
    
                // Print the entire map to see all registered clients.
                System.out.println("Current clientsMap after registering \"" + username + "\":");
                clientsMap.forEach((user, address) -> {
                    System.out.println("  " + user + " -> " + address);
                });
    
                // If the message has a UUID, add it to the AckProcessor.
                if (msg.getUUID() != null) {
                    ackProcessor.addAck(senderSocket, msg.getUUID());
                    System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
                }
                // Broadcast the message asynchronously.
                AsyncManager.run(() -> broadcastMessage(msg, username));
            } else {
                System.out.println("Concealed parameters missing or too short.");
            }
        } else {
            // Process ACK messages by calling reliableSender.acknowledge() with the message's UUID.
            String ackUuid = msg.getParameters()[0].toString();
            reliableSender.acknowledge(ackUuid);
            System.out.println("Processed ACK message for UUID " + msg.getUUID());
        }
    }
    
    
    /**
     * Broadcasts the given message to all clients except the sender (identified by senderUsername).
     */
    private void broadcastMessage(Message msg, String senderUsername) {
        
        for (ConcurrentHashMap.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            // Send to every client whose username is not the sender's.
            if (!clientUsername.equals(senderUsername)) {
                try {
                    reliableSender.sendMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                    System.out.println("Forwarded message to UDP client " + clientUsername + " at " + clientAddress);
                } catch (Exception e) {
                    System.err.println("Error sending message to " + clientUsername + " at " + clientAddress + ": " + e.getMessage());
                }
            }
        }
    }
    
    
    
    public static void main(String[] args) {
        new Server().start();
    }
}
