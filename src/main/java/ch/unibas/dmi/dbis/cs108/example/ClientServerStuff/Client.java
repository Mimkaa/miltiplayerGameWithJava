package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import chat.ChatManager;
import lombok.Getter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
@Getter
public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 9876;

    public ChatManager.ClientChatManager clientChatManager;

    // Global queue for outgoing messages.
    private final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    // Global queue for incoming messages.
    private final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    // The Game instance.
    private final Game game;

    // Scanner for the terminal.
    private final Scanner scanner = new Scanner(System.in);

    // The client's username as an atomic reference.
    private final AtomicReference<String> username = new AtomicReference<>(UUID.randomUUID().toString());

    // Additional client state.
    private String idGameObject;
    private String idGame;

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
     * Initializes the client chat manager.
     * IMPORTANT: Call this BEFORE setting up the UI so that clientChatManager is not null.
     */
    public void initChatManager() {
        this.clientChatManager = new ChatManager.ClientChatManager(username, game.getGameName(), outgoingQueue, getIdGame()); //TODO: look at client uuid.
    }


    /**
     * Starts the graphics-related tasks.
     */
    public void startGraphicsStuff() {

        SwingUtilities.invokeLater(() -> {
            // Initialize the game UI.
            game.initUI(username.get(), this);
            // Now install the chat UI. This adds a toggle button (Open Chat / Close Chat) at the top.
            // Note: game.getFrame() must return the JFrame used in initUI().
        });



        AsyncManager.run(() -> {
            try {
                while (true) {
                    // Always use the current username
                    game.updateActiveObject(username.get(), outgoingQueue);
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

            // IMPORTANT: Ensure the chat manager is initialized BEFORE starting the UI.
            initChatManager();


            // (Optional) Create and start PingManager if needed.
            // InetAddress serverInet = InetAddress.getByName(SERVER_ADDRESS);
            // pingManager = new PingManager(outgoingQueue, serverInet, SERVER_PORT, 300);
            // pingManager.start();

           /* Message mockMessage = new Message("MOCK", new Object[] { "Hello from " + username.get() }, "REQUEST");
            String[] concealedPrms = { "something1", "something2", username.get() };
            mockMessage.setConcealedParameters(concealedPrms);
            InetAddress serverInet = InetAddress.getByName(SERVER_ADDRESS);
            myReliableUDPSender.sendMessage(mockMessage, serverInet, SERVER_PORT);

            */




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
                    // Poll a message from the incomingQueue.
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
                        }
                        if ("CHAT".equalsIgnoreCase(msg.getMessageType())) {
                            // Delegate chat processing to clientChatManager.
                            clientChatManager.processIncomingChatMessage(msg, ackProcessor);
                            System.out.println("Processed CHAT message");
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
                                // Process responses (including ping replies) asynchronously.
                                AsyncManager.run(() -> processServerResponse(msg));
                            } else {
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
                        // Update the concealed parameters to always include the latest username.
                        String[] concealed = msg.getConcealedParameters();
                        if (concealed == null) {
                            concealed = new String[] { username.get() };
                        } else {
                            String[] newConcealed = new String[concealed.length + 1];
                            System.arraycopy(concealed, 0, newConcealed, 0, concealed.length);
                            newConcealed[newConcealed.length - 1] = username.get();
                            concealed = newConcealed;
                        }
                        msg.setConcealedParameters(concealed);
            
                        InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);
                        // If the option is "GAME", send via simple UDP using the existing clientSocket.
                        if ("GAME".equalsIgnoreCase(msg.getOption())) {
                            String encoded = MessageCodec.encode(msg);
                            byte[] data = encoded.getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, dest, SERVER_PORT);
                            clientSocket.send(packet);
                            System.out.println("Best effort sent: " + encoded);
                        } else {
                            // For other options, use the reliable sender.
                            myReliableUDPSender.sendMessage(msg, dest, SERVER_PORT);
                            System.out.println("Reliable sent: " + MessageCodec.encode(msg));
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
        AsyncManager.run(() -> outgoingQueue.offer(msg));
        
    }
    
    /**
     * Processes messages from the server.
     */
    private void processServerResponse(Message msg) {
        if ("PONG".equalsIgnoreCase(msg.getMessageType())) {
            if (pingManager != null) {
                game.updatePingIndicator(pingManager.getTimeDifferenceMillis());
            }
            return;
        }
        
        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 7) {
                String serverUuid = params[0].toString();
                String objectType = params[1].toString();
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
        } else if ("LOGIN".equalsIgnoreCase(msg.getMessageType().trim())) {
            System.out.println("Logging in...");
            System.out.println(msg);
            if (msg.getParameters() == null || msg.getParameters().length < 1) {
                System.err.println("Error: LOGIN response missing parameters");
                return;
            }

            // Get the assigned UUID and update the username accordingly.
            String assignedUUID = msg.getParameters()[0].toString();
            System.out.println("LOGIN confirmed for UUID: " + assignedUUID);

            boolean playerFound = false;
            for (GameObject gameObject : game.getGameObjects()) {
                if (gameObject.getId().equals(assignedUUID)) {
                    System.out.println("Found gameObject: " + gameObject);
                    playerFound = true;

                    SwingUtilities.invokeLater(() -> {
                        game.rebindKeyListeners(gameObject.getName());
                        // Update the atomic username.
                        username.set(gameObject.getName());
                        game.updateGamePanel();
                        game.updateActiveObject(username.get(), outgoingQueue);
                    });
                    System.out.println(gameObject.getName());
                }
            }

        }
        // handles Exit, it closes the client
        else if("EXIT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s +",""))) {
            //printing Logging out
            System.out.println("Exiting game...");

            System.exit(0);
            return;
        }
        else if ("LOGOUT".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Logging out " + msg.getParameters()[0].toString());

            try {
                String[] concealedParams = { "something1", "something2" };
                String messagelogString = "DELETE{REQUEST}[" + msg.getParameters()[0].toString() + "]||";

                // Decode a new DELETE message
                Message logoutMessage = MessageCodec.decode(messagelogString);
                logoutMessage.setConcealedParameters(concealedParams);

                // Create the destination (same as your server address/port)
                InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);

                // Send it via reliable sender
                myReliableUDPSender.sendMessage(logoutMessage, dest, SERVER_PORT);
                System.out.println("Reliable sent: " + MessageCodec.encode(logoutMessage));

            } catch (Exception e) {
                // If you want more specific handling, consider catching the exact exceptions
                // e.g., UnknownHostException, IOException, etc.
                System.err.println("Failed to send DELETE message reliably: " + e.getMessage());
                e.printStackTrace();
            }
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

    public void setUsername(String newUsername) {
        username.set(newUsername);
    }

    public void sendBulkCreateMessages() {
        AsyncManager.run(() -> {
            for (int i = 0; i < 50; i++) {
                float x = 300.0f + i * 50;
                float y = 200.0f + i * 50;
                Object[] params = new Object[] {"Player", "Mike", x, y, 25.0f, "GameSession1"};
                Message createMsg = new Message("CREATE", params, "REQUEST");
                createMsg.setConcealedParameters(new String[] {"Mike", "GameSession1"});
                sendMessage(createMsg);
                try {
                    Thread.sleep(50); // 50 ms delay to avoid flooding
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void login() {
        try {
            // Build the string for the CREATE message.
            // Format: CREATE{REQUEST}[S:Player, S:<username>, F:100.0, F:100.0, F:25.0, S:GameSession1]||
            String createStr = "CREATE{REQUEST}[S:Player, S:" + username.get() 
                    + ", F:100.0, F:100.0, F:25.0, S:GameSession1]||";
            Message createMsg = MessageCodec.decode(createStr);
            String[] concealedParams = { "something1", "something2" };
            createMsg.setConcealedParameters(concealedParams);
            
            // Queue the CREATE message.
            sendMessage(createMsg);
            System.out.println("Sent CREATE message: " + createStr);
            
            // Optionally, wait for a short period to allow the CREATE to be processed.
            Thread.sleep(1000);
            
            // Build the string for the LOGIN message.
            // Format: LOGIN{REQUEST}[<username>]||
            String loginStr = "LOGIN{REQUEST}[" + username.get() + "]||";
            Message loginMsg = MessageCodec.decode(loginStr);
            loginMsg.setConcealedParameters(concealedParams);
            
            // Queue the LOGIN message.
            sendMessage(loginMsg);
            System.out.println("Sent LOGIN message: " + loginStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) {
        try {
            Scanner inputScanner = new Scanner(System.in);

            // generate suggested name
            String suggestedNickname = Nickname_Generator.generateNickname();
            System.out.println("Suggested Nickname: " + suggestedNickname);
            System.out.println("Please enter to use suggested name, or type your own: ");

            //Get the nickname
            String userName = inputScanner.nextLine();

            // If nothing was entered, use the suggested nickname
            System.out.println("entered nick:"+userName);
            if (userName.isEmpty()) {
                userName = suggestedNickname;
            }





    
            Client client = new Client("GameSession1");
            client.setUsername(userName);
            System.out.println("Set client username: " + userName);

            // IMPORTANT: Initialize the chat manager BEFORE starting graphics/UI.
            client.initChatManager();


            // Start the graphical interface and console reader.
            client.startGraphicsStuff();
            client.startConsoleReaderLoop();
    
            // First, run the network initialization.
            new Thread(() -> client.run()).start();
            
            // Then, once run() is up and the reliable sender is initialized, you can call fullLogin().
            // Consider waiting a little bit or using a callback/flag to ensure initialization.
            Thread.sleep(1000); 
            client.login();
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
