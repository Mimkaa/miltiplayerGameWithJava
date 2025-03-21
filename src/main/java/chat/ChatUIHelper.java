package chat;

import javax.swing.*;
import java.awt.*;

/**
 * Helper class to install and manage the chat UI within the main game frame.
 */
public class ChatUIHelper {

    /**
     * Installs the chat UI into the provided JFrame.
     * This method adds a toggle button at the top to show/hide the chat panel.
     *
     * @param frame     the main application frame.
     * @param chatPanel the chat panel instance to install.
     */
    public static void installChatUI(JFrame frame, JPanel chatPanel) {
        // Create a top panel for the toggle button.
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleButton = new JButton("Open Chat");
        topPanel.add(toggleButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // Initially hide the chat panel.
        chatPanel.setVisible(false);
        frame.add(chatPanel, BorderLayout.SOUTH);

        // Toggle the chat panel's visibility when the button is clicked.
        toggleButton.addActionListener(e -> {
            boolean visible = chatPanel.isVisible();
            chatPanel.setVisible(!visible);
            toggleButton.setText(visible ? "Open Chat" : "Close Chat");

            if (!visible) {
                // When opening chat, request focus on the chat input component if available.
                // Assuming your ChatPanel class has a method getChatInputField() that returns a JTextField.
                if (chatPanel instanceof ChatPanel) {
                    ChatPanel cp = (ChatPanel) chatPanel;
                    JTextField chatInputField = cp.getInputField();
                    if (chatInputField != null) {
                        chatInputField.requestFocusInWindow();
                    } else {
                        chatPanel.requestFocusInWindow();
                    }
                } else {
                    chatPanel.requestFocusInWindow();
                }
            } else {
                // When closing chat, set the focus back to the main frame.
                frame.requestFocusInWindow();
            }
            frame.revalidate();
        });
    }
}
