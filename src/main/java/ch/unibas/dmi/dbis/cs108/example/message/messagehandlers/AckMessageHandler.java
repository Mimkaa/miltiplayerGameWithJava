package ch.unibas.dmi.dbis.cs108.example.message.messagehandlers;


import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.message.MessageHandler;

import java.net.InetSocketAddress;

public class AckMessageHandler implements MessageHandler {

    public AckMessageHandler() {
        // Wichtig für Reflections: no-arg Constructor
    }

    @Override
    public void handle(Server server, Message msg, InetSocketAddress senderSocket) {
        // 1) ACK-UUID extrahieren
        String ackUuid = msg.getParameters()[0].toString();
        // 2) Dem ReliableSender signalisieren
        server.getReliableSender().acknowledge(ackUuid);
        System.out.println("Processed ACK message for UUID " + msg.getUUID());
    }
}