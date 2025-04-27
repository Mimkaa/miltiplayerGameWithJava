package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Server} class.
 * <p>
 * This test suite verifies:
 * <ul>
 *   <li>Singleton instance behavior</li>
 *   <li>Response message creation via {@link Server#makeResponse(Message, Object[])}</li>
 *   <li>Unique name generation logic in {@link Server#findUniqueName(String)}</li>
 *   <li>Enqueuing of outgoing messages via {@link Server#enqueueMessage(Message, InetAddress, int)}</li>
 *   <li>Synchronization of game sessions through {@link Server#syncGames(InetSocketAddress)}</li>
 *   <li>Default handling of unknown requests in {@code handleRequest(Message, String)}</li>
 * </ul>
 * </p>
 */
class ServerTest {

    private Server server;

    /**
     * Prepare a clean {@link Server} singleton before each test.
     */
    @BeforeEach
    void setUp() {
        server = Server.getInstance();
        server.getClientsMap().clear();
        resetOutgoingQueue(new LinkedBlockingQueue<>());
    }

    /**
     * Utility method to reset the private {@code outgoingQueue} via reflection.
     *
     * @param newQueue the new empty queue to install
     */
    @SuppressWarnings("unchecked")
    private void resetOutgoingQueue(LinkedBlockingQueue<?> newQueue) {
        try {
            Field qf = Server.class.getDeclaredField("outgoingQueue");
            qf.setAccessible(true);
            qf.set(server, newQueue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method to retrieve the private {@code outgoingQueue} for assertions.
     *
     * @return the outgoing message queue
     */
    @SuppressWarnings("unchecked")
    private LinkedBlockingQueue<Object> getOutgoingQueue() {
        try {
            Field qf = Server.class.getDeclaredField("outgoingQueue");
            qf.setAccessible(true);
            return (LinkedBlockingQueue<Object>) qf.get(server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures that {@link Server#getInstance()} always returns the same singleton instance.
     */
    @Test
    void testSingletonInstance() {
        assertSame(Server.getInstance(), Server.getInstance(),
                "Server.getInstance() should always return the same object");
    }

    /**
     * Verifies that {@link Server#makeResponse(Message, Object[])} produces a response
     * with the same type, "RESPONSE" option, correct parameters, and preserved concealed parameters.
     */
    @Test
    void testMakeResponse() {
        Message original = new Message("PING", new Object[]{123}, "REQUEST", new String[]{"u1"});
        Message resp = Server.makeResponse(original, new Object[]{"pong", 456});
        assertEquals("PING", resp.getMessageType());
        assertEquals("RESPONSE", resp.getOption());
        assertArrayEquals(new Object[]{"pong", 456}, resp.getParameters());
        assertArrayEquals(new String[]{"u1"}, resp.getConcealedParameters(),
                "concealedParameters should be copied from original");
    }

    /**
     * Checks that {@link Server#findUniqueName(String)} returns the same name
     * when there is no conflict.
     */
    @Test
    void testFindUniqueName_noConflict() {
        String name = "Alice";
        assertEquals("Alice", server.findUniqueName(name),
                "When nobody has taken the name, it should return it unchanged");
    }

    /**
     * Checks that {@link Server#findUniqueName(String)} appends a suffix
     * when the desired name is already taken by an existing client.
     */
    @Test
    void testFindUniqueName_withConflict() {
        server.getClientsMap().put("Bob", new InetSocketAddress("127.0.0.1", 9999));
        String unique = server.findUniqueName("Bob");
        assertTrue(unique.startsWith("Bob_"), "Should append suffix when name is taken");
        assertNotEquals("Bob", unique, "Should not return the original when it's already in use");
    }

    /**
     * Verifies that {@link Server#enqueueMessage(Message, InetAddress, int)}
     * correctly adds an {@code OutgoingMessage} to the private queue with proper fields.
     */
    @Test
    void testEnqueueMessage_and_outgoingQueue() throws Exception {
        Message msg = new Message("TEST", new Object[]{}, "OPTION");
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        int port = 1111;
        server.enqueueMessage(msg, addr, port);

        LinkedBlockingQueue<Object> q = getOutgoingQueue();
        assertEquals(1, q.size(), "enqueueMessage should add one item to the queue");

        Object om = q.peek();
        assertNotNull(om);
        Class<?> omClass = om.getClass();
        Field msgField = omClass.getDeclaredField("msg");
        Field addrField = omClass.getDeclaredField("address");
        Field portField = omClass.getDeclaredField("port");
        msgField.setAccessible(true);
        addrField.setAccessible(true);
        portField.setAccessible(true);

        assertSame(msg, msgField.get(om));
        assertEquals(addr, addrField.get(om));
        assertEquals(port, portField.get(om));
    }

    /**
     * Ensures that {@link Server#syncGames(InetSocketAddress)} enqueues one
     * CREATEGAME message for each session in the {@link GameSessionManager}.
     */
    @Test
    void testSyncGames_enqueuesCreateGameMessages() throws Exception {
        Field gmField = Server.class.getDeclaredField("gameSessionManager");
        gmField.setAccessible(true);
        GameSessionManager gm = (GameSessionManager) gmField.get(server);

        gm.addGameSession("session42", "CrazyGame");
        Game game = gm.getGameSession("session42");
        assertNotNull(game, "GameSessionManager should store the new Game");

        InetSocketAddress fakeSocket = new InetSocketAddress("127.0.0.1", 2222);
        server.syncGames(fakeSocket);

        LinkedBlockingQueue<Object> q = getOutgoingQueue();
        assertEquals(1, q.size(), "One CREATEGAME message should be enqueued");

        Object om = q.take();
        Field msgField = om.getClass().getDeclaredField("msg");
        msgField.setAccessible(true);
        Message m = (Message) msgField.get(om);

        assertEquals("CREATEGAME", m.getMessageType());
        assertArrayEquals(new Object[]{"session42", "CrazyGame"}, m.getParameters());
    }

    /**
     * Verifies that an unknown REQUEST is handled by enqueuing a default RESPONSE
     * via the private {@code handleRequest} method.
     */
    @Test
    void testHandleRequest_noHandler_defaultsResponse() throws Exception {
        server.getClientsMap().put("Eve", new InetSocketAddress("127.0.0.1", 3333));
        Message unknown = new Message("FOOBAR", new Object[]{"x"}, "REQUEST", new String[]{"Eve"});

        Method handleRequest = Server.class.getDeclaredMethod("handleRequest", Message.class, String.class);
        handleRequest.setAccessible(true);
        handleRequest.invoke(server, unknown, "Eve");

        LinkedBlockingQueue<Object> q = getOutgoingQueue();
        assertEquals(1, q.size(), "Unknown request should enqueue a default response");

        Object om = q.take();
        Field msgField = om.getClass().getDeclaredField("msg");
        msgField.setAccessible(true);
        Message resp = (Message) msgField.get(om);

        assertEquals("FOOBAR", resp.getMessageType(), "Response type should match original");
        assertEquals("RESPONSE", resp.getOption(), "Should be marked as RESPONSE");
        assertArrayEquals(new Object[]{"x"}, resp.getParameters(), "Parameters should be echoed back");
    }
}
