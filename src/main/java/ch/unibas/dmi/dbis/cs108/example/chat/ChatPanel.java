package ch.unibas.dmi.dbis.cs108.example.chat;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 * {@code ChatPanel} is a singleton UI component for displaying and sending chat messages.
 * It provides a text area for incoming/outgoing messages and a field for user input.
 */
public class ChatPanel extends JPanel {
    /**
     * The singleton instance of {@code ChatPanel}.
     */
    private static ChatPanel instance; // singleton instance

    /**
     * Displays the chat messages.
     */
    private JTextPane chatPane;

    /**
     * Field where users can type messages.
     */
    private JTextField inputField;

    /**
     * Label used to show typing status (e.g., "User is typing...").
     */
    private JLabel typingIndicatorLabel;

    /**
     * The styled document associated with {@link #chatPane}.
     */
    private StyledDocument doc;

    /**
     * Style for locally sent messages.
     */
    private SimpleAttributeSet localStyle;

    /**
     * Style for remotely sent messages.
     */
    private SimpleAttributeSet remoteStyle;

    /**
     * Listener for handling chat events (sending messages, typing).
     */
    private ChatPanelListener listener;

    /**
     * Private constructor that initializes the chat UI.
     * Prevents external instantiation to maintain a singleton design.
     *
     * @param listener The listener for handling chat events (message sending, typing notifications).
     */
    private ChatPanel(ChatPanelListener listener) {
        this.listener = listener;
        initUI();
        //System.out.println("ChatPanel created, instance hashCode: " + this.hashCode());
    }

    /**
     * Returns the singleton instance of {@code ChatPanel}. If it hasn't been created yet,
     * it will be created with the specified listener. If it has already been created,
     * subsequent calls will return the existing instance (ignoring any new listener passed in).
     *
     * @param listener The {@link ChatPanelListener} to use on initial creation.
     * @return The singleton {@code ChatPanel} instance.
     */
    public static synchronized ChatPanel getInstance(ChatPanelListener listener) {
        if (instance == null) {
            instance = new ChatPanel(listener);
        }
        return instance;
    }

    /**
     * Updates the {@link ChatPanelListener} for this chat panel.
     *
     * @param listener The new listener to handle chat events.
     */
    public void setChatPanelListener(ChatPanelListener listener) {
        this.listener = listener;
    }

    /**
     * Initializes the user interface components for the chat panel.
     * This includes:
     * <ul>
     *   <li>A non-editable {@link JTextPane} for displaying chat messages.</li>
     *   <li>A {@link JTextField} for user input.</li>
     *   <li>A {@link JLabel} for showing typing indicators.</li>
     * </ul>
     */
    private void initUI() {
        setLayout(new BorderLayout());

        // Chat display area
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        localStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(localStyle, Color.BLUE);
        remoteStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(remoteStyle, Color.BLACK);
        JScrollPane scrollPane = new JScrollPane(chatPane);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel: contains a typing indicator and an input field.
        JPanel inputPanel = new JPanel(new BorderLayout());
        typingIndicatorLabel = new JLabel(" ");
        typingIndicatorLabel.setForeground(Color.GRAY);
        inputPanel.add(typingIndicatorLabel, BorderLayout.NORTH);

        inputField = new JTextField();
        inputField.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty() && listener != null) {
                listener.onChatMessage(text);
                inputField.setText("");
            }
        });
        inputField.addKeyListener(new KeyAdapter() {
            private long lastTypingTime = 0;

            @Override
            public void keyPressed(KeyEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastTypingTime > 3000) { // Throttle typing notifications
                    if (listener != null) {
                        listener.onTyping();
                    }
                    lastTypingTime = now;
                }
            }
        });
        inputPanel.add(inputField, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * Appends a new message to the chat pane with a timestamp.
     *
     * @param message  The message text to display.
     * @param isLocal  {@code true} if the message was sent locally (uses a different text color).
     */
    public void appendMessage(String message, boolean isLocal) {
        SimpleAttributeSet style = isLocal ? localStyle : remoteStyle;
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String fullMessage = "[" + timeStamp + "] " + message + "\n";
        System.out.println("Appending message: " + fullMessage);
        try {
            doc.insertString(doc.getLength(), fullMessage, style);
            chatPane.setCaretPosition(doc.getLength());
            chatPane.revalidate();
            chatPane.repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a typing indicator, usually invoked when another user is in the process of typing.
     *
     * @param sender The name of the sender who is typing.
     */
    public void showTypingIndicator(String sender) {
        typingIndicatorLabel.setText(sender + " is typing...");
    }

    /**
     * Clears the typing indicator, typically after the user has finished typing or after a timeout.
     */
    public void clearTypingIndicator() {
        typingIndicatorLabel.setText(" ");
    }

    /**
     * Gets the {@link JTextField} used for user input in this chat panel.
     *
     * @return The {@link JTextField} used for message input.
     */
    public JTextField getInputField() {
        return inputField;
    }

    /**
     * The listener interface for handling chat-related events, such as sending messages
     * and indicating that a user is typing.
     */
    public interface ChatPanelListener {
        /**
         * Called when the user sends a new chat message.
         *
         * @param message The message text that was sent.
         */
        void onChatMessage(String message);

        /**
         * Called when the user starts typing (or continues typing after a specified interval).
         */
        void onTyping();
    }
}
