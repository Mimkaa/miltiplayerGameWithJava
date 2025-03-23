package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import chat.ChatManager;
import chat.ChatPanel;
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

    // Make queues static.
    private static final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    // The Game instance remains instance-specific.
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

    // Singleton instance.
    private static Client instance;

    // Constructor creates the Game object.
    public Client(String gameSessionName) {
        this.game = new Game(gameSessionName);
        instance = this;  // Set the singleton instance.
    }

    /**
     * Returns the singleton instance of the Client.
     */
    public static Client getInstance() {
        return instance;
    }

    /**
     * Initializes the client chat manager.
     * IMPORTANT: Call this BEFORE setting up the UI so that clientChatManager is not null.
     */
    public void initChatManager() {
        this.clientChatManager = new ChatManager.ClientChatManager(username, game.getGameName(), outgoingQueue, getIdGame());
        
    }

    public ChatPanel getChatPanel()
    {
        return  this.clientChatManager.getChatPanel();
    }

    /**
     * Starts the graphics-related tasks.
     */
    public void startGraphicsStuff() {
        SwingUtilities.invokeLater(() -> {
            // Initialize the game UI.
            game.initUI(username.get(), this);
            // You can also install the chat UI here.
        });

        AsyncManager.run(() -> {
            try {
                while (true) {
                    // Always use the current username for the active object.
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

            // IMPORTANT: Initialize chat manager BEFORE starting the UI.
            initChatManager();

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
                            // Process the ACK.
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
                            // Non-ACK message logic.
                            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                                InetSocketAddress dest = new InetSocketAddress(InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                                ackProcessor.addAck(dest, msg.getUUID());
                            }
                            String option = msg.getOption();
                            if ("GAME".equalsIgnoreCase(option)) {
                                game.addIncomingMessage(msg);
                            } else if ("RESPONSE".equalsIgnoreCase(option)) {
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

            // Sender Task: Continuously poll outgoingQueue and send messages.
            AsyncManager.runLoop(() -> {
                Message msg = outgoingQueue.poll();
                if (msg != null) {
                    try {
                        InetAddress dest = InetAddress.getByName(SERVER_ADDRESS);
                        if ("GAME".equalsIgnoreCase(msg.getOption())) {
                            String encoded = MessageCodec.encode(msg);
                            byte[] data = encoded.getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, dest, SERVER_PORT);
                            clientSocket.send(packet);
                            System.out.println("Best effort sent: " + encoded);
                        } else {
                            myReliableUDPSender.sendMessage(msg, dest, SERVER_PORT);
                            // Uncomment below to log reliable sends if needed.
                            // System.out.println("Reliable sent: " + MessageCodec.encode(msg));
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

    
    
    /**
     * Static sendMessage method to update the message with the latest username and enqueue it.
     */
    public static void sendMessageStatic(Message msg) {
        // Access the singleton instance's username.
        String currentUsername = instance.username.get();
        String[] concealed = msg.getConcealedParameters();
        if (concealed == null) {
            concealed = new String[] { currentUsername };
        } else {
            String[] newConcealed = new String[concealed.length + 1];
            System.arraycopy(concealed, 0, newConcealed, 0, concealed.length);
            newConcealed[newConcealed.length - 1] = currentUsername;
            concealed = newConcealed;
        }
        msg.setConcealedParameters(concealed);
        
        // Enqueue the message.
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
            if (params != null ) {
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
            String assignedUUID = msg.getParameters()[0].toString();
            System.out.println("LOGIN confirmed for UUID: " + assignedUUID);
            boolean playerFound = false;
            for (GameObject gameObject : game.getGameObjects()) {
                if (gameObject.getId().equals(assignedUUID)) {
                    System.out.println("Found gameObject: " + gameObject);
                    playerFound = true;
                    SwingUtilities.invokeLater(() -> {
                        game.rebindKeyListeners(gameObject.getName());
                        instance.username.set(gameObject.getName());
                        game.updateGamePanel();
                        game.updateActiveObject(instance.username.get(), outgoingQueue);
                    });
                    System.out.println(gameObject.getName());
                }
            }
        }
        else if("EXIT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+", ""))) {
            System.out.println("Exiting game...");
            System.exit(0);
            return;
        }
        else if ("LOGOUT".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Logging out " + msg.getParameters()[0].toString());
            try {
                String messagelogString = "DELETE{REQUEST}[" + msg.getParameters()[0].toString() + "]||";
                Message logoutMessage = MessageCodec.decode(messagelogString);
                sendMessageStatic(logoutMessage);
                System.out.println("Reliable sent: " + MessageCodec.encode(logoutMessage));
            } catch (Exception e) {
                System.err.println("Failed to send DELETE message reliably: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
                sendMessageStatic(msg);
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
                sendMessageStatic(createMsg);
                try {
                    Thread.sleep(50); // 50 ms delay to avoid flooding.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void login() {
        try {
            String createStr = "CREATE{REQUEST}[S:Player, S:" + username.get() 
                    + ", F:100.0, F:100.0, F:25.0, S:GameSession1]||";
            Message createMsg = MessageCodec.decode(createStr);
            String[] concealedParams = { "something1", "something2" };
            createMsg.setConcealedParameters(concealedParams);
            sendMessageStatic(createMsg);
            System.out.println("Sent CREATE message: " + createStr);
            Thread.sleep(1000);
            String loginStr = "LOGIN{REQUEST}[" + username.get() + "]||";
            Message loginMsg = MessageCodec.decode(loginStr);
            loginMsg.setConcealedParameters(concealedParams);
            sendMessageStatic(loginMsg);
            System.out.println("Sent LOGIN message: " + loginStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try {
            Scanner inputScanner = new Scanner(System.in);
            String suggestedNickname = Nickname_Generator.generateNickname();
            System.out.println("Suggested Nickname: " + suggestedNickname);
            System.out.println("Please enter to use suggested name, or type your own: ");
            String userName = inputScanner.nextLine();
            System.out.println("entered nick:" + userName);
            if (userName.isEmpty()) {
                userName = suggestedNickname;
            }
            Client client = new Client("GameSession1");
            client.setUsername(userName);
            System.out.println("Set client username: " + userName);
            client.initChatManager();
            client.startGraphicsStuff();
            client.startConsoleReaderLoop();
            new Thread(() -> client.run()).start();
            Thread.sleep(1000);
            client.login();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
