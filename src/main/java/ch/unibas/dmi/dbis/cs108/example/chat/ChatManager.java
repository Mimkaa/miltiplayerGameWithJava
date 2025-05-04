package ch.unibas.dmi.dbis.cs108.example.chat;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AckProcessor;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code ChatManager} serves as a container for both the client-side and server-side
 * chat management logic. It includes nested classes for handling chat messages on the
 * client and the server.
 */
public class ChatManager {

    /**
     * Client-side ChatManager that encapsulates chat UI and message processing.
     */
    public static class ClientChatManager {
        /**
         * The panel managing chat UI (singleton instance).
         */
        private ChatPanel chatPanel;

        /**
         * The username wrapped in an {@link AtomicReference}, allowing thread-safe updates.
         */
        private AtomicReference<String> username;

        /**
         * The name of the game or session to which this chat belongs.
         */
        private String gameName;

        /**
         * A queue of outgoing {@link Message} objects to be sent from the client.
         */
        private ConcurrentLinkedQueue<Message> outgoingQueue;

        /**
         * Constructs a new {@code ClientChatManager} to handle chat functions on the client side.
         *
         * @param username       An {@link AtomicReference} containing the client user's name.
         * @param gameName       The name of the game or session for this chat.
         * @param outgoingQueue  A {@link ConcurrentLinkedQueue} of messages to be sent out.
         * @param uuid           A unique identifier (UUID) string associated with the client.
         */
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

        /**
         * Returns the current {@link ChatPanel} (singleton instance) used by this client manager.
         *
         * @return The current {@link ChatPanel}.
         */
        public ChatPanel getChatPanel() {
            return chatPanel;
        }

        /**
         * Optionally, set a new {@link ChatPanel} instance for this client manager.
         *
         * @param panel The new {@link ChatPanel} to be used.
         */
        public void setCurrentPanel(ChatPanel panel) {
            this.chatPanel = panel;
        }

        /**
         * Wraps the given text into a CHAT {@link Message} and enqueues it to be sent.
         * Instead of sending just the text as the sole parameter, this includes the sender's username.
         * The concealed parameters remain fixed.
         *
         * @param text The chat text to send.
         */
        public void sendChatMessage(String text) {
            String usernameString = username.get();
            // Include the username and text as the parameters.
            Message chatMsg = new Message("CHAT", new Object[]{usernameString, text}, "REQUEST");
            // Overwrite the concealed parameters with the fixed values.
            Client.sendMessageStatic(chatMsg);
        }

        /**
         * Processes an incoming CHAT {@link Message} and displays it in the chat panel.
         * It extracts the sender's name from {@code params[0]} and the chat text from {@code params[1]}.
         *
         * @param msg          The incoming chat message.
         * @param ackProcessor The {@link AckProcessor} used for acknowledging this message.
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
            //ackProcessor.addAck(socketAddress, msg.getUUID());
        }
    }

    /**
     * Server-side ChatManager that handles chat messages on the server.
     */
    public static class ServerChatManager {

        /**
         * Processes an incoming CHAT {@link Message} on the server:
         * <ol>
         *     <li>Broadcasts it to all clients.</li>
         *     <li>Sends an ACK back to the sender.</li>
         * </ol>
         *
         * @param msg           The incoming chat message from a client.
         * @param senderSocket  The {@link InetSocketAddress} of the message sender.
         * @param ackProcessor  The {@link AckProcessor} used for acknowledging this message.
         */
        public void handleChatMessage(Message msg, InetSocketAddress senderSocket, AckProcessor ackProcessor) {
            Server server = Server.getInstance();
            // Broadcast the chat message to all clients.
            server.broadcastMessageToAll(msg);
            System.out.println("Broadcasted Message to all Clients");

            // Send an ACK to the sender.
            //ackProcessor.addAck(senderSocket, msg.getUUID());
            System.out.println("Added message UUID " + msg.getUUID() + " to ACK handler");
        }
    }
}
