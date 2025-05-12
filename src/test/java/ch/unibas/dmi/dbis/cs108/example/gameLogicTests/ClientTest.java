package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AckProcessor;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.ReliableUDPSender;
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
        // reset the singleton
        Field instanceField = Client.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // defaults
        Client.SERVER_ADDRESS = "localhost";
        Client.SERVER_PORT    = 9876;

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
     * Test that sendMessageBestEffort(...) actually sends a DatagramPacket
     * when the ReliableUDPSender backlog is zero.
     */
    @Test
    void testSendMessageBestEffortSendsUdpPacket() throws Exception {
        // arrange: mock the socket and inject it
        DatagramSocket socketMock = mock(DatagramSocket.class);
        Field socketField = Client.class.getDeclaredField("clientSocket");
        socketField.setAccessible(true);
        socketField.set(client, socketMock);

        // arrange: mock the ReliableUDPSender to report no backlog
        ReliableUDPSender senderMock = mock(ReliableUDPSender.class);
        when(senderMock.hasBacklog()).thenReturn(0);
        Field senderField = Client.class.getDeclaredField("myReliableUDPSender");
        senderField.setAccessible(true);
        senderField.set(client, senderMock);

        // make this instance the singleton
        Field instanceField = Client.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, client);

        // prepare a test message
        Message msg = new Message("TEST", new Object[]{"param"}, "GAME");
        msg.setConcealedParameters(new String[]{"concealed"});

        // act
        Client.sendMessageBestEffort(msg);

        // assert: packet was sent exactly once
        @SuppressWarnings("unchecked")
        ArgumentCaptor<DatagramPacket> captor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socketMock, times(1)).send(captor.capture());

        DatagramPacket packet = captor.getValue();
        String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        assertTrue(data.contains("TEST"),  "Packet data should contain the message type");
        assertTrue(data.contains("param"), "Packet data should contain the parameter");
    }
    /**
     * Since acknowledge() no longer calls AckProcessor.addAck(...),
     * verify that invoking acknowledge() produces zero interactions
     * on the injected AckProcessor mock.
     */
    @Test
    void testAcknowledgeDoesNotDelegateToAckProcessor() throws Exception {
        // arrange: inject a mock AckProcessor
        AckProcessor ackMock = mock(AckProcessor.class);
        Field ackField = Client.class.getDeclaredField("ackProcessor");
        ackField.setAccessible(true);
        ackField.set(client, ackMock);

        // make this instance the singleton
        Field instanceField = Client.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, client);

        // prepare an ACK message
        String uuid = UUID.randomUUID().toString();
        Message msg = new Message("ACK", new Object[]{uuid}, null);

        // act
        Client.acknowledge(msg);

        // assert: ackProcessor.addAck(...) was never called
        verifyNoInteractions(ackMock);
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

}
