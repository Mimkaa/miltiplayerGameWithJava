package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.net.*;
import java.util.UUID;

/**
 * The {@code ServerCommandTests} class sends various command-oriented
 * {@link Message} objects to the running {@link Server} and prints
 * them for inspection. These tests confirm that the server starts,
 * accepts UDP packets, and logs each command. They do not assert
 * server-side responses but serve as an integration-level check
 * for message dispatch.
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
     *
     * @throws Exception if socket creation or server start fails
     */
    @BeforeAll
    static void setup() throws Exception {
        clientSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");
        Server.getInstance().start(); // Starts the server, which in turn initializes CommandRegistry
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
        try {
            Object[] params = msg.getParameters();
            if (params != null && params.length > 0) {
                for (Object param : params) {
                    System.out.println("  - " + param);
                }
            } else {
                System.out.println("  (none)");
            }
        } catch (NoSuchMethodError e) {
            // Fallback to reflection if getParameters() doesn't exist
            Object paramsObj = getFieldValue(msg, "parameters");
            if (paramsObj instanceof Object[]) {
                Object[] params = (Object[]) paramsObj;
                if (params != null && params.length > 0) {
                    for (Object param : params) {
                        System.out.println("  - " + param);
                    }
                } else {
                    System.out.println("  (none)");
                }
            } else {
                System.out.println("  (none)");
            }
        }

        System.out.println("Context   :");
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
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("  (not available)");
        }

        System.out.println("Encoded   : " + encoded);
        System.out.println("====================\n");
    }

    /**
     * Sends a "CREATE" command request to the server, instructing it
     * to create a new game object named "TestPlayer" with position data, etc.
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Sends a "PING" command request, expecting the server to respond
     * with "PONG" (though this test does not explicitly assert the response).
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Requests the UUID of a game object named "TestPlayer" using "GETOBJECTID".
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Tests renaming a game object from "TestPlayer" to "NewTestPlayer"
     * via the "CHANGENAME" command.
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Tests simulating a user joining via "USERJOINED".
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Sends a "LOGOUT" command for "testUser", telling the server
     * to remove or log out that user.
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Sends a "DELETE" command for "testUser", instructing the server
     * to remove a game object or reference to that user.
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Sends an "EXIT" command for "testUser", fully terminating
     * the client session.
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Tests the "LOGIN" command, asking the server to confirm login
     * and respond with an object's ID if relevant.
     *
     * @throws Exception if sending fails
     */
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

    /**
     * Tests "CREATEGAME", which instructs the server to create a new
     * game session named "MyNewGameSession".
     *
     * @throws Exception if sending fails
     */
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
}
