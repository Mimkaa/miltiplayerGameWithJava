package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * If you want special logic for messages that have msg.getOption() == "GAME"
 */
public class GameMessageHandler implements MessageHandler {

    public GameMessageHandler() {
    }

    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // Falls du "GAME" in alten processMessage abgefragt hast
        // Hier kannst du z.B. in die myGameInstance was legen
        server.getMyGameInstance().addIncomingMessage(msg);

        // Evtl. "bestEffort" broadcasting:
        AsyncManager.run(() -> broadcastBestEffort(server, msg, senderSocket));
    }

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