package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHub;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHogger;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import lombok.Getter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Getter
/**
 * The {@code Server} class implements a simple UDP-based server that
 * can handle both reliable and best-effort messages. It manages a set of
 * registered clients, game sessions via a GameSessionManager, and the logic for
 * handling various message types and requests (including creation of new game objects,
 * game session creation/joining, and selection of game objects).
 */
public class Server {

    // ================================
    // Singleton Implementation
    // ================================
    private Server() { }
    private static class SingletonHelper {
        private static final Server INSTANCE = new Server();
    }
    public static Server getInstance() {
        return SingletonHelper.INSTANCE;
    }

    // ================================
    // Server Properties
    // ================================
    public static final int SERVER_PORT = 9876;
    private ChatManager.ServerChatManager serverChatManager;
    private final ConcurrentHashMap<String, InetSocketAddress> clientsMap = new ConcurrentHashMap<>();
    private DatagramSocket serverSocket;
    private ReliableUDPSender reliableSender;
    private AckProcessor ackProcessor;
    // The game session(s) are managed via GameSessionManager.
    private final GameSessionManager gameSessionManager = new GameSessionManager();
    // Install the MessageHub as a singleton.
    private final MessageHub messageHub = MessageHub.getInstance();
    // Outgoing messages queue.
    private final ConcurrentLinkedQueue<OutgoingMessage> outgoingQueue = new ConcurrentLinkedQueue<>();

    // ================================
    // Outgoing Message Inner Class
    // ================================
    private static class OutgoingMessage {
        Message msg;
        InetAddress address;
        int port;
        public OutgoingMessage(Message msg, InetAddress address, int port) {
            this.msg = msg;
            this.address = address;
            this.port = port;
        }
    }

    public static Message makeResponse(Message original, Object[] newParams) {
        String type = original.getMessageType();
        String[] concealed = original.getConcealedParameters();
        return new Message(type, newParams, "RESPONSE", concealed);
    }

    private void enqueueMessage(Message msg, InetAddress address, int port) {
        outgoingQueue.offer(new OutgoingMessage(msg, address, port));
    }

    private String findUniqueName(String requestedName) {
        String baseName = requestedName;
        int counter = 1;
        while (isNameTaken(requestedName)) {
            requestedName = baseName + "_" + counter++;
        }
        return requestedName;
    }

    private boolean isNameTaken(String name) {
        if (clientsMap.containsKey(name)) {
            return true;
        }
        // If you have a default game instance, check its objects as well.
        return false;
    }

    // ================================
    // Server Start Method
    // ================================
    public void start() {
        try {
            InetAddress ipAddress = InetAddress.getByName("25.12.99.19");
            InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, SERVER_PORT);
            serverSocket = new DatagramSocket(socketAddress);
            System.out.println("UDP Server is running on " + ipAddress.getHostAddress() + ":" + SERVER_PORT);

            reliableSender = new ReliableUDPSender(serverSocket, 50, 200);
            ackProcessor = new AckProcessor(serverSocket);
            ackProcessor.start();

            // Optionally initialize a default game session:
            // Game defaultGame = new Game("GameSession1", "Default Game");
            // defaultGame.startPlayersCommandProcessingLoop();
            // gameSessionManager.addGameSession("GameSession1", defaultGame);

            // Process outgoing messages.
            AsyncManager.runLoop(() -> {
                OutgoingMessage om = outgoingQueue.poll();
                if (om != null) {
                    try {
                        reliableSender.sendMessage(om.msg, om.address, om.port);
                        System.out.println("Sent message to " + om.address + ":" + om.port);
                    } catch (Exception e) {
                        System.err.println("Error sending message to " + om.address + ":" + om.port + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Listen for incoming packets.
            new Thread(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);

                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                        InetAddress clientAddress = packet.getAddress();
                        int clientPort = packet.getPort();
                        InetSocketAddress senderSocket = new InetSocketAddress(clientAddress, clientPort);
                        String messageString = new String(data);
                        System.out.println("Received: " + messageString + " from " + senderSocket);

                        Message msg = MessageCodec.decode(messageString);
                        AsyncManager.run(() -> processMessage(msg, senderSocket));
                        // Also dispatch the message to the MessageHub.
                        messageHub.dispatch(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================================
    // Message Processing
    // ================================
    private void processMessage(Message msg, InetSocketAddress senderSocket) {
        if ("ACK".equalsIgnoreCase(msg.getMessageType())) {
            String ackUuid = msg.getParameters()[0].toString();
            reliableSender.acknowledge(ackUuid);
            System.out.println("Processed ACK message for UUID " + msg.getUUID());
            return;
        }
        if ("CHAT".equalsIgnoreCase(msg.getMessageType())) {
            System.out.println("Processed CHAT message 1 ");
            if (serverChatManager == null) {
                serverChatManager = new ChatManager.ServerChatManager();
            }
            if (msg.getUUID() != null && !msg.getUUID().isEmpty()) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
            }
            AsyncManager.run(() -> broadcastMessageToAll(msg));
            System.out.println("Processed CHAT message 2");
            return;
        }

        String[] concealed = msg.getConcealedParameters();
        if (concealed != null && concealed.length >= 2) {
            String username = concealed[concealed.length - 1];
            if (clientsMap.containsKey(username)) {
                InetSocketAddress existingSocket = clientsMap.get(username);
                if (!existingSocket.equals(senderSocket)) {
                    if (msg.getUUID() != null && !msg.getUUID().isEmpty()) {
                        ackProcessor.addAck(senderSocket, msg.getUUID());
                    }
                    String suggestedNickname = Nickname_Generator.generateNickname();
                    Message collisionResponse = new Message("NAME_TAKEN", new Object[]{suggestedNickname}, "RESPONSE");
                    collisionResponse.setUUID("");
                    enqueueMessage(collisionResponse, senderSocket.getAddress(), senderSocket.getPort());
                    return;
                }
            } else {
                clientsMap.put(username, senderSocket);
            }
            System.out.println("Registered user: " + username + " at " + senderSocket + ". Total clients: " + clientsMap.size());
            if (msg.getUUID() != null && !"GAME".equalsIgnoreCase(msg.getOption())) {
                ackProcessor.addAck(senderSocket, msg.getUUID());
                System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
            }
            if ("GAME".equalsIgnoreCase(msg.getOption())) {
                processMessageBestEffort(msg, senderSocket);
            } else if ("REQUEST".equalsIgnoreCase(msg.getOption())) {
                AsyncManager.run(() -> handleRequest(msg, username));
            } else {
                broadcastMessageToOthers(msg, username);
            }
        } else {
            System.out.println("Concealed parameters missing or too short.");
        }
    }

    private void processMessageBestEffort(Message msg, InetSocketAddress senderSocket) {
        try {
            for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
                if (!entry.getValue().equals(senderSocket)) {
                    InetAddress dest = entry.getValue().getAddress();
                    int port = entry.getValue().getPort();
                    String encoded = MessageCodec.encode(msg);
                    byte[] data = encoded.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, dest, port);
                    serverSocket.send(packet);
                    System.out.println("Best effort sent message to " + entry.getKey() + " at " + entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessageToAll(Message msg) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            try {
                enqueueMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                System.out.println("Enqueued broadcast message to " + clientUsername + " at " + clientAddress);
            } catch (Exception e) {
                System.err.println("Error enqueuing message to " + clientUsername + " at " + clientAddress + ": " + e.getMessage());
            }
        }
    }

    public void broadcastMessageToOthers(Message msg, String excludedUsername) {
        for (Map.Entry<String, InetSocketAddress> entry : clientsMap.entrySet()) {
            String clientUsername = entry.getKey();
            InetSocketAddress clientAddress = entry.getValue();
            if (!clientUsername.equals(excludedUsername)) {
                try {
                    enqueueMessage(msg, clientAddress.getAddress(), clientAddress.getPort());
                    System.out.println("Enqueued message to " + clientUsername + " at " + clientAddress);
                } catch (Exception e) {
                    System.err.println("Error enqueuing message to " + clientUsername + " at " + clientAddress + ": " + e.getMessage());
                }
            }
        }
    }

    // ================================
    // Request Handling
    // ================================
    private void handleRequest(Message msg, String senderUsername) {
        switch (msg.getMessageType().replaceAll("\\s+", "").toUpperCase()) {
            case "CREATEGO":
                handleCreateGORequest(msg, senderUsername);
                break;
            case "PING":
                handlePingRequest(senderUsername);
                break;
           
            case "CHANGENAME":
                handleChangeNameRequest(msg);
                break;
            case "USERJOINED":
                handleUserJoinedRequest(msg);
                break;
            case "LOGOUT":
            case "EXIT":
                handleLogoutOrExitRequest(msg, senderUsername);
                break;
            case "DELETEGO":
                handleDeleteRequest(msg, senderUsername);
                break;
            case "CREATEGAME":
                handleCreateGameRequest(msg, senderUsername);
                break;
            case "JOINGAME":
                handleJoinGameRequest(msg, senderUsername);
                break;
            case "SELECTGO":
                handleSelectGO(msg, senderUsername);
                break;
            default:
                System.out.println("Unknown request type: " + msg.getMessageType());
                Message defaultResponse = makeResponse(msg, msg.getParameters());
                InetSocketAddress senderAddress = clientsMap.get(senderUsername);
                if (senderAddress != null) {
                    enqueueMessage(defaultResponse, senderAddress.getAddress(), senderAddress.getPort());
                    System.out.println("Sent default response to " + senderUsername);
                } else {
                    System.err.println("Sender address not found for user: " + senderUsername);
                }
        }
    }

    /**
     * Handles a CREATEGO request by creating a new game object in a specified game session.
     * Expects parameters:
     *   - Parameter 0: The game session ID.
     *   - Parameter 1: The object type.
     *   - Remaining parameters: Other constructor arguments.
     */
    private void handleCreateGORequest(Message msg, String senderUsername) {
        Object[] originalParams = msg.getParameters();
        if (originalParams == null || originalParams.length < 2) {
            System.err.println("CREATEGO request requires at least two parameters: game session ID and object type.");
            return;
        }
        // originalParams[0]: game session ID, originalParams[1]: object type, remaining are constructor parameters
    
        // Generate a new UUID for the game object.
        String serverGeneratedUuid = UUID.randomUUID().toString();
        
        // Build a new parameter array with one extra element.
        Object[] newParams = new Object[originalParams.length + 1];
        // Insert the generated UUID as the first parameter.
        newParams[0] = serverGeneratedUuid;
        // Copy all original parameters into newParams starting at index 1.
        System.arraycopy(originalParams, 0, newParams, 1, originalParams.length);
        
        // Build the CREATEGO response message using the new parameter array.
        Message responseMsg = new Message("CREATEGO", newParams, "RESPONSE", msg.getConcealedParameters());
        
        // The newParams array is now structured as:
        // [0] generated UUID, [1] game session ID, [2] object type, [3...] additional constructor parameters.
        // Extract constructor parameters starting from index 3.
        Object[] constructorParams = new Object[0];
        if (newParams.length > 3) {
            constructorParams = java.util.Arrays.copyOfRange(newParams, 3, newParams.length);
        }
        
        // Retrieve the target game session from the manager using the game session ID (newParams[1]).
        String gameSessionId = newParams[1].toString();
        Game targetGame = gameSessionManager.getGameSession(gameSessionId);
        if (targetGame == null) {
            System.err.println("No game session found with ID: " + gameSessionId);
            return;
        }
        
        // Asynchronously create the game object using the factory.
        Future<GameObject> futureObj = targetGame.addGameObjectAsync(newParams[2].toString(), serverGeneratedUuid, constructorParams);
        try {
            GameObject newObj = futureObj.get();
            System.out.println("Created new game object with UUID: " + serverGeneratedUuid
                    + " and name: " + newObj.getName() + " in game session: " + gameSessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Broadcast the CREATEGO response message to all clients.
        System.out.println("Broadcasting CREATEGO to all: " + responseMsg);
        broadcastMessageToAll(responseMsg);
        
        // Optionally, for the new user, send existing objects from this session.
        InetSocketAddress newUserAddress = clientsMap.get(senderUsername);
        if (newUserAddress != null) {
            for (GameObject gameObject : targetGame.getGameObjects()) {
                if (gameObject.getId().equals(serverGeneratedUuid)) continue;
                Object[] constructorParameters = gameObject.getConstructorParamValues();
                Object[] finalParameters = new Object[constructorParameters.length + 2];
                finalParameters[0] = gameObject.getId();
                String objType = gameObject.getClass().getSimpleName();
                finalParameters[1] = objType;
                System.arraycopy(constructorParameters, 0, finalParameters, 2, constructorParameters.length);
                Message createResponseMessage = makeResponse(msg, finalParameters);
                enqueueMessage(createResponseMessage, newUserAddress.getAddress(), newUserAddress.getPort());
            }
        } else {
            System.err.println("No known address for user: " + senderUsername);
        }
        
        // After handling CREATEGO, print all game sessions (IDs and names) for debugging.
        System.out.println("Current game sessions:");
        gameSessionManager.getAllGameSessions().forEach((id, gameSession) -> {
            System.out.println("  Game Session ID: " + id + " | Name: " + gameSession.getGameName());
        });
    }
    

    private void handlePingRequest(String senderUsername) {
        InetSocketAddress senderAddress = clientsMap.get(senderUsername);
        if (senderAddress != null) {
            Message responseMsg = new Message("PONG", new Object[]{}, "RESPONSE");
            enqueueMessage(responseMsg, senderAddress.getAddress(), senderAddress.getPort());
            System.out.println("Enqueued PONG response to " + senderUsername);
        }
    }

   

    private void handleChangeNameRequest(Message msg) {
        Object[] originalParams = msg.getParameters();
        if (originalParams == null || originalParams.length < 3) {
            System.err.println("CHANGENAME request missing parameters. Expected: [gameSessionId, objectUUID, newName]");
            return;
        }
        
        // Parameter 0: game session ID
        String gameSessionId = originalParams[0].toString();
        // Parameter 1: game object's UUID
        String objectId = originalParams[1].toString();
        // Parameter 2: requested new name
        String requestedName = originalParams[2].toString();
        
        // Retrieve the game session from the manager.
        Game game = gameSessionManager.getGameSession(gameSessionId);
        if (game == null) {
            System.err.println("No game session found with id: " + gameSessionId);
            return;
        }
        
        // Determine a unique new name.
        String newName = findUniqueName(requestedName);
        
        // Find the game object by its UUID and update its name.
        String foundId = "";
        for (GameObject gameObject : game.getGameObjects()) {
            if (gameObject.getId().equals(objectId)) {
                foundId = gameObject.getId();
                gameObject.setName(newName);
                break;
            }
        }
        
        if (foundId.isEmpty()) {
            System.err.println("No game object found with UUID: " + objectId);
            return;
        }
        
        // Build a response message that includes all of the original parameters.
        // We update the third parameter (index 2) to the new name.
        Object[] responseParams = java.util.Arrays.copyOf(originalParams, originalParams.length);
        responseParams[2] = newName;
        
        Message responseMsg = new Message("CHANGENAME", responseParams, "RESPONSE");
        broadcastMessageToAll(responseMsg);
    }
    

    private void handleUserJoinedRequest(Message msg) {
        String nickname = msg.getParameters()[0].toString();
        boolean hasRepetition = clientsMap.containsKey(nickname);
        if (hasRepetition) {
            String suggestedNickname = Nickname_Generator.generateNickname();
            Message responseMsg = new Message("USERJOINED", new Object[]{suggestedNickname}, "RESPONSE");
            InetSocketAddress clientAddress = clientsMap.get(nickname);
            if (clientAddress != null) {
                enqueueMessage(responseMsg, clientAddress.getAddress(), clientAddress.getPort());
            }
        }
    }

    private void handleLogoutOrExitRequest(Message msg, String senderUsername) {
        String type = msg.getMessageType().replaceAll("\\s+", "").toUpperCase();
        System.out.println("Client " + type + ": " + senderUsername);
        Message logoutMessage = new Message(type, msg.getParameters(), "RESPONSE");
        InetSocketAddress clientAddress = clientsMap.get(senderUsername);
        if (clientAddress != null) {
            enqueueMessage(logoutMessage, clientAddress.getAddress(), clientAddress.getPort());
        }
        clientsMap.remove(senderUsername);
        System.out.println("Removed user: " + senderUsername);
    }

    

    private void handleDeleteRequest(Message msg, String senderUsername) {
        // Expecting parameters: [gameSessionId, objectId]
        Object[] originalParams = msg.getParameters();
        if (originalParams == null || originalParams.length < 2) {
            System.err.println("DELETE request missing required parameters. Expected: [gameSessionId, objectId]");
            return;
        }
        
        String sessionId = originalParams[0].toString();
        String objectId = originalParams[1].toString();
        
        // Retrieve the game session from the manager.
        Game game = gameSessionManager.getGameSession(sessionId);
        if (game == null) {
            System.out.println("No game session found with id: " + sessionId);
            return;
        }
        
        // Remove the game object with the matching UUID.
        boolean removed = game.getGameObjects().removeIf(go -> go.getId().equals(objectId));
        if (removed) {
            System.out.println("Deleted game object with id: " + objectId + " from session " + sessionId);
        } else {
            System.out.println("No game object with id " + objectId + " found in session " + sessionId);
        }
        
        // Build a response message containing all original parameters.
        Message responseMsg = new Message("DELETEGO", originalParams, "RESPONSE");
        broadcastMessageToAll(responseMsg);
    }

    private void handleCreateGameRequest(Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 1) {
            System.err.println("CREATEGAME request missing game name parameter.");
            return;
        }
        String requestedGameName = params[0].toString();
        String gameUuid = UUID.randomUUID().toString();
        Game newGame = new Game(gameUuid, requestedGameName);
        gameSessionManager.addGameSession(gameUuid, newGame);
        Message response = new Message("CREATEGAME", new Object[]{gameUuid, requestedGameName}, "RESPONSE", msg.getConcealedParameters());
        InetSocketAddress senderAddress = clientsMap.get(senderUsername);
        if (senderAddress != null) {
            broadcastMessageToAll(response);
            System.out.println("Created new game session '" + requestedGameName + "' with UUID: " + gameUuid);
        } else {
            System.err.println("Sender address not found for user: " + senderUsername);
        }
    }

    private void handleJoinGameRequest(Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        
        // 1) Validate we got the game name as a parameter
        if (params == null || params.length < 1) {
            System.err.println("JOINGAME request missing the game name to join.");
            return;
        }
        
        // 2) Extract the requested name of the game from the parameters
        String requestedGameName = params[0].toString();
        
        // 3) Search your GameSessionManager to find a Game with that name
        Game foundGame = null;
        for (Map.Entry<String, Game> entry : gameSessionManager.getAllGameSessions().entrySet()) {
            Game candidate = entry.getValue();
            // Compare ignoring case or exactly — your choice
            if (candidate.getGameName().equalsIgnoreCase(requestedGameName)) {
                foundGame = candidate;
                break;
            }
        }
        
        // 4) If no game was found, send back an error or create a new one
        if (foundGame == null) {
            System.err.println("No game session found with name: " + requestedGameName);
            
            // Option A: Return an error response to the client
            Message errorResponse = new Message(
                "JOINGAME_ERROR",
                new Object[]{"No game found with name: " + requestedGameName},
                "RESPONSE",
                msg.getConcealedParameters()
            );
            InetSocketAddress senderAddress = clientsMap.get(senderUsername);
            if (senderAddress != null) {
                enqueueMessage(errorResponse, senderAddress.getAddress(), senderAddress.getPort());
            }
            return;
            
            // Option B (Alternative): Create a new Game if not found
            // ...
        }
        
        // 5) At this point, `foundGame` is the correct session. Grab its ID
        String foundGameId = foundGame.getGameId();
        System.out.println("User " + senderUsername 
                           + " joined game session with name: " + requestedGameName 
                           + " (ID: " + foundGameId + ")");
        
        // 6) Send back a success response containing the found game’s ID
        Message response = new Message(
            "JOINGAME",
            new Object[]{foundGameId},
            "RESPONSE",
            msg.getConcealedParameters()
        );
        InetSocketAddress senderAddress = clientsMap.get(senderUsername);
        if (senderAddress != null) {
            enqueueMessage(response, senderAddress.getAddress(), senderAddress.getPort());
        } else {
            System.err.println("Sender address not found for user: " + senderUsername);
        }
    }
    

    /**
     * Handles a SELECTGO request by expecting two parameters:
     *   - Parameter 0: The game session ID.
     *   - Parameter 1: The game object name.
     * It retrieves the game session by ID and then searches its game objects for a matching name.
     * If found, it responds with the game object's UUID.
     */
    private void handleSelectGO(Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 2) {
            System.err.println("SELECTGO request requires two parameters: game session ID and game object name.");
            return;
        }
        String targetGameId = params[0].toString();
        String targetObjectName = params[1].toString();
        
        // Retrieve the game session directly by its ID.
        Game targetGame = gameSessionManager.getGameSession(targetGameId);
        if (targetGame == null) {
            System.out.println("No game session found with ID: " + targetGameId);
            return;
        }
        
        // Loop through the game objects in the target game.
        for (GameObject go : targetGame.getGameObjects()) {
            if (go.getName().equalsIgnoreCase(targetObjectName)) {
                Message response = new Message("SELECTGO", new Object[]{go.getId()}, "RESPONSE", msg.getConcealedParameters());
                InetSocketAddress senderAddress = clientsMap.get(senderUsername);
                if (senderAddress != null) {
                    enqueueMessage(response, senderAddress.getAddress(), senderAddress.getPort());
                    System.out.println("Sent SELECTGO response: game object UUID: " + go.getId());
                } else {
                    System.err.println("Sender address not found for user: " + senderUsername);
                }
                return;
            }
        }
        System.out.println("No game object with name \"" + targetObjectName + "\" found in game with ID \"" + targetGameId + "\".");
    }

    // ================================
    // Main Method
    // ================================
    public static void main(String[] args) {
        Server.getInstance().start();
    }
}
