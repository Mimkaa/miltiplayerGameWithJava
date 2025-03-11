package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 9876;
    public static final int ADMIN_TCP_PORT = 9877; // Dedicated port for admin messages

    // Global queue for outgoing messages.
    private final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    // Global queue for incoming messages.
    private final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    // Make the Game an attribute of the Client.
    private final Game game;

    // Scanner for the terminal.
    private Scanner scanner = new Scanner(System.in);

    // The client's username.
    private String username = "default";
    
    // New fields to store the local player's game object ID and the game session ID.
    private String idGameObject;
    private String idGame;

    // Constructor creates the Game object.
    public Client(String gameSessionName) {
        this.game = new Game(gameSessionName);
    }
    
    /**
     * Starts the graphics-related tasks:
     * - Initializes the UI.
     * - Displays the current players.
     * - Schedules the game updater at ~60 FPS.
     */
    private void startGraphicsStuff(String clientName) {
        // Initialize the UI on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            game.initUI(clientName);
        });

        // Schedule the Game Updater using AsyncManager.
        AsyncManager.run(() -> {
            try {
                while (true) {
                    game.updateActiveObject(clientName, outgoingQueue);
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void run() {
        // Optionally send a "mock" message to the server to register this client.
        Message mockMessage = new Message(
            "MOCK", 
            new Object[] {"Hello from New Client"}, 
            null, 
            new String[] {}
        );
        outgoingQueue.offer(mockMessage);

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverIP = InetAddress.getByName(SERVER_ADDRESS);

            // Receiver Task: Listen for UDP packets, decode them into Message objects,
            // and place them into the 'incomingQueue'.
            AsyncManager.runLoop(() -> {
                try {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Received: " + response);
                    Message receivedMessage = MessageCodec.decode(response);
                    incomingQueue.offer(receivedMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Consumer Task: Continuously poll 'incomingQueue' and push messages to the Game.
            AsyncManager.runLoop(() -> {
                Message msg = incomingQueue.poll();
                if (msg != null) {
                    String option = msg.getOption();  // e.g. "GAME" or "CLIENT"
                    if ("GAME".equalsIgnoreCase(option)) {
                        // Process game-related messages by handing them off to the game.
                        game.addIncomingMessage(msg);
                    } else if ("CLIENT".equalsIgnoreCase(option)) {
                        // Process client update messages using a client-side function.
                        processClientUpdateMessages(msg);
                    } else {
                        System.out.println("Unknown message option: " + option);
                    }
                }
            });
            
            // Sender Task: Continuously poll 'outgoingQueue' and send messages over UDP to the server.
            AsyncManager.runLoop(() -> {
                Message msg = outgoingQueue.poll();
                if (msg != null) {
                    if ("ADMIN".equalsIgnoreCase(msg.getOption())) {
                        // For administrative messages, use TCP on the dedicated admin port.
                        administrativeStuff(msg);
                    } else {
                        String msgStr = MessageCodec.encode(msg);
                        byte[] sendData = msgStr.getBytes();
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, SERVER_PORT);
                            clientSocket.send(sendPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // Block the main thread indefinitely.
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Message msg) {
        // Enqueue the message for sending.
        outgoingQueue.offer(msg);
    }

    /**
     * Processes client update messages.
     * For a START message, it creates the local player, stores its unique ID (idGameObject),
     * stores the game session ID (idGame), and starts the graphics.
     */
    private void processClientUpdateMessages(Message msg) {
        if ("CHCK".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("CHCK message received: " + msg.getMessageType());
        } else if ("START".equalsIgnoreCase(msg.getMessageType())) {
            if (msg.getParameters() != null && msg.getParameters().length > 0) {
                String playerName = msg.getParameters()[0].toString();
                // Create the player's game object and store its UUID.
                idGameObject = game.addGameObject("Player", playerName, 0.0f, 0.0f, 5.0f, game.getGameName());
                // Store the game session's identifier (using the game name in this example).
                idGame = game.getGameName();
                System.out.println("Stored idGameObject: " + idGameObject + ", idGame: " + idGame);
                startGraphicsStuff(playerName);
            } else {
                System.out.println("START message missing player name parameter.");
            }
        } else {
            System.out.println("Processing CLIENT update: " + msg.getMessageType());
        }
    }
    
    /**
     * Sends administrative messages via TCP on a dedicated port.
     * This method opens a TCP connection to the server's ADMIN_TCP_PORT,
     * sends the encoded message with a newline as a delimiter,
     * and optionally reads a response.
     */
    private void administrativeStuff(Message msg) {
        try (Socket tcpSocket = new Socket(SERVER_ADDRESS, ADMIN_TCP_PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()))) {
             
             String msgStr = MessageCodec.encode(msg);
             writer.write(msgStr);
             writer.newLine(); // Using newline as a message delimiter
             writer.flush();
             
             // Optionally, read a response from the server.
             String response = reader.readLine();
             if (response != null) {
                 System.out.println("Admin response: " + response);
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
       
    }

    public void startConsoleReaderLoop() {
        AsyncManager.runLoop(() -> {
            System.out.print("Command> ");
            String command = scanner.nextLine();
            if ("exit".equalsIgnoreCase(command)) {
                System.out.println("Exiting console reader...");
                Thread.currentThread().interrupt();
                return;
            }
            if (!command.contains("|")) {
                command = command + "||";
            }
            try {
                Message msg = MessageCodec.decode(command);
                sendMessage(msg);
            } catch (Exception e) {
                System.out.println("Invalid message format: " + command);
            }
        });
    }
    
    // Main method creates an instance of Client and runs it.
    public static void main(String[] args) {
        Client client = new Client("GameSession1");
        client.startConsoleReaderLoop();
        client.run();
    }
}
