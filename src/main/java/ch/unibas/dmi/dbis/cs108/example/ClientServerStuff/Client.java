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
import chat.ChatManager;
import chat.ChatUIHelper;

public class Client {
    public static final String SERVER_ADDRESS = "25.12.99.19";
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

    // Additional client state.
    private String idGameObject;
    private String idGame;
    private final AtomicReference<String> clientName = new AtomicReference<>("");

    // Reliable UDP Sender.
    private ReliableUDPSender myReliableUDPSender;
    // AckProcessor for sending ACK messages.
    private AckProcessor ackProcessor;

    // Client socket.
    private DatagramSocket clientSocket;

    // Instance of PingManager (optional).
    private PingManager pingManager;

    // Chat integration: dedicated client chat manager.
    private ChatManager.ClientChatManager clientChatManager;

    // Constructor creates the Game object.
    public Client(String gameSessionName) {
        this.game = new Game(gameSessionName);
    }

    /**
     * Initializes the client chat manager.
     * IMPORTANT: Call this BEFORE setting up the UI so that clientChatManager is not null.
     */
    public void initChatManager() {
        this.clientChatManager = new ChatManager.ClientChatManager(username, game.getGameName(), outgoingQueue);
    }

    /**
     * Starts the graphics-related tasks.
     * After initializing the game UI, this method installs the chat UI using ChatUIHelper.
     */
    public void startGraphicsStuff(String initialClientName) {
        clientName.set(initialClientName); // Set initial name

        SwingUtilities.invokeLater(() -> {
            // Initialize the game UI.
            game.initUI(clientName.get());
            // Now install the chat UI. This adds a toggle button (Open Chat / Close Chat) at the top.
            // Note: game.getFrame() must return the JFrame used in initUI().
            ChatUIHelper.installChatUI(game.getFrame(), clientChatManager.getChatPanel());
        });

        // Update active object in a loop.
        AsyncManager.run(() -> {
            try {
                while (true) {
                    game.updateActiveObject(clientName.get(), outgoingQueue);
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void run() {
        try {
            // Initialize the client socket.
            clientSocket = new DatagramSocket();

            // Initialize the reliable sender.
            myReliableUDPSender = new ReliableUDPSender(clientSocket, 50, 1000);

            // Initialize the AckProcessor.
            ackProcessor = new AckProcessor(clientSocket);
            ackProcessor.start();

            // Create and start PingManager (if needed).
            InetAddress serverInet = InetAddress.getByName(SERVER_ADDRESS);
            // Uncomment below to start pinging:
            // pingManager = new PingManager(outgoingQueue, serverInet, SERVER_PORT, 300);
            // pingManager.start();

            // IMPORTANT: Ensure the chat manager is initialized BEFORE starting the UI.
            initChatManager();

            // Send a mock message.
            Message mockMessage = new Message("MOCK", new Object[] { "Hello from " + username }, "REQUEST");
            String[] concealedPrms = { "something1", "something2", username };
            mockMessage.setConcealedParameters(concealedPrms);
            myReliableUDPSender.sendMessage(mockMessage, serverInet, SERVER_PORT);

            game.startPlayersCommandProcessingLoop();

            // Receiver Task: Continuously listen for UDP packets.
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
                            if (msg.getParameters() != null && msg.getParameters().length > 0) {
                                String ackUuid = msg.getParameters()[0].toString();
                                myReliableUDPSender.acknowledge(ackUuid);
                            } else {
                                System.out.println("Received ACK with no parameters.");
                            }
                        } else if ("CHAT".equalsIgnoreCase(msg.getMessageType())) {
                            // Delegate chat processing to clientChatManager.
                            clientChatManager.processIncomingChatMessage(msg);
                        } else {
                            if (msg.getUUID() != null) {
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
                        if ("GAME".equalsIgnoreCase(msg.getOption())) {
                            String encoded = MessageCodec.encode(msg);
                            byte[] data = encoded.getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, dest, SERVER_PORT);
                            clientSocket.send(packet);
                            System.out.println("Best effort sent: " + encoded);
                        } else {
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
     * Processes server response messages.
     */
    private void processServerResponse(Message msg) {
        System.out.println("Handling RESPONSE message: " + msg);
        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 7) {
                String serverUuid = params[0].toString();
                String objectType = params[1].toString();
                Object[] remainingParams = java.util.Arrays.copyOfRange(params, 2, params.length);
                Future<GameObject> futureObj = game.addGameObjectAsync(objectType, serverUuid, remainingParams);
                try {
                    GameObject newObj = futureObj.get();
                    System.out.println("Created new game object with UUID: " + serverUuid + " and name: " + newObj.getName());
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
            boolean playerFound = false;
            String assignedUUID = msg.getParameters()[0].toString();
            System.out.println("LOGIN confirmed for UUID: "+ assignedUUID);
            for (GameObject gameObject : game.getGameObjects().toArray(new GameObject[0])) {
                if (gameObject.getId().equals(assignedUUID)){
                    System.out.println("Found gameObject: " + gameObject);
                    playerFound = true;
                    SwingUtilities.invokeLater(() -> {
                        game.rebindKeyListeners(gameObject.getName());
                        clientName.set(gameObject.getName());
                        game.updateGamePanel();
                        game.updateActiveObject(gameObject.getName(), outgoingQueue);
                    });
                    System.out.println(gameObject.getName());
                }
            }
        } else {
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
                Object[] params = new Object[]{"Player", "Mike", x, y, 25.0f, "GameSession1"};
                Message createMsg = new Message("CREATE", params, "REQUEST");
                createMsg.setConcealedParameters(new String[]{"Mike", "GameSession1"});
                sendMessage(createMsg);
                try {
                    Thread.sleep(50);
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

            // IMPORTANT: Initialize the chat manager BEFORE starting graphics/UI.
            client.initChatManager();

            // Start the graphical interface (this will also install the chat UI).
            client.startGraphicsStuff(randomName);
            client.startConsoleReaderLoop();
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
