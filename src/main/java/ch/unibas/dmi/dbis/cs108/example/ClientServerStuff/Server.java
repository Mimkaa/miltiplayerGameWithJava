package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int SERVER_PORT = 9876;
    // Fixed thread pool to process incoming packets.
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    // Thread-safe list to keep track of client addresses.
    private static final CopyOnWriteArrayList<InetSocketAddress> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        // Initialize server-side player state (used only for logging purposes).
        Player[] players = new Player[3];
        players[0] = new Player("Alice", 100.0f, 200.0f, 10.0f);
        players[1] = new Player("Bob", 150.0f, 250.0f, 12.0f);
        players[2] = new Player("Carol", 200.0f, 300.0f, 15.0f);

        System.out.println("Initial players:");
        for (Player p : players) {
            System.out.println(p);
        }

        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("Server is running on port " + SERVER_PORT);

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
                }
                System.out.println("New client connected: " + senderSocket + ". Total clients: " + clients.size());

                // Process the packet asynchronously.
                executor.submit(() -> {
                    String clientMessage = new String(data);
                    System.out.println("Received from " + clientAddress + ":" + clientPort + " - " + clientMessage);
                    try {
                        // Decode the message using the new message system.
                        Message msg = MessageCodec.decode(clientMessage);
                        // Check if it's a movement message.
                        if ("MOVE".equals(msg.getMessageType())) {
                            // Simply forward the message to all clients except the sender.
                            String broadcastMessage = MessageCodec.encode(msg);
                            for (InetSocketAddress client : clients) {
                                if (!client.equals(senderSocket)) {
                                    byte[] sendData = broadcastMessage.getBytes();
                                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                                            client.getAddress(), client.getPort());
                                    try {
                                        serverSocket.send(sendPacket);
                                        System.out.println("Forwarded message to " + client);
                                    } catch (IOException e) {
                                        System.err.println("Error sending broadcast: " + e.getMessage());
                                    }
                                }
                            }
                        } else {
                            // For non-movement messages, echo back to the sender.
                            String response = "Echo: " + clientMessage;
                            byte[] sendData = response.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                            serverSocket.send(sendPacket);
                        }
                    } catch (IllegalArgumentException e) {
                        String errorResponse = "Error: " + e.getMessage();
                        byte[] sendData = errorResponse.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                        try {
                            serverSocket.send(sendPacket);
                        } catch (IOException ex) {
                            System.err.println("Error sending error response: " + ex.getMessage());
                        }
                    } catch (IOException e) {
                        System.err.println("I/O error: " + e.getMessage());
                    }
                });
            }
        } catch (SocketException e) {
            System.err.println("Socket error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}
