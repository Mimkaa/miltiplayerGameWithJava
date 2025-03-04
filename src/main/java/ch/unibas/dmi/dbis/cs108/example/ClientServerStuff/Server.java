package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int SERVER_PORT = 9876;
    
    // Fixed thread pool to process incoming packets.
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    // Thread-safe list to keep track of client addresses.
    private final CopyOnWriteArrayList<InetSocketAddress> clients = new CopyOnWriteArrayList<>();
    private DatagramSocket serverSocket;
    
    /**
     * Starts the server which listens for UDP packets,
     * records new clients, and forwards messages.
     */
    public void start() {
        try {
            // Bind the server to a specific IP address (localhost in this example).
            InetAddress ipAddress = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(packet);

                // Safely copy the received data.
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                InetSocketAddress senderSocket = new InetSocketAddress(clientAddress, clientPort);

                // Add sender to the client list if not already present.
                if (!clients.contains(senderSocket)) {
                    clients.add(senderSocket);
                    System.out.println("New client connected: " + senderSocket + ". Total clients: " + clients.size());
                }

                // Process the packet asynchronously.
                executor.submit(() -> processPacket(data, senderSocket));
            }
        } catch (SocketException ex) {
            System.err.println("Socket error: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdown();
        }
    }
    
    /**
     * Processes an individual packet by decoding the message,
     * then broadcasting it to all other clients.
     *
     * @param data The received data.
     * @param senderSocket The sender's socket address.
     */
    private void processPacket(byte[] data, InetSocketAddress senderSocket) {
        String clientMessage = new String(data);
        System.out.println("Received from " + senderSocket.getAddress().getHostAddress() + ":" + senderSocket.getPort() +
                           " - " + clientMessage);
        try {
            // Decode the message using your MessageCodec.
            Message msg = MessageCodec.decode(clientMessage);
            // Encode the message back for broadcast.
            String broadcastMessage = MessageCodec.encode(msg);
            byte[] sendData = broadcastMessage.getBytes();

            // Forward the message to all clients except the sender.
            for (InetSocketAddress client : clients) {
                if (!client.equals(senderSocket)) {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                            client.getAddress(), client.getPort());
                    try {
                        serverSocket.send(sendPacket);
                        System.out.println("Forwarded message to " + client);
                    } catch (Exception ex1) {
                        System.err.println("Error sending broadcast to " + client + ": " + ex1.getMessage());
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            // If decoding fails, send back an error message.
            String errorResponse = "Error: " + ex.getMessage();
            byte[] sendData = errorResponse.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                    senderSocket.getAddress(), senderSocket.getPort());
            try {
                serverSocket.send(sendPacket);
            } catch (Exception ex2) {
                System.err.println("Error sending error response: " + ex2.getMessage());
            }
        } catch (Exception ex) {
            System.err.println("Error processing packet: " + ex.getMessage());
        }
    }
    
    /**
     * Main method to run the Server.
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
