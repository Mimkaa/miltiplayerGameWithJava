package chat;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ChatManager {

    /**
     * Client-side ChatManager that encapsulates chat UI and message processing.
     */
    public static class ClientChatManager {
        private ChatPanel chatPanel;
        private AtomicReference username;
        private String gameName;
        private ConcurrentLinkedQueue<Message> outgoingQueue;

        public ClientChatManager(AtomicReference username, String gameName, ConcurrentLinkedQueue<Message> outgoingQueue, String uuid) {
            this.username = username;
            this.gameName = gameName;
            this.outgoingQueue = outgoingQueue;
            // Create the chat panel with a listener that sends messages and notifies typing.
            chatPanel = new ChatPanel(new ChatPanel.ChatPanelListener() {
                @Override
                public void onChatMessage(String message) {
                    sendChatMessage(message, uuid);
                }
                @Override
                public void onTyping() {
                    // Optionally, you can implement typing notifications here.
                    System.out.println(username + " is typing...");
                }
            });
        }

        public ChatPanel getChatPanel() {
            return chatPanel;
        }

        /**
         * Wraps the given text into a CHAT message and enqueues it.
         * The concealed parameters include the sender's username and the game name.
         */
        public void sendChatMessage(String text, String uuid) {
            String usernameString = username.toString();
            Message chatMsg = new Message("CHAT", new Object[]{ text }, null, new String[]{ usernameString, gameName }, uuid);
            outgoingQueue.offer(chatMsg);
        }

        /**
         * Processes an incoming CHAT message and displays it in the chat panel.
         */
        public void processIncomingChatMessage(Message msg) {
            Object[] params = msg.getParameters();
            String chatText = (params != null && params.length > 0) ? params[0].toString() : "";
            // Expect the first concealed parameter to be the sender's name.
            String senderName = (msg.getConcealedParameters() != null && msg.getConcealedParameters().length > 0)
                    ? msg.getConcealedParameters()[0] : "Unknown";
            boolean isLocal = senderName.equals(username);
            String fullMessage = senderName + ": " + chatText;
            SwingUtilities.invokeLater(() -> chatPanel.appendMessage(fullMessage, isLocal));
        }
    }

    /**
     * Server-side ChatManager that handles chat messages on the server.
     */
    public static class ServerChatManager {


        /**
         * Processes an incoming CHAT message:
         * 1. Broadcasts it to all clients.
         * 2. Sends an ACK back to the sender.
         */
        public void handleChatMessage(Message msg, InetSocketAddress senderSocket, String uuid) {
            Server server = Server.getInstance();
            // Broadcast the chat message to all clients.
            server.broadcastMessageToAll(msg);
            // Send an ACK to the sender to prevent repeated retransmission.
            System.out.println("Processed CHAT message 66 ");
            if (uuid != null && !uuid.isEmpty()) {
                System.out.println("Processed CHAT message 55wqewe ");
                server.getAckProcessor().addAck(senderSocket, uuid);
                System.out.println("Processed CHAT message 55 ");
                Message ackMsg = new Message("ACK", new Object[]{ uuid }, null);
                server.getReliableSender().sendMessage(ackMsg, senderSocket.getAddress(), senderSocket.getPort());
                System.out.println("Processed CHAT message 555 ");
            }
        }
    }


}
