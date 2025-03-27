package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import org.junit.jupiter.api.*;

import java.net.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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

    private void sendMessageAndPrint(Message msg) throws Exception {
        String encoded = MessageCodec.encode(msg);
        byte[] data = encoded.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        clientSocket.send(packet);
        System.out.println("Sent: " + encoded);
    }

    @Test
    @Order(1)
    void testCreateCommand() throws Exception {
        Message msg = new Message("CREATE", new Object[]{"Player", "TestPlayer", 100.0f, 200.0f, 25.0f, "TestGame"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(2)
    void testPingCommand() throws Exception {
        Message msg = new Message("PING", new Object[]{}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(3)
    void testGetObjectIdCommand() throws Exception {
        Message msg = new Message("GETOBJECTID", new Object[]{"TestPlayer"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(4)
    void testChangeNameCommand() throws Exception {
        Message msg = new Message("CHANGENAME", new Object[]{"TestPlayer", "NewTestPlayer"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(5)
    void testUserJoinedCommand() throws Exception {
        Message msg = new Message("USERJOINED", new Object[]{"testUser"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(6)
    void testLogoutCommand() throws Exception {
        Message msg = new Message("LOGOUT", new Object[]{"testUser"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(7)
    void testDeleteCommand() throws Exception {
        Message msg = new Message("DELETE", new Object[]{"testUser"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(8)
    void testExitCommand() throws Exception {
        Message msg = new Message("EXIT", new Object[]{"testUser"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(9)
    void testLoginCommand() throws Exception {
        Message msg = new Message("LOGIN", new Object[]{"NewTestPlayer"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }

    @Test
    @Order(10)
    void testCreateGameCommand() throws Exception {
        Message msg = new Message("CREATEGAME", new Object[]{"MyNewGameSession"}, "REQUEST", new String[]{"session1", "game1", "testUser", UUID.randomUUID().toString()});
        sendMessageAndPrint(msg);
    }
}
