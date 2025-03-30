package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.InetSocketAddress;

/**
 * Handles "ACK" messages, informing the server's reliable sender that
 * a particular UUID has been acknowledged.
 */
public class AckMessageHandler implements MessageHandler {

    /**
     * Required no-arg constructor for reflection-based discovery.
     */
    public AckMessageHandler() {
        // ...
    }

    /**
     * Extracts the ACK UUID from the message parameters and calls
     * {@code reliableSender.acknowledge(...)}. Logs the result.
     *
     * @param server       the server instance, used here to access the reliable sender
     * @param msg          the incoming "ACK" message
     * @param senderSocket the network address of the sender
     */
    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // 1) Extract the ACK UUID
        String ackUuid = msg.getParameters()[0].toString();
        // 2) Acknowledge
        server.getReliableSender().acknowledge(ackUuid);
        System.out.println("Processed ACK message for UUID " + msg.getUUID());
    }
}
