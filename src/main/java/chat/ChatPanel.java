package chat;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class ChatPanel extends JPanel {
    private JTextPane chatPane;
    private JTextField inputField;
    private JLabel typingIndicatorLabel;
    private StyledDocument doc;
    private SimpleAttributeSet localStyle;
    private SimpleAttributeSet remoteStyle;
    private ChatPanelListener listener;

    public ChatPanel(ChatPanelListener listener) {
        this.listener = listener;
        initUI();
    }

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
     * Appends a new message to the chat area with a timestamp.
     * @param message the message text to display.
     * @param isLocal true if the message was sent locally (uses a different style).
     */
    public void appendMessage(String message, boolean isLocal) {
        SimpleAttributeSet style = isLocal ? localStyle : remoteStyle;
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String fullMessage = "[" + timeStamp + "] " + message + "\n";
        try {
            doc.insertString(doc.getLength(), fullMessage, style);
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a typing indicator.
     * @param sender the name of the sender who is typing.
     */
    public void showTypingIndicator(String sender) {
        typingIndicatorLabel.setText(sender + " is typing...");
    }

    /**
     * Clears the typing indicator.
     */
    public void clearTypingIndicator() {
        typingIndicatorLabel.setText(" ");
    }

    /**
     * Listener interface for chat events.
     */
    public interface ChatPanelListener {
        void onChatMessage(String message);
        void onTyping();
    }
}
