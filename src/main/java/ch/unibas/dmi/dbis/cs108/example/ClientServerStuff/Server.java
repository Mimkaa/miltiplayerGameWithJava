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
        // Create an array of three players (server-side state).
        Player[] players = new Player[3];
        players[0] = new Player("Alice", 100.0f, 200.0f, 10.0f);
        players[1] = new Player("Bob", 150.0f, 250.0f, 12.0f);
        players[2] = new Player("Carol", 200.0f, 300.0f, 15.0f);
        
        // Print the players to verify initialization.
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

                // Copy received data for safe processing.
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                InetSocketAddress senderSocket = new InetSocketAddress(clientAddress, clientPort);

                // Add sender to clients list if not already present.
                if (!clients.contains(senderSocket)) {
                    clients.add(senderSocket);
                }

                // Submit task to the thread pool.
                executor.submit(() -> {
                    String clientMessage = new String(data);
                    System.out.println("Received from " + clientAddress + ":" + clientPort + " - " + clientMessage);
                    
                    // Check if it's a movement message.
                    if (clientMessage.startsWith(MovementCodec.MOVE_TYPE)) {
                        try {
                            // Decode the movement message.
                            MovementData movement = MovementCodec.decodeMovement(clientMessage);
                            System.out.println("Decoded movement: " + movement);
                            
                            // Update the corresponding player's position in the server's state.
                            Player updatedPlayer = null;
                            for (Player p : players) {
                                if (p.getName().equals(movement.getPlayerName())) {
                                    // Here, the movement message contains relative offsets.
                                    p.setX(p.getX() + movement.getXoffset());
                                    p.setY(p.getY() + movement.getYoffset());
                                    updatedPlayer = p;
                                    System.out.println("Updated " + p.getName() + " to new absolute position: x=" + p.getX() + ", y=" + p.getY());
                                    break;
                                }
                            }
                            
                            if (updatedPlayer != null) {
                                // Build a broadcast message that contains the updated absolute state.
                                String broadcastMessage = MovementCodec.encodeMovement(
                                        updatedPlayer.getName(), updatedPlayer.getX(), updatedPlayer.getY());
                                
                                // Broadcast to all clients except the sender.
                                for (InetSocketAddress client : clients) {
                                    if (!client.equals(senderSocket)) {
                                        byte[] sendData = broadcastMessage.getBytes();
                                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                                                client.getAddress(), client.getPort());
                                        try {
                                            serverSocket.send(sendPacket);
                                        } catch (IOException e) {
                                            System.err.println("Error sending broadcast: " + e.getMessage());
                                        }
                                    }
                                }
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
                        }
                    } else {
                        // For non-movement messages, simply echo back to sender.
                        String response = "Echo: " + clientMessage;
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                        try {
                            serverSocket.send(sendPacket);
                        } catch (IOException e) {
                            System.err.println("Error sending echo: " + e.getMessage());
                        }
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
