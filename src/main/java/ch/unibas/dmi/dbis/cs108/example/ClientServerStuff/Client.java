package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 9876;
    
    // Shared variables for movement input.
    private static volatile float inputX = 0;
    private static volatile float inputY = 0;
    
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
            frame.setVisible(true);
            
            // Add key listener to capture AWSD controls.
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                            inputY = -1;
                            break;
                        case KeyEvent.VK_S:
                            inputY = 1;
                            break;
                        case KeyEvent.VK_A:
                            inputX = -1;
                            break;
                        case KeyEvent.VK_D:
                            inputX = 1;
                            break;
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                        case KeyEvent.VK_S:
                            inputY = 0;
                            break;
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_D:
                            inputX = 0;
                            break;
                    }
                }
            });
            
            // Repaint the game panel periodically.
            Timer timer = new Timer(100, ev -> gamePanel.repaint());
            timer.start();
        });
        
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverIP = InetAddress.getByName(SERVER_ADDRESS);
            
            // Start a dedicated receiver thread to handle server responses.
            Thread receiverThread = new Thread(() -> {
                while (true) {
                    try {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        clientSocket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("Received from server: " + response);
                        
                        // Process the response if it's a movement message.
                        try {
                            MovementData movement = MovementCodec.decodeMovement(response);
                            if (movement.getType().equals("MOVE")) {
                                String movingPlayer = movement.getPlayerName();
                                float newX = movement.getXoffset(); // Now treated as absolute x
                                float newY = movement.getYoffset(); // Now treated as absolute y
                                
                                // Update the corresponding player's position by setting absolute coordinates.
                                for (Player p : players) {
                                    if (p.getName().equals(movingPlayer)) {
                                        p.setX(newX);
                                        p.setY(newY);
                                        System.out.println("Updated " + p.getName() + " to absolute position: x=" + p.getX() + ", y=" + p.getY());
                                    }
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Received non-movement message: " + response);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break; // Exit loop on error.
                    }
                }
            });
            receiverThread.start();
            
            // Main loop: send movement messages to the server periodically.
            while (true) {
                // Use AWSD input to calculate relative offsets.
                float speed = 5.0f;
                float xoffset = inputX * speed;
                float yoffset = inputY * speed;
                
                // Update the local player's position immediately (local prediction).
                for (Player p : players) {
                    if (p.getName().equals(clientName)) {
                        p.setX(p.getX() + xoffset);
                        p.setY(p.getY() + yoffset);
                        System.out.println("Local player " + p.getName() + " updated to new position: x=" + p.getX() + ", y=" + p.getY());
                        break;
                    }
                }
                
                // Encode the movement message including the client's name.
                // Here, we send relative movement offsets.
                String message = MovementCodec.encodeMovement(clientName, xoffset, yoffset);
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, SERVER_PORT);
                clientSocket.send(sendPacket);
                System.out.println("Sent: " + message);
                
                Thread.sleep(10); // Smaller sleep for more responsiveness.
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
