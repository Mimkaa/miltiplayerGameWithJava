package ch.unibas.dmi.dbis.cs108.example.chat;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AckProcessor;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ChatManager {

    /**
     * Client-side ChatManager that encapsulates chat UI and message processing.
     */
    public static class ClientChatManager {
        private ChatPanel chatPanel;
        private AtomicReference<String> username;
        private String gameName;
        private ConcurrentLinkedQueue<Message> outgoingQueue;

        public ClientChatManager(AtomicReference<String> username, String gameName,
                                 ConcurrentLinkedQueue<Message> outgoingQueue, String uuid) {
            this.username = username;
            this.gameName = gameName;
            this.outgoingQueue = outgoingQueue;
            // Use the singleton ChatPanel instance instead of creating a new one.
            chatPanel = ChatPanel.getInstance(new ChatPanel.ChatPanelListener() {
                @Override
                public void onChatMessage(String message) {
                    sendChatMessage(message);
                }
                @Override
                public void onTyping() {
                    // Optionally, you can implement typing notifications here.
                    System.out.println(username.get() + " is typing...");
                }
            });
            // For debugging, print the hash code of the singleton instance.
            System.out.println("ClientChatManager using ChatPanel instance: " + chatPanel.hashCode());
        }

        public ChatPanel getChatPanel() {
            return chatPanel;
        }

        /**
         * Optionally, set a new ChatPanel instance.
         */
        public void setCurrentPanel(ChatPanel panel) {
            this.chatPanel = panel;
        }

        /**
         * Wraps the given text into a CHAT message and enqueues it.
         * Instead of sending just the text as the sole parameter, we include the sender's username.
         * The concealed parameters remain fixed.
         */
        public void sendChatMessage(String text) {
            String usernameString = username.get();
            // Include the username and text as the parameters.
            Message chatMsg = new Message("CHAT", new Object[]{usernameString, text}, "GAME", new String[]{usernameString, gameName});
            // Overwrite the concealed parameters with the fixed values.
            chatMsg.setConcealedParameters(new String[]{"gameObject", "gameSession"});
            String encodedMessage = MessageCodec.encode(chatMsg);
            System.out.println("Encoded chat message: " + encodedMessage);
            outgoingQueue.offer(MessageCodec.decode(encodedMessage));
        }

        /**
         * Processes an incoming CHAT message and displays it in the chat panel.
         * It extracts the sender's name from parameters[0] and the chat text from parameters[1].
         */
        public void processIncomingChatMessage(Message msg, AckProcessor ackProcessor) {
            Object[] params = msg.getParameters();
            String senderName = (params != null && params.length > 0) ? params[0].toString() : "Unknown";
            String chatText   = (params != null && params.length > 1) ? params[1].toString() : "";
            boolean isLocal = senderName.equals(username.get());
            String fullMessage = senderName + ": " + chatText;
            System.out.println("Processing chat message: " + msg);
            SwingUtilities.invokeLater(() -> chatPanel.appendMessage(fullMessage, isLocal));
            InetSocketAddress socketAddress = new InetSocketAddress("localhost", 9876);
            ackProcessor.addAck(socketAddress, msg.getUUID());
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
        public void handleChatMessage(Message msg, InetSocketAddress senderSocket, AckProcessor ackProcessor) {
            Server server = Server.getInstance();
            // Broadcast the chat message to all clients.
            server.broadcastMessageToAll(msg);
            System.out.println("Broadcasted Message to all Clients");
            // Send an ACK to the sender.
            ackProcessor.addAck(senderSocket, msg.getUUID());
            System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
        }
    }
}
