package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 9876;
    
    // Global queue for outgoing messages.
    private static final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    // Global queue for incoming messages.
    private static final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        // Create an array of three players on the client side.
        Player[] players = new Player[3];
        players[0] = new Player("Alice", 100.0f, 200.0f, 10.0f);
        players[1] = new Player("Bob", 150.0f, 250.0f, 12.0f);
        players[2] = new Player("Carol", 200.0f, 300.0f, 15.0f);
        
        System.out.println("Initial players on client:");
        for (Player p : players) {
            System.out.println(p);
        }
        
        // Prompt the user to enter their name.
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String clientName = scanner.nextLine();
        
        // Create and show the GUI on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simple Game");
            GamePanel gamePanel = new GamePanel(players);
            frame.add(gamePanel);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Connect the controls for the player that matches the client name.
            for (Player p : players) {
                if (p.getName().equals(clientName)) {
                    frame.addKeyListener(p.getKeyListener());
                    System.out.println("Connected controls for " + p.getName());
                    break;
                }
            }
            frame.setVisible(true);
            
            // Repaint the game panel periodically.
            Timer timer = new Timer(100, ev -> gamePanel.repaint());
            timer.start();
        });
        
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverIP = InetAddress.getByName(SERVER_ADDRESS);
            
            // Receiver Thread:
            // Listen for UDP packets, decode them into Message objects,
            // and place them into the global incoming queue.
            Thread receiverThread = new Thread(() -> {
                while (true) {
                    try {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        clientSocket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("Received: " + response);
                        
                        // Decode the response into a Message object.
                        Message receivedMessage = MessageCodec.decode(response);
                        incomingQueue.offer(receivedMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            receiverThread.start();
            
            // Consumer Thread:
            // Continuously poll the global incoming queue and route messages to
            // the appropriate player's internal queue based on the sender's name.
            Thread consumerThread = new Thread(() -> {
                while (true) {
                    Message msg = incomingQueue.poll();
                    if (msg != null) {
                        // Extract sender's name from the concealed parameters.
                        String[] concealed = msg.getConcealedParameters();
                        if (concealed != null && concealed.length > 0) {
                            String senderName = concealed[0];
                            // Route the message to the matching player.
                            for (Player p : players) {
                                if (p.getName().equals(senderName)) {
                                    p.addIncomingMessage(msg);
                                    System.out.println("Consumer routed message to " + p.getName());
                                    break;
                                }
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            consumerThread.start();
            
            // Sender Thread:
            // Continuously poll the global outgoing queue and send any messages over UDP.
            Thread senderThread = new Thread(() -> {
                while (true) {
                    Message msg = outgoingQueue.poll();
                    if (msg != null) {
                        String msgStr = MessageCodec.encode(msg);
                        byte[] sendData = msgStr.getBytes();
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, SERVER_PORT);
                            clientSocket.send(sendPacket);
                            System.out.println("Sent: " + msgStr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            senderThread.start();
            
            // Main Loop:
            // Update every player's movement and enqueue movement messages into the outgoing queue.
            while (true) {
                // Update all players.
                for (Player p : players) {
                    p.update(outgoingQueue);
                    //System.out.println("Player " + p.getName() + " position: x=" + p.getX() + ", y=" + p.getY());
                }
                
                // Process incoming messages for all players.
                for (Player p : players) {
                    p.updateMessages();
                }
                
                Thread.sleep(1); // Sleep briefly for responsiveness.
            }
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
