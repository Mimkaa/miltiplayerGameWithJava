package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.net.*;
import java.util.UUID;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerCommandTests {

    private static DatagramSocket clientSocket;
    private static InetAddress serverAddress;
    private static final int SERVER_PORT = 9876;

    @BeforeAll
    static void setup() throws Exception {
        clientSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");
        Server.getInstance().start();
        Thread.sleep(1000); // Wait for server to boot
    }

    @AfterAll
    static void tearDown() {
        clientSocket.close();
    }

    // Helper method to extract a field value via reflection.
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendMessageAndPrint(Message msg) throws Exception {
        // Encode and send the message.
        String encoded = MessageCodec.encode(msg);
        byte[] data = encoded.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        clientSocket.send(packet);

        // Print a formatted output for better readability.
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
            // Fallback to reflection if getParameters() is not available.
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

    @Test
    @Order(1)
    void testCreateCommand() throws Exception {
        Message msg = new Message("CREATE",
                new Object[]{"Player", "TestPlayer", 100.0f, 200.0f, 25.0f, "TestGame"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(2)
    void testPingCommand() throws Exception {
        Message msg = new Message("PING",
                new Object[]{},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(3)
    void testGetObjectIdCommand() throws Exception {
        Message msg = new Message("GETOBJECTID",
                new Object[]{"TestPlayer"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(4)
    void testChangeNameCommand() throws Exception {
        Message msg = new Message("CHANGENAME",
                new Object[]{"TestPlayer", "NewTestPlayer"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(5)
    void testUserJoinedCommand() throws Exception {
        Message msg = new Message("USERJOINED",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(6)
    void testLogoutCommand() throws Exception {
        Message msg = new Message("LOGOUT",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(7)
    void testDeleteCommand() throws Exception {
        Message msg = new Message("DELETE",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(8)
    void testExitCommand() throws Exception {
        Message msg = new Message("EXIT",
                new Object[]{"testUser"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(9)
    void testLoginCommand() throws Exception {
        Message msg = new Message("LOGIN",
                new Object[]{"NewTestPlayer"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(10)
    void testCreateGameCommand() throws Exception {
        Message msg = new Message("CREATEGAME",
                new Object[]{"MyNewGameSession"},
                "REQUEST",
                new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }
}
