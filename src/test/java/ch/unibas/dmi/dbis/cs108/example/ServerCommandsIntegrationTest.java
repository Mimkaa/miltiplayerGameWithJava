package ch.unibas.dmi.dbis.cs108.example;



import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

    public class ServerCommandsIntegrationTest {

        private static Server server;
        private static Thread serverThread;
        private static AtomicBoolean serverRunning = new AtomicBoolean(false);

        @BeforeAll
        public static void setUpClass() throws Exception {
            // 1) Create and start the server in a background thread
            server = Server.getInstance();
            serverThread = new Thread(() -> {
                serverRunning.set(true);
                server.start(); // blocks until closed or error
                serverRunning.set(false);
            }, "ServerMainThread");
            serverThread.start();

            // Wait a little bit to ensure server has started
            Thread.sleep(500);
        }

        @AfterAll
        public static void tearDownClass() throws Exception {
            // In your real code, you might have a method to stop the server gracefully.
            // For now, we can just interrupt the server thread.
            if (serverRunning.get()) {
                serverThread.interrupt();
            }
        }

        @Test
        public void testAllCommands() throws Exception {
            // 2) Create a client instance
            //    We’ll use the same Client class from your code, or you can do raw DatagramSocket
            Client client = new Client();
            // By default, the client picks a random username. Let's override it:
            client.setUsername("testUser");

            // 3) Start the client in a background thread so it can listen for responses
            Thread clientThread = new Thread(() -> {
                client.run(); // blocks indefinitely
            }, "ClientThread");
            clientThread.start();

            // Wait briefly for the client to get set up
            Thread.sleep(500);

            // Now we send each command in turn:

            // ========== CREATE ==========
            String createStr = "CREATE {REQUEST}[S:Player, S:MyPlayer, F:100.0, F:200.0, F:25.0, S:MyGameSession]||";
            Message createMsg = MessageCodec.decode(createStr);
            Client.sendMessageStatic(createMsg);
            System.out.println("Sent CREATE command.");

            // ========== PING ==========
            String pingStr = "PING {REQUEST}[]||";
            Message pingMsg = MessageCodec.decode(pingStr);
            Client.sendMessageStatic(pingMsg);
            System.out.println("Sent PING command.");

            // ========== GETOBJECTID ==========
            // This might fail if 'myGameInstance' is null, but let's test it anyway:
            String getObjectIdStr = "GETOBJECTID {REQUEST}[S:MyPlayer]||";
            Message getObjectIdMsg = MessageCodec.decode(getObjectIdStr);
            Client.sendMessageStatic(getObjectIdMsg);
            System.out.println("Sent GETOBJECTID command.");

            // ========== CHANGENAME ==========
            // We’ll attempt to rename "OldName" to "NewName" (though OldName might not exist).
            String changeNameStr = "CHANGENAME {REQUEST}[S:OldName, S:NewName]||";
            Message changeNameMsg = MessageCodec.decode(changeNameStr);
            Client.sendMessageStatic(changeNameMsg);
            System.out.println("Sent CHANGENAME command.");

            // ========== USERJOINED ==========
            String userJoinedStr = "USERJOINED {REQUEST}[S:MyNickname]||";
            Message userJoinedMsg = MessageCodec.decode(userJoinedStr);
            Client.sendMessageStatic(userJoinedMsg);
            System.out.println("Sent USERJOINED command.");

            // ========== LOGOUT ==========
            String logoutStr = "LOGOUT {REQUEST}[S:MyNickname]||";
            Message logoutMsg = MessageCodec.decode(logoutStr);
            Client.sendMessageStatic(logoutMsg);
            System.out.println("Sent LOGOUT command.");

            // ========== DELETE ==========
            String deleteStr = "DELETE {REQUEST}[S:MyNickname]||";
            Message deleteMsg = MessageCodec.decode(deleteStr);
            Client.sendMessageStatic(deleteMsg);
            System.out.println("Sent DELETE command.");

            // ========== EXIT ==========
            String exitStr = "EXIT {REQUEST}[S:MyNickname]||";
            Message exitMsg = MessageCodec.decode(exitStr);
            Client.sendMessageStatic(exitMsg);
            System.out.println("Sent EXIT command.");

            // ========== LOGIN ==========
            // This might do nothing if myGameInstance is null, but let's see if the server logs it:
            String loginStr = "LOGIN {REQUEST}[S:MyPlayerName]||";
            Message loginMsg = MessageCodec.decode(loginStr);
            Client.sendMessageStatic(loginMsg);
            System.out.println("Sent LOGIN command.");

            // ========== CREATEGAME ==========
            String createGameStr = "CREATEGAME {REQUEST}[S:MyNewGameName]||";
            Message createGameMsg = MessageCodec.decode(createGameStr);
            Client.sendMessageStatic(createGameMsg);
            System.out.println("Sent CREATEGAME command.");

            // Wait a bit for all commands to process
            Thread.sleep(1000);

            // 4) Edge Cases
            //    e.g. re-sending "USERJOINED" with the same nickname, or
            //    "CHANGENAME" to a name that’s already used, etc.

            String userJoinedCollision = "USERJOINED {REQUEST}[S:testUser]||";
            // testUser is the username we gave the client
            Message userJoinedCollisionMsg = MessageCodec.decode(userJoinedCollision);
            Client.sendMessageStatic(userJoinedCollisionMsg);
            System.out.println("Sent USERJOINED command with existing nickname.");

            Thread.sleep(1000);

            // In a real test, you might parse the server logs or maintain an in-memory
            // list of responses to do actual assertions. Here, we’re just demonstrating
            // the flow of sending commands.

            // 5) Cleanup: If you want to stop the client
            clientThread.interrupt();
            // The server is torn down in @AfterAll or we can do it here if we want.

            // 6) Assertions (Optional)
            // We might do real checks like:
            // assertTrue(server.getClientsMap().isEmpty(), "All clients should be logged out after EXIT");
            // ...
        }
    }

