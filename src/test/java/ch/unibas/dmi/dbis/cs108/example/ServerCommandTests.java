package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

/**
 * The {@code ServerCommandTests} class sends various command-oriented
 * {@link Message} objects to the running {@link Server} and prints
 * them for inspection. This includes original commands like CREATE,
 * PING, etc., as well as newly added commands like CREATEGO, DELETEGO,
 * JOINGAME, and SELECTGO.
 * <p>
 * The test only demonstrates sending the commands; it does not do
 * extensive assertion of server responses. Logs are displayed
 * so you can confirm the server processes each message.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerCommandTests {

    /** The UDP socket for sending messages to the server. */
    private static DatagramSocket clientSocket;

    /** The {@link InetAddress} of the server, typically localhost. */
    private static InetAddress serverAddress;

    /** The UDP port on which the server is running. */
    private static final int SERVER_PORT = 9876;

    /**
     * Sets up the client socket, obtains the server address,
     * and starts the {@link Server} singleton. Waits briefly
     * for the server to initialize fully.
     */
    @BeforeAll
    static void setup() throws Exception {
        clientSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");
        Server.getInstance().start();
        Thread.sleep(1000); // Allow the server some time to finish starting up
    }

    /**
     * Closes the client socket after all tests have completed.
     */
    @AfterAll
    static void tearDown() {
        clientSocket.close();
    }

    /**
     * Retrieves a private field from the given object by reflection.
     * If the field is not found or inaccessible, returns null.
     *
     * @param obj       the object from which to extract the field value
     * @param fieldName the name of the private field
     * @return the field value, or null if not found or not accessible
     */
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Encodes the given message, sends it via UDP to the server,
     * and prints out the message details for readability.
     *
     * @param msg the {@link Message} to send
     * @throws Exception if a socket error occurs
     */
    private void sendMessageAndPrint(Message msg) throws Exception {
        // 1) Encode and send
        String encoded = MessageCodec.encode(msg);
        byte[] data = encoded.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        clientSocket.send(packet);

        // 2) Print for readability
        System.out.println("\n=== Sent Message ===");
        System.out.println("Command : " + getFieldValue(msg, "command"));
        System.out.println("Type    : " + getFieldValue(msg, "type"));

        System.out.println("Parameters:");
        Object[] params;
        try {
            params = msg.getParameters();
        } catch (NoSuchMethodError e) {
            // fallback to reflection
            params = (Object[]) getFieldValue(msg, "parameters");
        }
        if (params != null && params.length > 0) {
            for (Object p : params) {
                System.out.println("  - " + p);
            }
        } else {
            System.out.println("  (none)");
        }

        System.out.println("Context:");
        try {
            Field contextField = msg.getClass().getDeclaredField("context");
            contextField.setAccessible(true);
            String[] context = (String[]) contextField.get(msg);
            if (context != null && context.length > 0) {
                for (String ctx : context) {
                    System.out.println("  - " + ctx);
                }
            } else {
                System.out.println("  (none)");
            }
        } catch (Exception e) {
            System.out.println("  (not available)");
        }

        System.out.println("Encoded : " + encoded);
        System.out.println("====================\n");
    }

    // ================================
    // Original 10 Tests
    // ================================

    @Test
    @Order(1)
    void testCreateCommand() throws Exception {
        Message msg = new Message(
                "CREATE",
                new Object[]{"Player", "TestPlayer", 100.0f, 200.0f, 25.0f, "TestGame"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(2)
    void testPingCommand() throws Exception {
        Message msg = new Message(
                "PING",
                new Object[]{},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(3)
    void testGetObjectIdCommand() throws Exception {
        Message msg = new Message(
                "GETOBJECTID",
                new Object[]{"TestPlayer"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(4)
    void testChangeNameCommand() throws Exception {
        Message msg = new Message(
                "CHANGENAME",
                new Object[]{"TestPlayer", "NewTestPlayer"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(5)
    void testUserJoinedCommand() throws Exception {
        Message msg = new Message(
                "USERJOINED",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(6)
    void testLogoutCommand() throws Exception {
        Message msg = new Message(
                "LOGOUT",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(7)
    void testDeleteCommand() throws Exception {
        Message msg = new Message(
                "DELETE",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(8)
    void testExitCommand() throws Exception {
        Message msg = new Message(
                "EXIT",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(9)
    void testLoginCommand() throws Exception {
        Message msg = new Message(
                "LOGIN",
                new Object[]{"NewTestPlayer"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(10)
    void testCreateGameCommand() throws Exception {
        Message msg = new Message(
                "CREATEGAME",
                new Object[]{"MyNewGameSession"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    // ================================
    // New Tests for CREATEGO, DELETEGO, JOINGAME, SELECTGO
    // ================================

    /**
     * Tests creating a game object within a specified session
     * using the "CREATEGO" command.
     */
    @Test
    @Order(11)
    void testCreateGoCommand() throws Exception {
        // e.g., param 0 = session ID, param 1 = object type
        Message msg = new Message(
                "CREATEGO",
                new Object[]{"sessionXYZ", "Player", 100.0f, 200.0f, "SomeOtherParam"},
                "REQUEST",
                new String[]{"session1", "gameXYZ", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    /**
     * Tests deleting a game object from a session using "DELETEGO".
     */
    @Test
    @Order(12)
    void testDeleteGoCommand() throws Exception {
        // param 0 = session ID, param 1 = object ID
        Message msg = new Message(
                "DELETEGO",
                new Object[]{"sessionXYZ", "someUUID"},
                "REQUEST",
                new String[]{"session1", "gameXYZ", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    /**
     * Tests joining a game by name using "JOINGAME".
     */
    @Test
    @Order(13)
    void testJoinGameCommand() throws Exception {
        Message msg = new Message(
                "JOINGAME",
                new Object[]{"MyExistingGameName"},
                "REQUEST",
                new String[]{"session1", "gameXYZ", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }

    /**
     * Tests selecting a specific game object by name in a session
     * using "SELECTGO".
     */
    @Test
    @Order(14)
    void testSelectGoCommand() throws Exception {
        // param 0 = session ID, param 1 = object name
        Message msg = new Message(
                "SELECTGO",
                new Object[]{"sessionXYZ", "PlayerName123"},
                "REQUEST",
                new String[]{"session1", "gameXYZ", "testUser", UUID.randomUUID().toString()}
        );
        sendMessageAndPrint(msg);
    }
}
