package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AckProcessor;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatManager;
import ch.unibas.dmi.dbis.cs108.example.chat.ChatPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link Client} class, focusing on static messaging methods,
 * configuration setters, and acknowledgement logic.
 */
public class ClientTest {

    private Client client;

    @BeforeEach
    void setup() throws Exception {
        // Reset singleton instance
        Field instanceField = Client.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Reset SERVER_ADDRESS and SERVER_PORT defaults
        Client.SERVER_ADDRESS = "localhost";
        Client.SERVER_PORT = 9876;

        client = new Client();
    }

    /**
     * Test setting and getting server address and port.
     */
    @Test
    void testServerConfiguration() {
        client.setServerAddress("example.com");
        client.setServerPort(12345);
        assertEquals("example.com", Client.SERVER_ADDRESS, "SERVER_ADDRESS should be updated");
        assertEquals(12345, Client.SERVER_PORT, "SERVER_PORT should be updated");
    }

    /**
     * Test username setter and retrieval in sendMessageStatic.
     */
    @Test
    void testSendMessageStaticUpdatesConcealedParams() {
        // Initialize username
        client.setUsername("Alice");
        // Create a message without concealed parameters
        Message msg = new Message("TYPE", new Object[]{}, "OPTION");
        // Ensure concealed initially null or default
        msg.setConcealedParameters(null);

        // Call sendMessageStatic
        Client.sendMessageStatic(msg);

        // After calling, concealed should contain username
        String[] concealed = msg.getConcealedParameters();
        assertNotNull(concealed, "Concealed parameters should not be null");
        assertEquals(1, concealed.length, "Should have one concealed element");
        assertEquals("Alice", concealed[0], "Concealed username should be Alice");
    }

    /**
     * Test sendMessageBestEffort sends a UDP packet with encoded message.
     */
    @Test
    void testSendMessageBestEffortSendsUdpPacket() throws Exception {
        // Mock the DatagramSocket
        DatagramSocket socketMock = mock(DatagramSocket.class);
        // Inject mock socket into client instance
        Field socketField = Client.class.getDeclaredField("clientSocket");
        socketField.setAccessible(true);
        socketField.set(client, socketMock);

        // Set instance to our client
        Field instanceField = Client.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, client);

        // Prepare message
        Message msg = new Message("TEST", new Object[]{"param"}, "GAME");
        msg.setConcealedParameters(new String[]{"concealed"});

        // Call best-effort send
        Client.sendMessageBestEffort(msg);

        // Capture sent packet
        ArgumentCaptor<DatagramPacket> captor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socketMock, times(1)).send(captor.capture());
        DatagramPacket packet = captor.getValue();

        // Verify packet data contains encoded message
        String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        assertTrue(data.contains("TEST"), "Packet data should contain message type");
        assertTrue(data.contains("param"), "Packet data should contain parameter");
    }

    /**
     * Test acknowledge() delegates to ackProcessor.addAck with correct address and UUID.
     */
    @Test
    void testAcknowledgeDelegatesToAckProcessor() throws Exception {
        // Prepare mock AckProcessor
        AckProcessor ackMock = mock(AckProcessor.class);
        // Inject into client
        Field ackField = Client.class.getDeclaredField("ackProcessor");
        ackField.setAccessible(true);
        ackField.set(client, ackMock);

        // Set instance
        Field instanceField = Client.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, client);

        // Prepare a message with parameters = [uuid]
        String uuid = UUID.randomUUID().toString();
        Message msg = new Message("ACK", new Object[]{uuid}, null);

        // Call acknowledge
        Client.acknowledge(msg);

        // Verify addAck called with correct address and uuid
        ArgumentCaptor<InetSocketAddress> addrCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
        verify(ackMock, times(1)).addAck(addrCaptor.capture(), eq(uuid));
        InetSocketAddress dest = addrCaptor.getValue();
        assertEquals(Client.SERVER_PORT, dest.getPort(), "Port should match SERVER_PORT");
        assertEquals(Client.SERVER_ADDRESS, dest.getAddress().getHostName(), "Host should match SERVER_ADDRESS");
    }

    @Test
    void testUpdateLocalClientStateChangeUsername() throws Exception {
        Client spy = spy(new Client());
        // inject spy as singleton
        Field inst = Client.class.getDeclaredField("instance");
        inst.setAccessible(true);
        inst.set(null, spy);

        // prepare message
        Message msg = new Message("CHANGE_USERNAME", new Object[]{"Bob"}, "RESPONSE");
        // call private updateLocalClientState
        Method m = Client.class.getDeclaredMethod("updateLocalClientState", Message.class);
        m.setAccessible(true);
        m.invoke(spy, msg);

        assertEquals("Bob", spy.getUsername().get());
    }

    @Test
    void testUpdateLocalClientStateFastLogin() throws Exception {
        Client spy = spy(new Client());
        doNothing().when(spy).login();
        Field inst = Client.class.getDeclaredField("instance");
        inst.setAccessible(true);
        inst.set(null, spy);

        Message msg = new Message("FAST_LOGIN", new Object[]{}, "RESPONSE");
        Method m = Client.class.getDeclaredMethod("updateLocalClientState", Message.class);
        m.setAccessible(true);
        m.invoke(spy, msg);

        verify(spy, times(1)).login();
    }


    @Test
    void testProcessServerResponseCreateGame() throws Exception {
        Client spy = new Client();
        Field inst = Client.class.getDeclaredField("instance");
        inst.setAccessible(true);
        inst.set(null, spy);

        Message msg = new Message("CREATEGAME",
                new Object[]{"idX","NameX"}, "RESPONSE");
        Method m = Client.class.getDeclaredMethod("processServerResponse", Message.class);
        m.setAccessible(true);
        m.invoke(spy, msg);

        assertNotNull(spy.getGame());
        assertEquals("idX", spy.getGame().getGameId());
        assertEquals("NameX", spy.getGame().getGameName());
    }


    @Test
    void testProcessServerResponseLogout() throws Exception {
        MockedStatic<Client> staticClient = Mockito.mockStatic(Client.class);
        Client spy = new Client();
        Field inst = Client.class.getDeclaredField("instance");
        inst.setAccessible(true);
        inst.set(null, spy);

        Message msg = new Message("LOGOUT", new Object[]{"Bob"}, "RESPONSE");
        Method m = Client.class.getDeclaredMethod("processServerResponse", Message.class);
        m.setAccessible(true);
        m.invoke(spy, msg);

        // Verify DELETE message constructed and sendMessageStatic() called
        staticClient.verify(() -> Client.sendMessageStatic(argThat(deleteMsg ->
                deleteMsg.getMessageType().equals("DELETE")
                        && deleteMsg.getParameters()[0].equals("Bob")
        )), times(1));
        staticClient.close();
    }


    @Test
    void testGetChatPanelDelegation() {
        Client spy = new Client();
        ChatPanel panel = new ChatPanel();
        ChatManager.ClientChatManager mgr = mock(ChatManager.ClientChatManager.class);
        when(mgr.getChatPanel()).thenReturn(panel);
        spy.clientChatManager = mgr;

        assertSame(panel, spy.getChatPanel());
    }

}
