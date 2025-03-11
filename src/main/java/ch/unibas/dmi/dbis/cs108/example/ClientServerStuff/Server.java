package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int SERVER_PORT = 9876;
    public static final int ADMIN_TCP_PORT = 9877; // Dedicated port for admin commands
    
    // Executor for processing UDP packets.
    private final ExecutorService udpExecutor = Executors.newFixedThreadPool(10);
    // Executor for processing TCP admin connections.
    private final ExecutorService adminExecutor = Executors.newFixedThreadPool(5);
    
    // Thread-safe list to track UDP client addresses.
    private final CopyOnWriteArrayList<InetSocketAddress> clients = new CopyOnWriteArrayList<>();
    
    private DatagramSocket serverSocket;
    
    /**
     * Starts the server by launching two listener threads:
     * one for UDP game messages and one for TCP administrative commands.
     */
    public void start() {
        // Start the UDP listener.
        new Thread(this::startUDPListener).start();
        // Start the TCP admin listener.
        new Thread(this::startTCPAdminListener).start();
    }
    
    /**
     * UDP listener method.
     * Listens on SERVER_PORT for game messages, records client addresses,
     * and forwards incoming messages to all clients (except the sender).
     */
    private void startUDPListener() {
        try {
            InetAddress ipAddress = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);

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
                udpExecutor.submit(() -> processPacket(data, senderSocket));
            }
        } catch (SocketException ex) {
            System.err.println("Socket error (UDP): " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IO error (UDP): " + ex.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            udpExecutor.shutdown();
        }
    }
    
    /**
     * Processes an individual UDP packet by decoding the message,
     * then broadcasting it to all connected clients except the sender.
     *
     * @param data The received data.
     * @param senderSocket The sender's socket address.
     */
    private void processPacket(byte[] data, InetSocketAddress senderSocket) {
        String clientMessage = new String(data);
        System.out.println("Received from " + senderSocket.getAddress().getHostAddress() + ":" 
                           + senderSocket.getPort() + " - " + clientMessage);
        try {
            // Decode the message using your MessageCodec.
            Message msg = MessageCodec.decode(clientMessage);
            // Encode the message for broadcast.
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
     * TCP listener method for administrative commands.
     * Listens on ADMIN_TCP_PORT, accepts connections, and delegates each to a thread from the adminExecutor.
     */
    private void startTCPAdminListener() {
        try (ServerSocket adminServerSocket = new ServerSocket(ADMIN_TCP_PORT)) {
            System.out.println("TCP Admin Server is running on port " + ADMIN_TCP_PORT);
            while (true) {
                Socket clientSocket = adminServerSocket.accept();
                // Process each admin connection concurrently.
                adminExecutor.submit(() -> handleAdminConnection(clientSocket));
            }
        } catch (IOException ex) {
            System.err.println("Error in TCP Admin Listener: " + ex.getMessage());
        }
    }
    
    /**
     * Handles an individual TCP admin connection.
     * Reads a newline-delimited command, processes it (using if/switch logic),
     * sends the response, and then closes the connection.
     *
     * @param clientSocket The TCP socket for the admin connection.
     */
    private void handleAdminConnection(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
             
             // Read the admin command.
             String adminMessage = reader.readLine();
             System.out.println("Received admin message: " + adminMessage);
             
             // Decode the message using MessageCodec.
             Message msg = MessageCodec.decode(adminMessage);
             String response;
             
             // Process the command.
             if ("LIST_GAMES".equalsIgnoreCase(msg.getMessageType())) {
                 response = getListOfGames();
             } else {
                 response = "Unknown command";
             }
             
             // Send the response.
             writer.write(response);
             writer.newLine();
             writer.flush();
        } catch (IOException ex) {
            System.err.println("Error handling admin connection: " + ex.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.err.println("Error closing admin connection: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Example helper method to simulate retrieving a list of games.
     *
     * @return A comma-separated string of game names.
     */
    private String getListOfGames() {
        // In a real scenario, this method would collect game session data.
        return "Game1, Game2, Game3";
    }
    
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
