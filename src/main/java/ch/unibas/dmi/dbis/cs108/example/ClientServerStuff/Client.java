package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import java.util.UUID;

public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 9876;

    // Global queue for outgoing messages.
    private final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    // Global queue for incoming messages.
    private final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    // The Game instance.
    private final Game game;

    // Scanner for the terminal.
    private final Scanner scanner = new Scanner(System.in);

    // The client's username.
    private String username = UUID.randomUUID().toString();

    // Fields to store additional client state.
    private String idGameObject;
    private String idGame;

    // Reliable UDP Sender.
    private ReliableUDPSender myReliableUDPSender;
    // AckProcessor for sending ACK messages.
    private AckProcessor ackProcessor;

    // Client socket as a class attribute.
    private DatagramSocket clientSocket;

    // Constructor creates the Game object.
    public Client(String gameSessionName) {
        this.game = new Game(gameSessionName);
    }

    /**
     * Starts the graphics-related tasks.
     */
    private void startGraphicsStuff(String clientName) {
        SwingUtilities.invokeLater(() -> game.initUI(clientName));

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
        try {
            // Initialize the client socket once.
            clientSocket = new DatagramSocket();
            
            // Initialize the reliable sender without a fixed destination.
            myReliableUDPSender = new ReliableUDPSender(clientSocket, 10, 200);
            
            // Initialize the AckProcessor using the same socket.
            ackProcessor = new AckProcessor(clientSocket);
            ackProcessor.start();

            Message mockMessage = new Message("MOCK", new Object[] { "Hello from " + username }, null);
            String[] concealedPrms = { "something1", "something2", username };
            mockMessage.setConcealedParameters(concealedPrms);
            // For demonstration, send the mock message to SERVER_ADDRESS:SERVER_PORT
            InetAddress serverInet = InetAddress.getByName(SERVER_ADDRESS);
            myReliableUDPSender.sendMessage(mockMessage, serverInet, SERVER_PORT);
            
            
            
            // Receiver Task: Continuously listen for UDP packets and enqueue decoded messages.
            AsyncManager.runLoop(() -> {
                try {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Received (UDP): " + response);
                    Message receivedMessage = MessageCodec.decode(response);
                    incomingQueue.offer(receivedMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            // Consumer Task: Process incoming messages.
            AsyncManager.runLoop(() -> {
                try {
                    Message msg = incomingQueue.poll();
                    if (msg != null) {
                        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
                            // Process the ACK
                            if (msg.getParameters() != null && msg.getParameters().length > 0) {
                                String ackUuid = msg.getParameters()[0].toString();
                                myReliableUDPSender.acknowledge(ackUuid);
                            } else {
                                System.out.println("Received ACK with no parameters.");
                            }
                        } else {
                            // Non-ACK message logic
                            if (msg.getUUID() != null) {
                                InetSocketAddress dest = 
                                    new InetSocketAddress(InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                                ackProcessor.addAck(dest, msg.getUUID());
                            }
            
                            // Check the "option" field to see what kind of message it is
                            String option = msg.getOption();
                            
                            if ("GAME".equalsIgnoreCase(option)) {
                                // If it's a GAME update, forward it to your game logic/UI
                                game.addIncomingMessage(msg);
                            } else if ("RESPONSE".equalsIgnoreCase(option)) {
                                // If it's some sort of server "RESPONSE", call a dedicated handler
                                AsyncManager.run(() -> {processServerResponse(msg);});
                            } else {
                                // For anything else, do whatever fallback logic you want
                                System.out.println("Unknown message option: " + option);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            // Sender Task: Continuously poll outgoingQueue and send messages using the reliable sender.
            AsyncManager.runLoop(() -> {
                Message msg = outgoingQueue.poll();
                if (msg != null) {
                    try {
                        // Retrieve the existing concealed parameters.
                        String[] concealed = msg.getConcealedParameters();
                        if (concealed == null) {
                            // If none exist, create a new array with one element.
                            concealed = new String[] { username };
                        } else {
                            // Otherwise, create a new array with one extra slot.
                            String[] newConcealed = new String[concealed.length + 1];
                            System.arraycopy(concealed, 0, newConcealed, 0, concealed.length);
                            // Insert the client's UUID as the last concealed parameter.
                            newConcealed[newConcealed.length - 1] = username;
                            concealed = newConcealed;
                        }
                        // Set the modified concealed parameters back on the message.
                        msg.setConcealedParameters(concealed);
            
                        // For demonstration, send the message to the server.
                        InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);
                        myReliableUDPSender.sendMessage(msg, dest, SERVER_PORT);
                    } catch (Exception e) {
                        e.printStackTrace();
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
        outgoingQueue.offer(msg);
    }
    
    /**
     * Processes client update messages (if needed).
     */
    private void processServerResponse(Message msg) {
        System.out.println("Handling RESPONSE message: " + msg);
        
        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            // Expecting parameters:
            // [serverGeneratedUuid, objectType, objectName, posX, posY, size, gameSession]
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 7) {
                String serverUuid = params[0].toString();
                String objectType = params[1].toString();
                
                // Pass the remaining parameters (from index 2 to the end) as varargs.
                Object[] remainingParams = java.util.Arrays.copyOfRange(params, 2, params.length);
                Future<GameObject> futureObj = game.addGameObjectAsync(objectType, serverUuid, remainingParams);
                
                try {
                    GameObject newObj = futureObj.get();
                    System.out.println("Created new game object with UUID: " + serverUuid 
                            + " and name: " + newObj.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("CREATE RESPONSE message does not contain enough parameters.");
            }
        } else {
            System.out.println("Unhandled RESPONSE message type: " + msg.getMessageType());
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
                String[] concealedParams = { "something1", "something2" };
                msg.setConcealedParameters(concealedParams);
                sendMessage(msg);
            } catch (Exception e) {
                System.out.println("Invalid message format: " + command);
            }
        });
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public static void main(String[] args) {
        try {

            Scanner inputScanner = new Scanner(System.in);
            System.out.print("Enter your name: ");
            String userName = inputScanner.nextLine();

            // Choose a random name from the list.
            String[] names = {"Alice", "Bob", "Carol"};
            String randomName = names[new java.util.Random().nextInt(names.length)];
            System.out.println("Selected name: " + randomName);
            
            Client client = new Client("GameSession1");
            
            client.setUsername(userName);
            System.out.println("Set client username: " + userName);
            // Start the graphical interface using the randomly selected name.
            client.startGraphicsStuff(randomName);
            client.startConsoleReaderLoop();
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
