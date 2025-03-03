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
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    // Thread-safe list to keep track of client addresses.
    private static final CopyOnWriteArrayList<InetSocketAddress> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("No server-side players created.");

        try {
            // Bind the server to a specific IP address.
            InetAddress ipAddress = InetAddress.getByName("25.12.99.19");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            
            try (DatagramSocket serverSocket = new DatagramSocket(socketAddress)) {
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
                    }
                    System.out.println("New client connected: " + senderSocket + ". Total clients: " + clients.size());

                    // Process the packet asynchronously.
                    executor.submit(() -> {
                        String clientMessage = new String(data);
                        System.out.println("Received from " + clientAddress + ":" + clientPort + " - " + clientMessage);
                        try {
                            // Decode the message.
                            Message msg = MessageCodec.decode(clientMessage);
                            // Forward the message to all clients except the sender.
                            String broadcastMessage = MessageCodec.encode(msg);
                            for (InetSocketAddress client : clients) {
                                if (!client.equals(senderSocket)) {
                                    byte[] sendData = broadcastMessage.getBytes();
                                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                                            client.getAddress(), client.getPort());
                                    try {
                                        serverSocket.send(sendPacket);
                                        System.out.println("Forwarded message to " + client);
                                    } catch (Exception ex1) {
                                        System.err.println("Error sending broadcast: " + ex1.getMessage());
                                    }
                                }
                            }
                        } catch (IllegalArgumentException ex) {
                            String errorResponse = "Error: " + ex.getMessage();
                            byte[] sendData = errorResponse.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                            try {
                                serverSocket.send(sendPacket);
                            } catch (Exception ex2) {
                                System.err.println("Error sending error response: " + ex2.getMessage());
                            }
                        } catch (Exception ex) {
                            System.err.println("Error processing packet: " + ex.getMessage());
                        }
                    });
                }
            }
        } catch (SocketException ex) {
            System.err.println("Socket error: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}
