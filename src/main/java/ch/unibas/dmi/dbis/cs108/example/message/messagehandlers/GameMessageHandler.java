package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * Handles messages with {@code msg.getOption() == "GAME"}, allowing
 * best-effort broadcast to other clients. This is useful for
 * real-time game updates that don't require guaranteed delivery.
 */
public class GameMessageHandler implements MessageHandler {

    /**
     * Required no-arg constructor for reflection-based discovery.
     */
    public GameMessageHandler() {
        // ...
    }

    /**
     * Adds the message to the main game's queue, then triggers a best-effort
     * broadcast to all other clients.
     *
     * @param server       the server instance
     * @param msg          the "GAME" option message
     * @param senderSocket the network address of the sending client
     */
    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        server.getMyGameInstance().addIncomingMessage(msg);

        // Possibly do non-reliable broadcast
        AsyncManager.run(() -> broadcastBestEffort(server, msg, senderSocket));
    }

    /**
     * Sends the given {@link Message} to all clients except the sender, without
     * using the reliable sending mechanism.
     */
    private void broadcastBestEffort(Server server, Message msg, InetSocketAddress senderSocket) {
        for (InetSocketAddress target : server.getClientsMap().values()) {
            if (!target.equals(senderSocket)) {
                try {
                    String encoded = MessageCodec.encode(msg);
                    byte[] data = encoded.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length,
                            target.getAddress(), target.getPort());
                    server.getServerSocket().send(packet);
                    System.out.println("Best effort sent message to " + target);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
