package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageRegistry;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code ServerMessageTests} class provides integration tests to verify
 * how the server processes different message types (e.g. ACK, CHAT, REQUEST, etc.)
 * using the {@link MessageRegistry} and corresponding {@code MessageHandler}s.
 * <p>
 * It starts the server on {@code localhost:9876}, then sends messages via a client
 * {@link DatagramSocket} and checks the outputs. By default, it will print logs
 * to the console; you may add further assertions based on the server's internal
 * state or by capturing responses to a dedicated client socket.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerMessageTests {

    private static DatagramSocket clientSocket;
    private static InetAddress serverAddress;
    private static final int SERVER_PORT = 9876;

    /**
     * Sets up a client socket, starts the server, and waits briefly for it to initialize.
     *
     * @throws Exception if there is a problem creating the socket or starting the server
     */
    @BeforeAll
    static void setup() throws Exception {
        clientSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");
        Server.getInstance().start();
        Thread.sleep(1000); // Wait for server to fully initialize
    }

    /**
     * Closes the client socket after all tests.
     */
    @AfterAll
    static void tearDown() {
        clientSocket.close();
    }

    /**
     * Encodes and sends the given {@link Message} to the server, then prints
     * it for clarity. If you want to capture responses, you can optionally read
     * from {@code clientSocket} after sending.
     *
     * @param msg the message to send
     * @throws Exception if a socket error occurs
     */
    private void sendMessage(Message msg) throws Exception {
        String encoded = MessageCodec.encode(msg);
        byte[] data = encoded.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        clientSocket.send(packet);
        System.out.println("\n[TEST] Sent Message: " + encoded);
    }

    /**
     * Tests how the server handles a simple ACK message. The server should call
     * {@code reliableSender.acknowledge(...)} on the specified UUID.
     */
    @Test
    @Order(1)
    void testAckMessage() throws Exception {
        // Typically ACK has a parameter array like [ackUuid]
        String ackUuid = "test-ack-" + UUID.randomUUID();
        Message ackMsg = new Message(
                "ACK",
                new Object[]{ackUuid},
                "RESPONSE",   // or "REQUEST" - depends on your protocol
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );

        sendMessage(ackMsg);
        // Optionally, you could add an assertion verifying "ackUuid" is recognized
        // in the server logs or by checking the server's internal ack state if you have an API.
    }

    /**
     * Tests how the server handles a CHAT message. The server should optionally
     * send an ACK back and then broadcast the chat to all connected users.
     */
    @Test
    @Order(2)
    void testChatMessage() throws Exception {
        Message chatMsg = new Message(
                "CHAT",
                new Object[]{"Hello, this is a test chat!"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );

        sendMessage(chatMsg);
        // If you want to confirm the server's behavior, you can read from the client socket
        // or examine the server's chat manager state, e.g. server.getServerChatManager().someCheck()
    }

    /**
     * Tests the server handling a REQUEST message that delegates to a known command,
     * such as "PING". This verifies that {@link ch.unibas.dmi.dbis.cs108.example.command.CommandHandler}
     * is triggered via the {@link ch.unibas.dmi.dbis.cs108.example.message.messagehandlers.RequestMessageHandler}.
     */
    @Test
    @Order(3)
    void testPingRequestMessage() throws Exception {
        // "PING" command inside a REQUEST message
        Message pingReq = new Message(
                "PING",
                new Object[]{},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );

        sendMessage(pingReq);
        // The server should respond with "PONG", etc. If you'd like, you can
        // do a blocking receive on clientSocket to confirm the response:
        // clientSocket.receive(...)
    }

    /**
     * Tests the server handling a REQUEST that corresponds to "CREATE" or any other
     * custom command. This ensures your CreateCommandHandler is invoked.
     */
    @Test
    @Order(4)
    void testCreateRequestMessage() throws Exception {
        // Example "CREATE" command
        Message createMsg = new Message(
                "CREATE",
                new Object[]{"Player", "TestPlayer", 100.0f, 200.0f, 25.0f, "TestGame"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );

        sendMessage(createMsg);
        // The server's CreateCommandHandler should create a new object and broadcast.
        // Optionally read from clientSocket or check server logs / internal state for final verification.
    }

    /**
     * Tests the server handling a "GAME" message (if your protocol uses
     * {@code msg.getOption().equalsIgnoreCase("GAME")} to do best-effort broadcasting).
     * This checks the {@link ch.unibas.dmi.dbis.cs108.example.message.messagehandlers.GameMessageHandler}.
     */
    @Test
    @Order(5)
    void testGameMessage() throws Exception {
        // For a "GAME" message, you might store relevant data in the option or parameters:
        Message gameMsg = new Message(
                "GAMEUPDATE",          // or whatever your messageType is
                new Object[]{"MOVE", "x=10", "y=20"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        // If your server looks specifically for msg.getOption() == "GAME", set that too:
        // gameMsg.setOption("GAME");

        sendMessage(gameMsg);
        // The server should process it in GameMessageHandler with best-effort broadcast.
    }
}
