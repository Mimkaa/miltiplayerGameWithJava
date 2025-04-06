package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHub;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatPanel;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import lombok.Getter;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code Client} class represents a networked game client that communicates
 * with a server via UDP, sending and receiving messages related to gameplay and chat.
 * <p>
 * This class contains:
 * <ul>
 *   <li>A {@link Game} instance responsible for game logic.</li>
 *   <li>A {@code ChatManager.ClientChatManager} for handling chat functionality.</li>
 *   <li>Static queues for outgoing and incoming {@link Message} objects.</li>
 *   <li>Reliable and best-effort UDP message sending through {@link ReliableUDPSender}.</li>
 *   <li>An {@link AckProcessor} for handling acknowledgments (ACKs).</li>
 *   <li>An optional {@link PingManager} for tracking round-trip times (disabled by default in this code).</li>
 * </ul>
 *
 * <p>
 * Typical usage for running the client might look like this:
 * <ol>
 *   <li>Create an instance of {@code Client} with a desired game session name.</li>
 *   <li>Optionally customize the client username.</li>
 *   <li>Initialize chat via {@link #initChatManager()}.</li>
 *   <li>Set up the game UI using .</li>
 *   <li>Start console reading via {@link #startConsoleReaderLoop()}.</li>
 *   <li>Run the main loop in a separate thread via {@link #run()}.</li>
 *   <li>Use {@link #login()} to join the game session on the server.</li>
 * </ol>
 *
 * <p>
 * A singleton instance is maintained and can be retrieved with {@link #getInstance()}.
 */
@Getter
public class Client {

    /** The default server address (localhost). */
    public static final String SERVER_ADDRESS = "localhost";
    /** The default server port (9876). */
    public static final int SERVER_PORT = 9876;

    /** Manages chat functionality for this client. */
    public ChatManager.ClientChatManager clientChatManager;

    /** Static queue for outgoing messages. */
    private static final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    /** Static queue for incoming messages. */
    private static final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    /** Game logic object for this client. */
    private Game game;

    /** Console input scanner for reading commands. */
    private final Scanner scanner = new Scanner(System.in);

    private final String clientId = UUID.randomUUID().toString();

    /** The client's username, stored as an AtomicReference for thread safety. */
    private final AtomicReference<String> username = new AtomicReference<>(clientId);

    /** A reliable UDP sender instance. */
    private ReliableUDPSender myReliableUDPSender;
    /** A processor for handling incoming ACK messages and dispatching ACKs. */
    private AckProcessor ackProcessor;

    /** The UDP socket used by this client. */
    private DatagramSocket clientSocket;

    /** Tracks ping (round-trip time) data; left unused unless explicitly started. */
    private PingManager pingManager;

    /** The singleton client instance. */
    private static Client instance;

     // We add a MessageHub field to route incoming messages.
    private final MessageHub messageHub = MessageHub.getInstance();

    /**
     * Constructs a new {@code Client} with the given game session name and
     * initializes the associated {@link Game} object.
     *
     */
    public Client() {
        instance = this;  // Set the singleton instance.
    }

    /**
     * Returns the singleton {@code Client} instance.
     *
     * @return the singleton client instance
     */
    public static Client getInstance() {
        return instance;
    }

    /**
     * Initializes the client chat manager. Must be called <strong>before</strong>
     * setting up the UI to ensure {@code clientChatManager} is not null.
     */
    public void initChatManager() {
        this.clientChatManager = new ChatManager.ClientChatManager(
                username,
                game.getGameName(),
                outgoingQueue,
                game.getGameId()
        );
    }

    /**
     * Retrieves the chat panel UI component from the chat manager.
     *
     * @return the {@link ChatPanel} for the client
     */
    public ChatPanel getChatPanel() {
        return this.clientChatManager.getChatPanel();
    }



    /**
     * Main entry point for the client's networking logic, including:
     * <ul>
     *   <li>Opening a {@link DatagramSocket}.</li>
     *   <li>Starting a {@link ReliableUDPSender} and {@link AckProcessor}.</li>
     *   <li>Starting background loops for receiving and sending messages.</li>
     * </ul>
     *
     * <p>
     * This method blocks the current thread (using {@code join()}) to keep
     * the client alive indefinitely.
     */
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
            //initChatManager();

            // Start processing loops for player commands (optional, depends on Game logic).
            //game.startPlayersCommandProcessingLoop();

            // Optionally, we could start ping tracking here (commented out for demonstration).
            // pingManager = new PingManager(outgoingQueue, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT, 300);
            // pingManager.start();

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

            AsyncManager.runLoop(() -> {
                try {
                    Message msg = incomingQueue.poll();
                    if (msg != null) {
                        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
                            // Process the ACK message.
                            if (msg.getParameters() != null && msg.getParameters().length > 0) {
                                String ackUuid = msg.getParameters()[0].toString();
                                myReliableUDPSender.acknowledge(ackUuid);
                            } else {
                                System.out.println("Received ACK with no parameters.");
                            }
                        } else {
                            // For non-ACK messages, if the message has a UUID and option is not "GAME",
                            // add it to the ACK processor.
                            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                                InetSocketAddress dest = new InetSocketAddress(
                                    InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                                ackProcessor.addAck(dest, msg.getUUID());
                            }
                            // Now dispatch the message via the MessageHub.
                            messageHub.dispatch(msg);
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
                            // Send best effort for "GAME" messages
                            String encoded = MessageCodec.encode(msg);
                            byte[] data = encoded.getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, dest, SERVER_PORT);
                            clientSocket.send(packet);
                            System.out.println("Best effort sent: " + encoded);

                        } else if ("CLIENT".equalsIgnoreCase(msg.getOption())) {
                            // Perform local client state updates
                            AsyncManager.run(() -> updateLocalClientState(msg));
                        } else {
                            // Reliable send otherwise
                            myReliableUDPSender.sendMessage(msg, dest, SERVER_PORT);
                            // Uncomment for debug:
                            // System.out.println("Reliable sent: " + MessageCodec.encode(msg));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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
     * A static utility to send a {@link Message} by appending the current username
     * to the concealed parameters, then enqueuing it for sending.
     *
     * @param msg the {@link Message} to be sent
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

        // Enqueue the message for sending.
        AsyncManager.run(() -> outgoingQueue.offer(msg));
    }

    /**
     * Handles client-specific state updates based on certain message types.
     * E.g., a change of username or fast login trigger.
     *
     * @param msg the message containing state update instructions
     */
    private void updateLocalClientState(Message msg) {
        if ("CHANGE_USERNAME".equalsIgnoreCase(msg.getMessageType())) {
            this.username.set(msg.getParameters()[0].toString());
            System.out.println("Username Changed to: " + msg.getParameters()[0].toString());
        }
        if ("FAST_LOGIN".equalsIgnoreCase(msg.getMessageType())) {
            login();
        } else {
            System.out.println("Unhandled response type: " + msg.getMessageType());
        }
    }

    /**
     * Process server response messages. Common types include:
     * <ul>
     *   <li>PONG: For ping results.</li>
     *   <li>CREATE: Add a new game object to the client's {@link Game}.</li>
     *   <li>CHANGENAME: Rename an existing game object.</li>
     *   <li>LOGIN: Associate the client with a game object on the server.</li>
     *   <li>LOGOUT: Request to remove a given player from the game.</li>
     *   <li>DELETE: Remove a player from the client's {@link Game} object list.</li>
     *   <li>EXIT: Command the client to shut down.</li>
     * </ul>
     *
     * @param msg the response message to process
     */
    private void processServerResponse(Message msg) {
        if ("PONG".equalsIgnoreCase(msg.getMessageType())) {
            if (pingManager != null) {
                //game.updatePingIndicator(pingManager.getTimeDifferenceMillis());
            }
            return;
        }

        if ("CREATE".equalsIgnoreCase(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params != null) {
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
        } else if ("CHANGENAME".equalsIgnoreCase(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            String objectID = params[0].toString();
            String newObjectName = params[1].toString();
            List<GameObject> gameObjectList = game.getGameObjects();
            for (GameObject gameObject : gameObjectList) {
                if (gameObject.getId().equals(objectID)) {
                    // If it is the object we are controlling, also change our local username
                    if (gameObject.getName().equals(username.get())) {
                        username.set(newObjectName);
                    }
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
            for (GameObject gameObject : game.getGameObjects()) {
                if (gameObject.getId().equals(assignedUUID)) {
                    // Rebind local input handling to the newly confirmed object
                    SwingUtilities.invokeLater(() -> {
                        //game.rebindKeyListeners(gameObject.getName());
                        instance.username.set(gameObject.getName());
                        //game.updateGamePanel();

                    });
                }
            }
        } else if ("EXIT".equalsIgnoreCase(msg.getMessageType().replaceAll("\\s+", ""))) {
            System.out.println("Exiting game...");
            System.exit(0);
        } else if ("LOGOUT".equalsIgnoreCase(msg.getMessageType())) {
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
        } else if ("DELETE".equalsIgnoreCase(msg.getMessageType())) {
            String targetPlayerId = msg.getParameters()[0].toString();
            for (GameObject go : game.getGameObjects()) {
                if (go.getName().equals(targetPlayerId)) {
                    game.getGameObjects().remove(go);
                    break;
                }
            }

        }

        if ("CREATEGAME".equalsIgnoreCase(msg.getMessageType())) {
            // Expecting the response parameters to contain the game UUID and name.
            if (msg.getParameters() != null && msg.getParameters().length >= 2) {
                String newGameUuid = msg.getParameters()[0].toString();
                String newGameName = msg.getParameters()[1].toString();
                // Create a new Game instance with the received UUID and friendly name.
                Game newGame = new Game(newGameUuid, newGameName);
                // Optionally start the game loop or reinitialize the UI.
                newGame.startPlayersCommandProcessingLoop();
                // Update the client's game reference.
                this.game = newGame;
                System.out.println("CREATEGAME response received. New game created: "
                                   + newGameName + " with UUID: " + newGameUuid);

            } else {
                System.err.println("CREATEGAME response missing required parameters!");
            }
            return;
        }
    }

    /**
     * Continuously reads commands from the console and enqueues them as Messages.
     * Type "exit" (without quotes) to stop reading from the console.
     */
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

    /**
     * Changes the username for this client.
     *
     * @param newUsername the desired new username
     */
    public void setUsername(String newUsername) {
        username.set(newUsername);
    }

    /**
     * Sends many CREATE messages in quick succession for testing or debugging.
     * Each message creates a "Player" object at incrementally spaced coordinates.
     */
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

    /**
     * Performs a minimal "login" sequence by sending a CREATE (to spawn a Player)
     * followed by a LOGIN request. This simply demonstrates the initial handshake
     * that might be needed to join a game session.
     */
    public void login() {
        try {
            String createStr = "CREATE{REQUEST}[S:Player, S:" + username.get()
                    + ", F:100.0, F:100.0, F:25.0, S:GameSession1]||";
            Message createMsg = MessageCodec.decode(createStr);
            String[] concealedParams = { "something1", "something2" };
            createMsg.setConcealedParameters(concealedParams);
            sendMessageStatic(createMsg);
            System.out.println("Sent CREATE message: " + createStr);

            // Small delay before sending LOGIN to ensure CREATE is processed.
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

    /**
     * Main method for launching the client as a standalone application.
     * Prompts for a username (or auto-generates one), then:
     * <ul>
     *   <li>Sets the username</li>
     *   <li>Initializes chat</li>
     *   <li>Starts the game UI</li>
     *   <li>Begins console input reading</li>
     *   <li>Runs the client's main network loop in a background thread</li>
     *   <li>Issues a login request</li>
     * </ul>
     *
     * @param args ignored
     */
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

            Client client = new Client();
            client.setUsername(userName);
            System.out.println("Set client username: " + userName);

            //client.initChatManager();
            //client.startGraphicsStuff();
            client.startConsoleReaderLoop();

            new Thread(client::run).start();
            Thread.sleep(1000);

            //client.login();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
