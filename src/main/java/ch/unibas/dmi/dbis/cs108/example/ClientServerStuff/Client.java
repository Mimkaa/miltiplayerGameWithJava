package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
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
    private final AtomicReference<String> clientName = new AtomicReference<>("");

    // Reliable UDP Sender.
    private ReliableUDPSender myReliableUDPSender;
    // AckProcessor for sending ACK messages.
    private AckProcessor ackProcessor;

    // Client socket as a class attribute.
    private DatagramSocket clientSocket;
    
    // Instance of PingManager.
    private PingManager pingManager;

    // Constructor creates the Game object.
    public Client(String gameSessionName) {
        this.game = new Game(gameSessionName);
    }

    /**
     * Starts the graphics-related tasks.
     */
    public void startGraphicsStuff(String initialClientName) {
        clientName.set(initialClientName); // Set initial name

        SwingUtilities.invokeLater(() -> game.initUI(clientName.get()));

        AsyncManager.run(() -> {
            try {
                while (true) {
                    game.updateActiveObject(clientName.get(), outgoingQueue); // Always get the latest client name
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
            myReliableUDPSender = new ReliableUDPSender(clientSocket, 50, 1000);
            
            // Initialize the AckProcessor using the same socket.
            ackProcessor = new AckProcessor(clientSocket);
            ackProcessor.start();

            // Create and start PingManager (pings every 1000 ms)
            InetAddress serverInet = InetAddress.getByName(SERVER_ADDRESS);
            //pingManager = new PingManager(outgoingQueue, serverInet, SERVER_PORT, 300);
            //pingManager.start();

            Message mockMessage = new Message("MOCK", new Object[] { "Hello from " + username }, "REQUEST");
            String[] concealedPrms = { "something1", "something2", username };
            mockMessage.setConcealedParameters(concealedPrms);
            // For demonstration, send the mock message to SERVER_ADDRESS:SERVER_PORT
            myReliableUDPSender.sendMessage(mockMessage, serverInet, SERVER_PORT);

            game.startPlayersCommandProcessingLoop();

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
                                InetSocketAddress dest = new InetSocketAddress(InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                                ackProcessor.addAck(dest, msg.getUUID());
                            }
            
                            // Check the "option" field to see what kind of message it is
                            String option = msg.getOption();
                            
                            if ("GAME".equalsIgnoreCase(option)) {
                                // If it's a GAME update, forward it to your game logic/UI
                                game.addIncomingMessage(msg);
                            } else if ("RESPONSE".equalsIgnoreCase(option)) {
                                // When the server sends a response (including ping replies),
                                // process it in the dedicated response handler.
                                AsyncManager.run(() -> { processServerResponse(msg); });
                            } else {
                                // For anything else, do your fallback logic
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
                        // Retrieve the existing concealed parameters and update them.
                        String[] concealed = msg.getConcealedParameters();
                        if (concealed == null) {
                            concealed = new String[] { username };
                        } else {
                            String[] newConcealed = new String[concealed.length + 1];
                            System.arraycopy(concealed, 0, newConcealed, 0, concealed.length);
                            newConcealed[newConcealed.length - 1] = username;
                            concealed = newConcealed;
                        }
                        msg.setConcealedParameters(concealed);
            
                        InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);
                        // If the option is "GAME", send via simple UDP using the existing clientSocket.
                        if ("GAME".equalsIgnoreCase(msg.getOption())) {
                            String encoded = MessageCodec.encode(msg);
                            byte[] data = encoded.getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, dest, SERVER_PORT);
                            // Use the already initialized clientSocket for sending.
                            clientSocket.send(packet);
                            System.out.println("Best effort sent: " + encoded);
                        } else {
                            // For other options, use the reliable sender.
                            myReliableUDPSender.sendMessage(msg, dest, SERVER_PORT);
                        }
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
     * If the message is a ping reply (i.e. message type is "RESPONSE"),
     * the time difference (in milliseconds) since the last ping is printed.
     */
    private void processServerResponse(Message msg) {
        // Check if this is a ping reply.
        if ("PONG".equalsIgnoreCase(msg.getMessageType())) {
            if (pingManager != null) {
                game.updatePingIndicator(pingManager.getTimeDifferenceMillis());
            }
            // Exit after handling the ping reply.
            return;
        }
        
        // Handle other types of responses.
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
        } else if ("CHANGENAME".equalsIgnoreCase(msg.getMessageType())){
            Object[] params = msg.getParameters();
            String objectID = params[0].toString();
            String newObjectName = params[1].toString();
            List<GameObject> gameObjectList = game.getGameObjects();

            for (GameObject gameObject : gameObjectList) {
                if (gameObject.getId().equals(objectID)) {
                    gameObject.setName(newObjectName);
                }
            }
        }else if ("LOGIN".equalsIgnoreCase(msg.getMessageType().trim())) {
            System.out.println("Loggin in..");
            System.out.println(msg);
            if (msg.getParameters() == null || msg.getParameters().length < 1) {
                System.err.println("Error: LOGIN response missing parameters");
                return;
            }

            boolean playerFound = false;
            //get the USERID
            String assignedUUID = msg.getParameters()[0].toString();
            System.out.println("LOGIN confirmed for UUID: "+ assignedUUID);

            //compares all the USERID's with the ID from the created player
            for (GameObject gameObject : game.getGameObjects()) {
                if (gameObject.getId().equals(assignedUUID)){
                    System.out.println("Found gameObject: " + gameObject);
                    playerFound = true;

                    //initialize initUI for the Keybinds
                    SwingUtilities.invokeLater(() -> {
                        game.rebindKeyListeners(gameObject.getName());
                        this.clientName.set(gameObject.getName());
                        game.updateGamePanel();
                        game.updateActiveObject(gameObject.getName(), outgoingQueue);
                    });
                    System.out.println(gameObject.getName());

                }
            }

        }
        // handles Logout
        else if("LOGOUT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s +",""))) {
            //printing Logging out
            System.out.println("Logging out");

            System.exit(0);
            return;
        }
        //handles deletion of the player
        else if("DELETE".equalsIgnoreCase(msg.getMessageType())) {
            String targetPlayerId = msg.getParameters()[0].toString();

            for (GameObject go : game.getGameObjects()) {
                if (go.getName().equals(targetPlayerId)) {
                    game.getGameObjects().remove(go);
                    break;
                }
            }
            game.updateGamePanel();
        }


        else {
            System.out.println("Unhandled response type: " + msg.getMessageType());
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

    public void sendBulkCreateMessages() {
        AsyncManager.run(() -> {
            for (int i = 0; i < 50; i++) {
                float x = 300.0f + i * 50;
                float y = 200.0f + i * 50;
                // Create parameters array:
                // [ "Player", "Mike", x, y, 25.0f, "GameSession1" ]
                Object[] params = new Object[] {"Player", "Mike", x, y, 25.0f, "GameSession1"};
                
                // Create the message with type "CREATE" and option "REQUEST"
                Message createMsg = new Message("CREATE", params, "REQUEST");
                
                // Optionally, set concealed parameters if required.
                createMsg.setConcealedParameters(new String[] {"Mike", "GameSession1"});
                
                // Enqueue the message for sending.
                sendMessage(createMsg);
                
                // Optional: add a slight delay between messages to avoid flooding.
                try {
                    Thread.sleep(50); // 50 ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
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
